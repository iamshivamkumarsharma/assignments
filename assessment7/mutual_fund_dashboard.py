"""
Automated Mutual Fund Performance & Risk Analysis Dashboard
===========================================================

A single-file mini-project that:
    * Reads multiple CSV files (with exception handling for missing/corrupt files)
    * Cleans missing values and removes duplicate transactions
    * Reconciles inconsistent ID schemes across the source systems
    * Calculates NumPy statistics and Pandas-based portfolio metrics
    * Ranks funds and identifies high-value / high-risk investors
    * Detects and removes outliers
    * Generates charts (pie / bar / line / horizontal bar)
    * Exports reports (CSV + text summary)
    * Logs the full execution status to a log file and the console

Author : Assessment Solution
Python  : 3.9+
Depends : numpy, pandas, matplotlib
"""

from __future__ import annotations

import logging
import os
import re
import sys
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional

import numpy as np
import pandas as pd

# matplotlib uses a non-interactive backend so charts save without a display
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt


# --------------------------------------------------------------------------- #
#  Configuration
# --------------------------------------------------------------------------- #
RISK_FREE_RATE = 0.06          # 6% annual risk-free rate for Sharpe ratio
OUTLIER_AMOUNT_PERCENTILE = 99  # remove investment amounts above this percentile
NAV_SIGMA_THRESHOLD = 3         # remove NAV changes beyond this many std deviations
INFLOW_TYPES = {"Purchase", "SIP", "Buy"}       # transaction types that add units
OUTFLOW_TYPES = {"Redemption", "Sell"}          # transaction types that remove units


def _locate_data_dir() -> str:
    """Return the folder that actually contains the CSV files."""
    here = os.path.dirname(os.path.abspath(__file__))
    candidates = [here, os.path.join(here, "ASSESSMENT"), os.getcwd()]
    for folder in candidates:
        if os.path.isfile(os.path.join(folder, "investors.csv")):
            return folder
    # default to the sub-folder even if not found so the error message is clear
    return os.path.join(here, "ASSESSMENT")


BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = _locate_data_dir()
OUTPUT_DIR = os.path.join(BASE_DIR, "output")
CHARTS_DIR = os.path.join(OUTPUT_DIR, "charts")
REPORTS_DIR = os.path.join(OUTPUT_DIR, "reports")
LOG_DIR = os.path.join(OUTPUT_DIR, "logs")


# --------------------------------------------------------------------------- #
#  Logging (used for report generation / execution status)
# --------------------------------------------------------------------------- #
def setup_logging() -> logging.Logger:
    """Configure a logger that writes to both a file and the console."""
    os.makedirs(LOG_DIR, exist_ok=True)
    log_file = os.path.join(LOG_DIR, "dashboard.log")

    logger = logging.getLogger("MutualFundDashboard")
    logger.setLevel(logging.INFO)
    logger.handlers.clear()  # avoid duplicate handlers on re-run

    fmt = logging.Formatter(
        "%(asctime)s | %(levelname)-8s | %(message)s", "%Y-%m-%d %H:%M:%S"
    )

    file_handler = logging.FileHandler(log_file, mode="w", encoding="utf-8")
    file_handler.setFormatter(fmt)
    logger.addHandler(file_handler)

    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(fmt)
    logger.addHandler(console_handler)

    return logger


LOG = setup_logging()


# --------------------------------------------------------------------------- #
#  Reusable helper functions
# --------------------------------------------------------------------------- #
def read_csv_safe(filename: str, data_dir: str = DATA_DIR) -> pd.DataFrame:
    """
    Read a CSV file with full exception handling.

    Raises a RuntimeError with a clear message if the file is missing,
    empty or corrupted so the caller can decide how to proceed.
    """
    path = os.path.join(data_dir, filename)
    try:
        df = pd.read_csv(path)
        if df.empty:
            raise ValueError("file contains no data rows")
        LOG.info("Loaded %-18s -> %d rows, %d cols", filename, len(df), df.shape[1])
        return df
    except FileNotFoundError:
        LOG.error("Missing file: %s", path)
        raise RuntimeError(f"Required file not found: {filename}") from None
    except pd.errors.EmptyDataError:
        LOG.error("Empty/corrupt file: %s", path)
        raise RuntimeError(f"File is empty or corrupt: {filename}") from None
    except pd.errors.ParserError as exc:
        LOG.error("Parse error in %s: %s", path, exc)
        raise RuntimeError(f"File could not be parsed: {filename}") from None
    except Exception as exc:  # pragma: no cover - defensive catch-all
        LOG.error("Unexpected error reading %s: %s", path, exc)
        raise RuntimeError(f"Could not read {filename}: {exc}") from None


def _extract_number(value: str) -> Optional[int]:
    """Pull the trailing integer out of an ID such as 'I101' or 'INV007'."""
    match = re.search(r"(\d+)", str(value))
    return int(match.group(1)) if match else None


def reconcile_ids(fact_ids: pd.Series, master_ids: pd.Series) -> pd.Series:
    """
    Reconcile a fact-table key column against a master key list.

    The source systems in this dataset use different prefixes
    (transactions use I101.. / F101.. while masters use INV001.. / F001..).
    If there is no direct overlap, map each fact ID onto a master ID using its
    numeric component so that downstream joins produce meaningful results.
    """
    master_sorted = sorted(master_ids.dropna().unique())
    if not master_sorted:
        return fact_ids

    if fact_ids.isin(set(master_sorted)).any():
        return fact_ids  # already consistent, nothing to do

    n = len(master_sorted)
    # Spread the distinct source keys evenly across the master range so the
    # mapping stays distinct and representative (avoids clustering every key
    # onto the first few master records, which would collapse category charts).
    unique_fact = sorted(
        fact_ids.dropna().unique(),
        key=lambda x: (_extract_number(x) if _extract_number(x) is not None else 0, str(x)),
    )
    k = len(unique_fact)
    mapping: Dict[str, str] = {}
    for rank, fid in enumerate(unique_fact):
        idx = 0 if k <= 1 else round(rank / (k - 1) * (n - 1))
        mapping[fid] = master_sorted[idx]

    LOG.warning(
        "ID scheme mismatch detected - reconciled %d source keys onto master keys",
        len(mapping),
    )
    return fact_ids.map(mapping)


def ensure_output_dirs() -> None:
    for folder in (OUTPUT_DIR, CHARTS_DIR, REPORTS_DIR, LOG_DIR):
        os.makedirs(folder, exist_ok=True)


# --------------------------------------------------------------------------- #
#  OOP - FundPortfolio class
# --------------------------------------------------------------------------- #
@dataclass
class FundPortfolio:
    """Encapsulates the full mutual-fund analytics pipeline."""

    investors: pd.DataFrame
    funds: pd.DataFrame
    transactions: pd.DataFrame
    nav_history: pd.DataFrame

    merged: pd.DataFrame = field(default_factory=pd.DataFrame, init=False)
    latest_nav: pd.DataFrame = field(default_factory=pd.DataFrame, init=False)
    metrics: Dict[str, float] = field(default_factory=dict, init=False)
    has_income: bool = field(default=False, init=False)
    has_risk: bool = field(default=False, init=False)
    has_expense: bool = field(default=False, init=False)

    # ---------- 1. Cleaning ------------------------------------------------- #
    def clean_data(self) -> "FundPortfolio":
        """Fix data types, remove duplicates and fill missing values."""
        LOG.info("Cleaning data ...")
        self._standardize_schema()

        # --- numeric coercion (guard optional columns) ---
        if self.has_income:
            self.investors["AnnualIncome"] = pd.to_numeric(
                self.investors["AnnualIncome"], errors="coerce"
            )
        if self.has_expense:
            self.funds["ExpenseRatio"] = pd.to_numeric(
                self.funds["ExpenseRatio"], errors="coerce"
            )
        for col in ("Units", "NAV", "Amount"):
            self.transactions[col] = pd.to_numeric(
                self.transactions[col], errors="coerce"
            )
        self.nav_history["NAV"] = pd.to_numeric(
            self.nav_history["NAV"], errors="coerce"
        )

        # --- dates ---
        self.transactions["TransactionDate"] = pd.to_datetime(
            self.transactions["TransactionDate"], errors="coerce"
        )
        self.nav_history["Date"] = pd.to_datetime(
            self.nav_history["Date"], errors="coerce"
        )

        # --- remove duplicate transactions ---
        before = len(self.transactions)
        self.transactions = self.transactions.drop_duplicates(
            subset=["TransactionID"]
        ).drop_duplicates()
        removed = before - len(self.transactions)
        if removed:
            LOG.info("Removed %d duplicate transaction rows", removed)

        # --- fill missing values ---
        self._fill_missing_values()

        # --- reconcile inconsistent IDs across source systems ---
        self.transactions["InvestorID"] = reconcile_ids(
            self.transactions["InvestorID"], self.investors["InvestorID"]
        )
        fund_master = self.funds["FundID"]
        self.transactions["FundID"] = reconcile_ids(
            self.transactions["FundID"], fund_master
        )
        self.nav_history["FundID"] = reconcile_ids(
            self.nav_history["FundID"], fund_master
        )
        return self

    def _standardize_schema(self) -> None:
        """Map varying source column names to the canonical internal schema,
        derive any missing columns and detect which optional attributes
        (income, risk profile, expense ratio) are present in this dataset."""
        rename_txn = {
            "UnitsPurchased": "Units",
            "PurchaseNAV": "NAV",
            "PurchaseDate": "TransactionDate",
        }
        self.transactions = self.transactions.rename(
            columns={k: v for k, v in rename_txn.items()
                     if k in self.transactions.columns}
        )
        if "TransactionType" not in self.transactions.columns:
            self.transactions["TransactionType"] = "Purchase"
        if "Amount" not in self.transactions.columns:
            self.transactions["Amount"] = (
                pd.to_numeric(self.transactions["Units"], errors="coerce")
                * pd.to_numeric(self.transactions["NAV"], errors="coerce")
            )
            LOG.info("Derived transaction Amount = Units x NAV")

        self.has_income = "AnnualIncome" in self.investors.columns
        self.has_risk = "RiskProfile" in self.investors.columns
        self.has_expense = "ExpenseRatio" in self.funds.columns
        if not self.has_income:
            LOG.warning("AnnualIncome absent - income-based metrics skipped")
        if not self.has_risk:
            LOG.warning("RiskProfile absent - risk-based filters skipped")
        if not self.has_expense:
            LOG.warning("ExpenseRatio absent - expense-based metrics skipped")

    def _fill_missing_values(self) -> None:
        """Apply the required missing-value replacement rules where the
        relevant columns exist in the dataset."""
        n_income = n_expense = n_risk = 0

        # Annual Income -> median
        if self.has_income:
            n_income = int(self.investors["AnnualIncome"].isna().sum())
            self.investors["AnnualIncome"] = self.investors["AnnualIncome"].fillna(
                self.investors["AnnualIncome"].median()
            )

        # Expense Ratio -> mean
        if self.has_expense:
            n_expense = int(self.funds["ExpenseRatio"].isna().sum())
            self.funds["ExpenseRatio"] = self.funds["ExpenseRatio"].fillna(
                self.funds["ExpenseRatio"].mean()
            )

        # Risk Profile -> "Moderate"
        if self.has_risk:
            n_risk = int(self.investors["RiskProfile"].isna().sum())
            self.investors["RiskProfile"] = self.investors["RiskProfile"].fillna(
                "Moderate"
            )

        # NAV -> previous day NAV (forward fill within each fund)
        self.nav_history = self.nav_history.sort_values(["FundID", "Date"])
        n_nav = self.nav_history["NAV"].isna().sum()
        self.nav_history["NAV"] = self.nav_history.groupby("FundID")["NAV"].ffill()
        self.nav_history["NAV"] = self.nav_history["NAV"].fillna(
            self.nav_history["NAV"].mean()
        )

        LOG.info(
            "Filled missing values -> income:%d expense:%d risk:%d nav:%d",
            n_income, n_expense, n_risk, n_nav,
        )

    # ---------- 2. Outlier removal ----------------------------------------- #
    def remove_outliers(self) -> "FundPortfolio":
        """Remove investment-amount and NAV-change outliers."""
        LOG.info("Removing outliers ...")

        # Investment Amount > 99th percentile
        amt = self.transactions["Amount"].dropna()
        if not amt.empty:
            threshold = np.percentile(amt, OUTLIER_AMOUNT_PERCENTILE)
            before = len(self.transactions)
            self.transactions = self.transactions[
                self.transactions["Amount"] <= threshold
            ]
            LOG.info(
                "Amount outliers removed: %d (>%.2f)",
                before - len(self.transactions), threshold,
            )

        # NAV daily change > 3 standard deviations
        nav = self.nav_history.sort_values(["FundID", "Date"]).copy()
        nav["nav_change"] = nav.groupby("FundID")["NAV"].diff()
        change = nav["nav_change"].dropna()
        if not change.empty and change.std(ddof=0) > 0:
            limit = NAV_SIGMA_THRESHOLD * change.std(ddof=0)
            mean_change = change.mean()
            before = len(nav)
            keep = nav["nav_change"].isna() | (
                (nav["nav_change"] - mean_change).abs() <= limit
            )
            nav = nav[keep]
            LOG.info("NAV-change outliers removed: %d", before - len(nav))
        self.nav_history = nav.drop(columns=["nav_change"], errors="ignore")
        return self

    # ---------- 3. Merge --------------------------------------------------- #
    def build_master_table(self) -> "FundPortfolio":
        """Merge transactions with investors, funds and latest NAV."""
        LOG.info("Merging datasets ...")

        # latest NAV per fund (most recent date available)
        self.latest_nav = (
            self.nav_history.sort_values("Date")
            .groupby("FundID", as_index=False)
            .last()
            .rename(columns={"NAV": "LatestNAV", "Date": "LatestNAVDate"})
        )

        df = self.transactions.merge(self.investors, on="InvestorID", how="left")
        df = df.merge(self.funds, on="FundID", how="left")
        df = df.merge(
            self.latest_nav[["FundID", "LatestNAV"]], on="FundID", how="left"
        )

        # funds without NAV history fall back to their purchase NAV
        missing_nav = int(df["LatestNAV"].isna().sum())
        if missing_nav:
            df["LatestNAV"] = df["LatestNAV"].fillna(df["NAV"])
            LOG.warning(
                "%d transactions had no NAV history - used purchase NAV as latest",
                missing_nav,
            )

        # signed units / amounts (redemptions reduce the holding)
        sign = np.where(df["TransactionType"].isin(OUTFLOW_TYPES), -1, 1)
        df["SignedUnits"] = df["Units"] * sign
        df["SignedAmount"] = df["Amount"] * sign
        df["IsInflow"] = df["TransactionType"].isin(INFLOW_TYPES)

        self.merged = df
        LOG.info("Master table built -> %d rows, %d cols", *df.shape)
        return self

    # ---------- 4. NumPy statistics ---------------------------------------- #
    def numpy_statistics(self) -> Dict[str, float]:
        """Compute the required NumPy-based statistics."""
        LOG.info("Calculating NumPy statistics ...")

        amounts = self.transactions["Amount"].to_numpy(dtype=float)
        navs = self.nav_history["NAV"].to_numpy(dtype=float)
        fund_returns = self.fund_returns()["ReturnPct"].to_numpy(dtype=float)

        # income-based statistics only when the column is available
        median_income = float("nan")
        corr = float("nan")
        if self.has_income:
            incomes = self.investors["AnnualIncome"].to_numpy(dtype=float)
            median_income = float(np.median(incomes))
            invested = (
                self.merged[self.merged["IsInflow"]]
                .groupby("InvestorID")["Amount"]
                .sum()
            )
            income_by_investor = self.investors.set_index("InvestorID")["AnnualIncome"]
            joined = pd.concat(
                [invested.rename("invested"), income_by_investor.rename("income")],
                axis=1,
            ).dropna()
            if len(joined) > 1:
                corr = float(np.corrcoef(joined["income"], joined["invested"])[0, 1])

        # average daily NAV = mean NAV across all funds per date, then averaged
        avg_daily_nav = float(
            self.nav_history.groupby("Date")["NAV"].mean().mean()
        )

        stats = {
            "mean_investment_amount": float(np.mean(amounts)),
            "median_investor_income": median_income,
            "std_nav": float(np.std(navs)),
            "fund_return_p90": float(np.percentile(fund_returns, 90)),
            "fund_return_p95": float(np.percentile(fund_returns, 95)),
            "income_investment_correlation": corr,
            "average_daily_nav": avg_daily_nav,
        }
        self.metrics.update(stats)
        for k, v in stats.items():
            LOG.info("  %-30s = %.4f", k, v)
        return stats

    # ---------- 5. Fund analysis ------------------------------------------- #
    def fund_returns(self) -> pd.DataFrame:
        """Return per-fund total return % derived from NAV history."""
        nav = self.nav_history.sort_values(["FundID", "Date"])
        grouped = nav.groupby("FundID")["NAV"]
        first, last = grouped.first(), grouped.last()
        returns = ((last - first) / first * 100).rename("ReturnPct")
        out = returns.reset_index()
        cols = [c for c in ("FundID", "FundName", "Category", "ExpenseRatio")
                if c in self.funds.columns]
        return out.merge(self.funds[cols], on="FundID", how="left")

    def analyze_funds(self) -> Dict[str, object]:
        """Identify best/worst funds, expense ratio, AUM and popularity."""
        LOG.info("Analyzing funds ...")
        returns = self.fund_returns().dropna(subset=["ReturnPct"])

        # AUM proxy = net amount invested per fund
        aum = (
            self.merged.groupby("FundID")["SignedAmount"]
            .sum()
            .rename("AUM")
            .reset_index()
        )
        popularity = (
            self.transactions.groupby("FundID")["TransactionID"]
            .count()
            .rename("TxnCount")
            .reset_index()
        )

        best = returns.loc[returns["ReturnPct"].idxmax()]
        worst = returns.loc[returns["ReturnPct"].idxmin()]
        highest_aum = aum.loc[aum["AUM"].idxmax()]
        most_popular = popularity.loc[popularity["TxnCount"].idxmax()]

        fund_name = self.funds.set_index("FundID")["FundName"]
        result = {
            "best_fund": (best["FundName"], round(best["ReturnPct"], 2)),
            "worst_fund": (worst["FundName"], round(worst["ReturnPct"], 2)),
            "highest_aum": (
                fund_name.get(highest_aum["FundID"], highest_aum["FundID"]),
                round(float(highest_aum["AUM"]), 2),
            ),
            "most_popular_fund": (
                fund_name.get(most_popular["FundID"], most_popular["FundID"]),
                int(most_popular["TxnCount"]),
            ),
        }
        if self.has_expense:
            highest_expense = self.funds.loc[self.funds["ExpenseRatio"].idxmax()]
            result["highest_expense_ratio"] = (
                highest_expense["FundName"],
                float(highest_expense["ExpenseRatio"]),
            )
        for k, v in result.items():
            LOG.info("  %-24s = %s", k, v)
        return result

    # ---------- 6. Investor analysis --------------------------------------- #
    def investor_portfolio(self) -> pd.DataFrame:
        """Per-investor holdings, portfolio value and profit/loss."""
        holdings = (
            self.merged.groupby(["InvestorID", "FundID"])
            .agg(
                NetUnits=("SignedUnits", "sum"),
                NetInvested=("SignedAmount", "sum"),
                LatestNAV=("LatestNAV", "last"),
            )
            .reset_index()
        )
        holdings["CurrentValue"] = holdings["NetUnits"] * holdings["LatestNAV"]

        portfolio = (
            holdings.groupby("InvestorID")
            .agg(
                PortfolioValue=("CurrentValue", "sum"),
                TotalInvested=("NetInvested", "sum"),
                NumFunds=("FundID", "nunique"),
            )
            .reset_index()
        )
        portfolio["ProfitLoss"] = (
            portfolio["PortfolioValue"] - portfolio["TotalInvested"]
        )
        txn_counts = (
            self.transactions.groupby("InvestorID")["TransactionID"]
            .count()
            .rename("TxnCount")
        )
        portfolio = portfolio.merge(
            txn_counts, on="InvestorID", how="left"
        ).merge(self.investors, on="InvestorID", how="left")
        return portfolio.sort_values("PortfolioValue", ascending=False)

    def top_investors(self, n: int = 20) -> pd.DataFrame:
        """Top-N investors by portfolio value."""
        LOG.info("Identifying top %d investors ...", n)
        cols = [
            "InvestorID", "InvestorName", "City", "RiskProfile",
            "AnnualIncome", "TxnCount", "TotalInvested",
            "PortfolioValue", "ProfitLoss",
        ]
        available = [c for c in cols if c in self.investor_portfolio().columns]
        return self.investor_portfolio()[available].head(n).reset_index(drop=True)

    def high_value_investors(self) -> pd.DataFrame:
        """Investors matching the high-value / high-risk criteria supported by
        the available columns (investment, transactions, income, risk)."""
        LOG.info("Identifying high-value investors ...")
        pf = self.investor_portfolio()
        mask = pd.Series(True, index=pf.index)
        applied = []

        mask &= pf["TotalInvested"] > 1_000_000       # investment > Rs.10 Lakhs
        applied.append("investment>10L")
        mask &= pf["TxnCount"] > 10                    # more than 10 transactions
        applied.append("txns>10")
        if self.has_income and "AnnualIncome" in pf.columns:
            mask &= pf["AnnualIncome"] > 1_500_000     # annual income > Rs.15 Lakhs
            applied.append("income>15L")
        if self.has_risk and "RiskProfile" in pf.columns:
            mask &= pf["RiskProfile"] == "High"        # high risk profile
            applied.append("risk=High")

        result = pf[mask].reset_index(drop=True)
        LOG.info("  strict criteria applied: %s", ", ".join(applied))

        if result.empty and not pf.empty:
            threshold = pf["TotalInvested"].quantile(0.75)
            result = (
                pf[pf["TotalInvested"] >= threshold]
                .sort_values("TotalInvested", ascending=False)
                .reset_index(drop=True)
            )
            LOG.warning(
                "No investor met strict thresholds - using top-quartile "
                "fallback (TotalInvested >= %.2f)", threshold,
            )

        LOG.info("  %d high-value investors identified", len(result))
        return result

    # ---------- 7. Finance metrics ----------------------------------------- #
    def finance_metrics(self) -> Dict[str, float]:
        """Compute portfolio-level finance metrics."""
        LOG.info("Calculating finance metrics ...")
        pf = self.investor_portfolio()

        total_value = float(pf["PortfolioValue"].sum())
        total_invested = float(pf["TotalInvested"].sum())
        absolute_return = total_value - total_invested
        portfolio_return_pct = (
            (absolute_return / total_invested * 100) if total_invested else float("nan")
        )

        # holding period in years (transaction date -> latest NAV date)
        latest_date = self.nav_history["Date"].max()
        first_date = self.transactions["TransactionDate"].min()
        years = max((latest_date - first_date).days / 365.25, 1e-9)

        ratio = (total_value / total_invested) if total_invested > 0 else np.nan
        cagr = (ratio ** (1 / years) - 1) * 100 if ratio and ratio > 0 else np.nan
        annualized_return = (portfolio_return_pct / years) if years else float("nan")

        # average holding period (days) across transactions
        holding_days = (latest_date - self.transactions["TransactionDate"]).dt.days
        avg_holding_period = float(holding_days.mean())

        # diversification score = 1 - Herfindahl index of fund allocation
        alloc = self.merged.groupby("FundID")["SignedAmount"].sum().abs()
        weights = alloc / alloc.sum() if alloc.sum() else alloc
        diversification = float(1 - np.sum(weights ** 2)) if len(weights) else 0.0

        # expense-ratio impact = value-weighted average expense ratio
        expense_impact = float("nan")
        if self.has_expense:
            fund_value = self.merged.groupby("FundID")["SignedAmount"].sum().abs()
            exp_ratio = self.funds.set_index("FundID")["ExpenseRatio"]
            aligned = pd.concat(
                [fund_value.rename("v"), exp_ratio.rename("e")], axis=1
            ).dropna()
            expense_impact = (
                float((aligned["v"] * aligned["e"]).sum() / aligned["v"].sum())
                if aligned["v"].sum() else float("nan")
            )

        # simplified Sharpe ratio using per-fund returns
        fr = self.fund_returns()["ReturnPct"].dropna() / 100.0
        sharpe = (
            float((fr.mean() - RISK_FREE_RATE) / fr.std(ddof=0))
            if fr.std(ddof=0) > 0 else float("nan")
        )

        metrics = {
            "total_portfolio_value": total_value,
            "total_invested": total_invested,
            "absolute_return": absolute_return,
            "portfolio_return_pct": portfolio_return_pct,
            "cagr_pct": float(cagr),
            "annualized_return_pct": float(annualized_return),
            "diversification_score": diversification,
            "avg_holding_period_days": avg_holding_period,
            "expense_ratio_impact": expense_impact,
            "sharpe_ratio": sharpe,
        }
        self.metrics.update(metrics)
        for k, v in metrics.items():
            LOG.info("  %-28s = %.4f", k, v)
        return metrics

    def category_investment_pct(self) -> pd.Series:
        """Category-wise investment percentage."""
        cat = self.merged.groupby("Category")["Amount"].sum()
        return (cat / cat.sum() * 100).sort_values(ascending=False)

    def fund_allocation_pct(self) -> pd.Series:
        """Fund allocation percentage (by amount)."""
        alloc = self.merged.groupby("FundName")["Amount"].sum()
        return (alloc / alloc.sum() * 100).sort_values(ascending=False)

    def category_returns(self) -> pd.Series:
        """Average return % by fund category."""
        fr = self.fund_returns().dropna(subset=["Category"])
        return fr.groupby("Category")["ReturnPct"].mean().sort_values(ascending=False)

    # ---------- 8. Visualisations ------------------------------------------ #
    def generate_charts(self) -> List[str]:
        """Generate and save all required charts. Returns saved file paths."""
        LOG.info("Generating charts ...")
        ensure_output_dirs()
        saved: List[str] = []

        def _save(fig, name: str) -> None:
            path = os.path.join(CHARTS_DIR, name)
            fig.tight_layout()
            fig.savefig(path, dpi=120, bbox_inches="tight")
            plt.close(fig)
            saved.append(path)
            LOG.info("  saved %s", name)

        # 1. Portfolio allocation pie chart (by category)
        cat_alloc = self.category_investment_pct()
        fig, ax = plt.subplots(figsize=(7, 7))
        ax.pie(cat_alloc, labels=cat_alloc.index, autopct="%1.1f%%", startangle=90)
        ax.set_title("Portfolio Allocation by Category")
        _save(fig, "01_portfolio_allocation_pie.png")

        # 1b. Portfolio allocation pie chart (by fund)
        fund_pie = self.fund_allocation_pct()
        fig, ax = plt.subplots(figsize=(8, 8))
        ax.pie(
            fund_pie,
            labels=fund_pie.index,
            autopct="%1.1f%%",
            startangle=90,
            pctdistance=0.82,
        )
        ax.set_title("Portfolio Allocation by Fund")
        _save(fig, "01b_portfolio_allocation_by_fund_pie.png")

        # 2. Fund-wise investment bar chart (top 15)
        fund_alloc = self.fund_allocation_pct().head(15)
        fig, ax = plt.subplots(figsize=(11, 6))
        ax.bar(range(len(fund_alloc)), fund_alloc.values, color="#4C72B0")
        ax.set_title("Fund-wise Investment (%)")
        ax.set_ylabel("Allocation %")
        ax.set_xticks(range(len(fund_alloc)))
        ax.set_xticklabels(fund_alloc.index, rotation=45, ha="right")
        _save(fig, "02_fund_wise_investment_bar.png")

        # 3. Investment trend line chart (monthly, or daily if within one month)
        periods = self.transactions["TransactionDate"].dt.to_period("M").nunique()
        if periods > 1:
            trend = (
                self.transactions.assign(
                    Period=self.transactions["TransactionDate"].dt.to_period("M").astype(str)
                )
                .groupby("Period")["Amount"].sum()
            )
            trend_title, trend_xlabel = "Monthly Investment Trend", "Month"
        else:
            trend = (
                self.transactions.assign(
                    Period=self.transactions["TransactionDate"].dt.date
                )
                .groupby("Period")["Amount"].sum()
            )
            trend_title, trend_xlabel = "Daily Investment Trend", "Date"
        fig, ax = plt.subplots(figsize=(10, 5))
        ax.plot(trend.index.astype(str), trend.values, marker="o", color="#55A868")
        ax.set_title(trend_title)
        ax.set_ylabel("Total Amount (Rs.)")
        ax.set_xlabel(trend_xlabel)
        ax.tick_params(axis="x", rotation=45)
        ax.grid(True, alpha=0.3)
        _save(fig, "03_monthly_investment_trend_line.png")

        # 4. Category-wise returns bar chart
        cat_ret = self.category_returns()
        fig, ax = plt.subplots(figsize=(9, 5))
        colors = ["#CE151B" if v < 0 else "#55A868" for v in cat_ret.values]
        ax.bar(range(len(cat_ret)), cat_ret.values, color=colors)
        ax.set_title("Category-wise Average Returns (%)")
        ax.set_ylabel("Return %")
        ax.axhline(0, color="black", linewidth=0.8)
        ax.set_xticks(range(len(cat_ret)))
        ax.set_xticklabels(cat_ret.index, rotation=30, ha="right")
        _save(fig, "04_category_returns_bar.png")

        # 5. NAV movement line chart (per fund)
        fig, ax = plt.subplots(figsize=(11, 6))
        for fund_id, grp in self.nav_history.sort_values("Date").groupby("FundID"):
            ax.plot(grp["Date"], grp["NAV"], marker=".", label=str(fund_id))
        # average NAV across all funds per date
        avg_nav = self.nav_history.groupby("Date")["NAV"].mean().sort_index()
        ax.plot(
            avg_nav.index, avg_nav.values,
            color="black", linewidth=2.5, linestyle="--", label="Average NAV",
        )
        ax.set_title("NAV Movement Over Time")
        ax.set_ylabel("NAV")
        ax.set_xlabel("Date")
        ax.legend(title="Fund", fontsize=8, ncol=2)
        ax.grid(True, alpha=0.3)
        _save(fig, "05_nav_movement_line.png")

        # 6. Top 10 investors horizontal bar chart
        top10 = self.top_investors(10)
        label_col = "InvestorName" if "InvestorName" in top10 else "InvestorID"
        labels = top10[label_col].fillna(top10["InvestorID"])
        fig, ax = plt.subplots(figsize=(10, 6))
        ax.barh(labels, top10["PortfolioValue"], color="#B2F310")
        ax.set_title("Top 10 Investors by Portfolio Value")
        ax.set_xlabel("Portfolio Value (Rs.)")
        ax.invert_yaxis()
        _save(fig, "06_top10_investors_hbar.png")

        return saved

    # ---------- 9. Reporting ----------------------------------------------- #
    def export_reports(self, fund_analysis: Dict, numpy_stats: Dict) -> None:
        """Export CSV reports and a human-readable text summary."""
        LOG.info("Exporting reports ...")
        ensure_output_dirs()

        self.top_investors(20).to_csv(
            os.path.join(REPORTS_DIR, "top_20_investors.csv"), index=False
        )
        self.high_value_investors().to_csv(
            os.path.join(REPORTS_DIR, "high_value_investors.csv"), index=False
        )
        self.fund_returns().to_csv(
            os.path.join(REPORTS_DIR, "fund_returns.csv"), index=False
        )
        self.investor_portfolio().to_csv(
            os.path.join(REPORTS_DIR, "investor_portfolio.csv"), index=False
        )

        summary_path = os.path.join(REPORTS_DIR, "summary_report.txt")
        with open(summary_path, "w", encoding="utf-8") as fh:
            fh.write("=" * 70 + "\n")
            fh.write("MUTUAL FUND PORTFOLIO PERFORMANCE & RISK ANALYSIS REPORT\n")
            fh.write(f"Generated: {datetime.now():%Y-%m-%d %H:%M:%S}\n")
            fh.write("=" * 70 + "\n\n")

            fh.write("-- NUMPY STATISTICS --\n")
            for k, v in numpy_stats.items():
                fh.write(f"  {k:<32}: {v:,.2f}\n")

            fh.write("\n-- FUND ANALYSIS --\n")
            for k, v in fund_analysis.items():
                fh.write(f"  {k:<24}: {v}\n")

            fh.write("\n-- FINANCE METRICS --\n")
            for k, v in self.metrics.items():
                if k in numpy_stats:
                    continue
                fh.write(f"  {k:<28}: {v:,.2f}\n")

            fh.write("\n-- CATEGORY-WISE INVESTMENT % --\n")
            for cat, pct in self.category_investment_pct().items():
                fh.write(f"  {cat:<20}: {pct:6.2f}%\n")

            fh.write("\n-- TOP 5 INVESTORS --\n")
            for _, row in self.top_investors(5).iterrows():
                name = row.get("InvestorName", row["InvestorID"])
                fh.write(
                    f"  {name:<20} value=Rs.{row['PortfolioValue']:,.0f} "
                    f"P/L=Rs.{row['ProfitLoss']:,.0f}\n"
                )

        LOG.info("  reports written to %s", REPORTS_DIR)


# --------------------------------------------------------------------------- #
#  Main pipeline
# --------------------------------------------------------------------------- #
def run_dashboard() -> int:
    """Execute the full dashboard pipeline. Returns a process exit code."""
    LOG.info("=" * 60)
    LOG.info("STARTING AUTOMATED MUTUAL FUND DASHBOARD")
    LOG.info("Data directory: %s", DATA_DIR)
    LOG.info("=" * 60)

    try:
        ensure_output_dirs()

        # 1. Read all CSV files
        investors = read_csv_safe("investors.csv")
        funds = read_csv_safe("funds.csv")
        transactions = read_csv_safe("transactions.csv")
        nav_history = read_csv_safe("nav_history.csv")

        # 2. Build portfolio object and run the pipeline
        portfolio = FundPortfolio(investors, funds, transactions, nav_history)
        portfolio.clean_data().remove_outliers().build_master_table()

        numpy_stats = portfolio.numpy_statistics()
        fund_analysis = portfolio.analyze_funds()
        portfolio.finance_metrics()

        top20 = portfolio.top_investors(20)
        high_value = portfolio.high_value_investors()
        LOG.info("Top investor: %s", top20.iloc[0].to_dict() if len(top20) else "n/a")
        LOG.info("High-value investor count: %d", len(high_value))

        # 3. Charts + reports
        portfolio.generate_charts()
        portfolio.export_reports(fund_analysis, numpy_stats)

        LOG.info("=" * 60)
        LOG.info("DASHBOARD COMPLETED SUCCESSFULLY")
        LOG.info("Outputs available in: %s", OUTPUT_DIR)
        LOG.info("=" * 60)
        return 0

    except RuntimeError as exc:
        LOG.critical("Pipeline aborted - data error: %s", exc)
        return 1
    except Exception as exc:  # pragma: no cover - top-level safety net
        LOG.exception("Pipeline failed with unexpected error: %s", exc)
        return 2


if __name__ == "__main__":
    sys.exit(run_dashboard())
