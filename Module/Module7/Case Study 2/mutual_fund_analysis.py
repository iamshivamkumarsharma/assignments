"""
Case Study 2 - Mutual Fund Performance Analytics (Medium)
=========================================================

Business scenario
-----------------
ABC Asset Management Company (AMC) manages several mutual funds. Management
wants an application that analyses fund performance, investor portfolios and
returns to recommend the best-performing funds.

Datasets (live next to this script)
-----------------------------------
  funds.csv        : FundID, FundName, Category, AMC
  investors.csv    : InvestorID, InvestorName, Age, State, InvestorType
  transactions.csv : TransactionID, InvestorID, FundID, UnitsPurchased, PurchaseNAV, PurchaseDate
  nav_history.csv  : FundID, Date, NAV

Structure follows the 10-part brief:
  1 Read   2 Clean   3 Merge   4 New columns   5 NumPy   6 Analysis
  7 GroupBy   8 Detect issues   9 Finance metrics   10 Export reports

Documented assumptions
----------------------
  * Latest NAV        = most recent NAV per fund in nav_history.csv.
  * Holding period    = 1 year, so Annual Return = ROI %.
  * Volatility        = std-dev of a fund's daily NAV returns (%).
  * Sharpe Ratio      = (Annual Return % - Risk Free Rate) / Volatility, Risk Free = 6%.
"""

import json
import os

import numpy as np
import pandas as pd

pd.set_option("display.max_columns", None)
pd.set_option("display.width", 160)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(BASE_DIR, "output")
RISK_FREE_RATE = 6.0  # percent


def p(name: str) -> str:
    return os.path.join(BASE_DIR, name)


def header(title: str) -> None:
    print("\n" + "=" * 80)
    print(title)
    print("=" * 80)


def show(df: pd.DataFrame, n: int = 10) -> None:
    print("(none)" if len(df) == 0 else df.head(n).to_string(index=False))


def read_csv_safe(filename: str) -> pd.DataFrame:
    """Read a CSV with exception handling for missing / corrupted files."""
    path = p(filename)
    try:
        df = pd.read_csv(path)
        if df.empty:
            raise ValueError("file is empty")
        print(f"  loaded {filename:18} -> {df.shape[0]:>4} rows x {df.shape[1]} cols")
        return df
    except FileNotFoundError:
        raise SystemExit(f"ERROR: required file not found -> {path}")
    except pd.errors.EmptyDataError:
        raise SystemExit(f"ERROR: no data in file -> {path}")
    except pd.errors.ParserError as exc:
        raise SystemExit(f"ERROR: could not parse (corrupted?) -> {path}\n{exc}")


# ---------------------------------------------------------------------------
# PART 1 - READ DATA
# ---------------------------------------------------------------------------
header("PART 1 - READ DATA")
funds = read_csv_safe("funds.csv")
investors = read_csv_safe("investors.csv")
transactions = read_csv_safe("transactions.csv")
nav = read_csv_safe("nav_history.csv")


# ---------------------------------------------------------------------------
# PART 2 - DATA CLEANING
# ---------------------------------------------------------------------------
header("PART 2 - DATA CLEANING")

before = len(funds) + len(investors) + len(transactions) + len(nav)
funds = funds.drop_duplicates()
investors = investors.drop_duplicates()
transactions = transactions.drop_duplicates()
nav = nav.drop_duplicates()
print(f"Duplicate rows removed: {before - (len(funds)+len(investors)+len(transactions)+len(nav))}")

print("\nMissing values per table (before fill):")
for name, d in [("funds", funds), ("investors", investors),
                ("transactions", transactions), ("nav_history", nav)]:
    print(f"  {name:12}: {int(d.isna().sum().sum())}")

# Convert date columns to datetime.
nav["Date"] = pd.to_datetime(nav["Date"], errors="coerce")
transactions["PurchaseDate"] = pd.to_datetime(transactions["PurchaseDate"], errors="coerce")

# Fill missing NAV using forward fill (per fund, in date order).
nav = nav.sort_values(["FundID", "Date"])
nav["NAV"] = nav.groupby("FundID")["NAV"].ffill()

# Replace missing InvestorType with "Retail".
investors["InvestorType"] = investors["InvestorType"].fillna("Retail")

# Remove rows having negative NAV.
neg_nav = int((nav["NAV"] < 0).sum())
nav = nav[nav["NAV"] >= 0]
print(f"\nApplied: NAV forward-fill | InvestorType->'Retail' | removed {neg_nav} negative-NAV rows")
print("Dates converted to datetime: nav.Date, transactions.PurchaseDate")


# ---------------------------------------------------------------------------
# PART 3 - MERGE DATASETS
# ---------------------------------------------------------------------------
header("PART 3 - MERGE DATASETS")

# Latest NAV per fund = most recent record in nav_history.
latest_nav = (
    nav.sort_values("Date").groupby("FundID", as_index=False).last()
    .rename(columns={"NAV": "LatestNAV"})[["FundID", "LatestNAV"]]
)

df = (
    transactions
    .merge(investors, on="InvestorID", how="left")
    .merge(funds, on="FundID", how="left")
    .merge(latest_nav, on="FundID", how="left")
)

# Some funds referenced in transactions have no NAV history, so LatestNAV is missing
# and Current Value / Profit / ROI cannot be valued. Those rows are reported here and
# excluded from the performance analysis (otherwise their NaN profit would sum to 0 and
# masquerade as the "most profitable" funds).
funds_missing_nav = sorted(set(transactions["FundID"]) - set(latest_nav["FundID"]))
missing_rows = int(df["LatestNAV"].isna().sum())
if funds_missing_nav:
    print(f"Funds with no NAV history (cannot value) : {funds_missing_nav}")
    print(f"Transactions excluded from returns       : {missing_rows}")
df = df[df["LatestNAV"].notna()].copy()

required = ["InvestorName", "FundName", "Category", "AMC", "State",
            "UnitsPurchased", "PurchaseNAV", "LatestNAV"]
print("Valued transactions:", df.shape[0])
print("\nUnified dataframe (required columns):")
show(df[required])


# ---------------------------------------------------------------------------
# PART 4 - CREATE NEW COLUMNS
# ---------------------------------------------------------------------------
header("PART 4 - CREATE NEW COLUMNS")
df["InvestmentAmount"] = df["UnitsPurchased"] * df["PurchaseNAV"]
df["CurrentValue"] = df["UnitsPurchased"] * df["LatestNAV"]
df["Profit"] = df["CurrentValue"] - df["InvestmentAmount"]
df["ROI"] = (df["Profit"] / df["InvestmentAmount"]) * 100

show(df[["InvestorName", "FundName", "InvestmentAmount", "CurrentValue",
         "Profit", "ROI"]].round(2))


# ---------------------------------------------------------------------------
# PART 5 - NUMPY TASKS (on NAV)
# ---------------------------------------------------------------------------
header("PART 5 - NUMPY TASKS (NAV)")
nav_values = nav["NAV"].to_numpy()
nav_stats = {
    "Average NAV": float(np.mean(nav_values)),
    "Maximum NAV": float(np.max(nav_values)),
    "Minimum NAV": float(np.min(nav_values)),
    "Variance of NAV": float(np.var(nav_values)),
    "Std Deviation of NAV": float(np.std(nav_values)),
}
for k, v in nav_stats.items():
    print(f"  {k:22}: {v:,.4f}")

# Rolling average (window = 5) - shown per fund on the ordered NAV series.
nav["RollingAvg5"] = nav.groupby("FundID")["NAV"].transform(
    lambda s: s.rolling(window=5).mean()
)
print("\nRolling average (window=5) sample:")
show(nav[["FundID", "Date", "NAV", "RollingAvg5"]].dropna(), n=8)


# ---------------------------------------------------------------------------
# PART 6 - PANDAS ANALYSIS
# ---------------------------------------------------------------------------
header("PART 6 - PANDAS ANALYSIS")

top5_investors = (
    df.groupby(["InvestorID", "InvestorName"], as_index=False)["InvestmentAmount"]
    .sum().nlargest(5, "InvestmentAmount")
)
top5_funds = (
    df.groupby(["FundID", "FundName"], as_index=False)["Profit"]
    .sum().nlargest(5, "Profit")
)
fund_roi = df.groupby(["FundID", "FundName"], as_index=False)["ROI"].mean()
worst_fund = fund_roi.nsmallest(1, "ROI")
highest_nav_fund = latest_nav.merge(funds, on="FundID").nlargest(1, "LatestNAV")
lowest_nav_fund = latest_nav.merge(funds, on="FundID").nsmallest(1, "LatestNAV")

print("Top 5 investors by investment amount:")
show(top5_investors.round(2))
print("\nTop 5 profitable funds:")
show(top5_funds.round(2))
print("\nWorst performing fund (lowest avg ROI):")
show(worst_fund.round(2))
print("\nHighest NAV fund:")
show(highest_nav_fund[["FundID", "FundName", "AMC", "LatestNAV"]])
print("\nLowest NAV fund:")
show(lowest_nav_fund[["FundID", "FundName", "AMC", "LatestNAV"]])


# ---------------------------------------------------------------------------
# PART 7 - GROUP BY
# ---------------------------------------------------------------------------
header("PART 7 - GROUP BY")

by_category = df.groupby("Category").agg(
    AvgROI=("ROI", "mean"),
    AvgNAV=("LatestNAV", "mean"),
    TotalInvestment=("InvestmentAmount", "sum"),
).round(2)

by_amc = df.groupby("AMC").agg(
    NumberOfFunds=("FundID", "nunique"),
    AvgNAV=("LatestNAV", "mean"),
    TotalInvestment=("InvestmentAmount", "sum"),
).round(2)

by_state = df.groupby("State").agg(
    NumberOfInvestors=("InvestorID", "nunique"),
    TotalInvestment=("InvestmentAmount", "sum"),
    AvgROI=("ROI", "mean"),
).round(2)

by_type = df.groupby("InvestorType").agg(
    TotalInvestment=("InvestmentAmount", "sum"),
    AvgProfit=("Profit", "mean"),
).round(2)

print("By Category:\n", by_category.to_string())
print("\nBy AMC:\n", by_amc.to_string())
print("\nBy State:\n", by_state.to_string())
print("\nBy Investor Type:\n", by_type.to_string())


# ---------------------------------------------------------------------------
# PART 8 - DETECT ISSUES
# ---------------------------------------------------------------------------
header("PART 8 - DETECT ISSUES")
today = pd.Timestamp.today().normalize()
issues = {
    "Duplicate NAV records": int(nav.duplicated(subset=["FundID", "Date"]).sum()),
    "Negative NAV rows": int((nav["NAV"] < 0).sum()),
    "Future NAV dates": int((nav["Date"] > today).sum()),
    "Future purchase dates": int((transactions["PurchaseDate"] > today).sum()),
    "Missing Fund IDs (txn not in funds)":
        int((~transactions["FundID"].isin(funds["FundID"])).sum()),
    "Missing Investor IDs (txn not in investors)":
        int((~transactions["InvestorID"].isin(investors["InvestorID"])).sum()),
    "Invalid Purchase NAV (< 0)": int((transactions["PurchaseNAV"] < 0).sum()),
    "Funds missing NAV history": len(funds_missing_nav),
}
for k, v in issues.items():
    print(f"  {k:44}: {v}")


# ---------------------------------------------------------------------------
# PART 9 - FINANCE METRICS (per fund: return, volatility, Sharpe)
# ---------------------------------------------------------------------------
header("PART 9 - FINANCE METRICS")


def fund_return_pct(group: pd.DataFrame) -> float:
    g = group.sort_values("Date")
    first, last = g["NAV"].iloc[0], g["NAV"].iloc[-1]
    return (last - first) / first * 100 if first else np.nan


def fund_volatility(group: pd.DataFrame) -> float:
    g = group.sort_values("Date")
    daily_returns = g["NAV"].pct_change().dropna() * 100
    return float(np.std(daily_returns)) if len(daily_returns) else np.nan


rows = []
for fid, grp in nav.groupby("FundID"):
    ann_return = fund_return_pct(grp)          # holding period assumed 1 year
    vol = fund_volatility(grp)
    sharpe = (ann_return - RISK_FREE_RATE) / vol if vol else np.nan
    rows.append({
        "FundID": fid,
        "AnnualReturnPct": round(ann_return, 2),
        "AbsoluteReturnNAV": round(grp.sort_values("Date")["NAV"].iloc[-1]
                                   - grp.sort_values("Date")["NAV"].iloc[0], 2),
        "VolatilityPct": round(vol, 3),
        "SharpeRatio": round(sharpe, 3),
    })
fund_metrics = (
    pd.DataFrame(rows).merge(funds, on="FundID", how="left")
    [["FundID", "FundName", "AMC", "AnnualReturnPct", "AbsoluteReturnNAV",
      "VolatilityPct", "SharpeRatio"]]
    .sort_values("SharpeRatio", ascending=False)
)
print("Per-fund return / volatility / Sharpe (Risk Free = 6%):")
show(fund_metrics, n=20)


# ---------------------------------------------------------------------------
# PART 10 - EXPORT REPORTS
# ---------------------------------------------------------------------------
header("PART 10 - EXPORT REPORTS")
os.makedirs(OUTPUT_DIR, exist_ok=True)


def out(name: str) -> str:
    return os.path.join(OUTPUT_DIR, name)


# TopFunds.xlsx - fund level performance.
top_funds_report = (
    df.groupby(["FundID", "FundName", "Category", "AMC"], as_index=False)
    .agg(TotalInvestment=("InvestmentAmount", "sum"),
         TotalProfit=("Profit", "sum"),
         AvgROI=("ROI", "mean"),
         LatestNAV=("LatestNAV", "first"))
    .merge(fund_metrics[["FundID", "VolatilityPct", "SharpeRatio"]], on="FundID", how="left")
    .sort_values("TotalProfit", ascending=False)
    .round(2)
)
with pd.ExcelWriter(out("TopFunds.xlsx"), engine="openpyxl") as xl:
    top_funds_report.to_excel(xl, sheet_name="TopFunds", index=False)
    fund_metrics.round(3).to_excel(xl, sheet_name="RiskReturn", index=False)

# InvestorSummary.xlsx - investor level portfolio.
investor_report = (
    df.groupby(["InvestorID", "InvestorName", "State", "InvestorType"], as_index=False)
    .agg(TotalInvestment=("InvestmentAmount", "sum"),
         CurrentValue=("CurrentValue", "sum"),
         TotalProfit=("Profit", "sum"),
         AvgROI=("ROI", "mean"),
         FundsHeld=("FundID", "nunique"))
    .sort_values("TotalInvestment", ascending=False)
    .round(2)
)
with pd.ExcelWriter(out("InvestorSummary.xlsx"), engine="openpyxl") as xl:
    investor_report.to_excel(xl, sheet_name="InvestorSummary", index=False)
    by_state.reset_index().to_excel(xl, sheet_name="StateWise", index=False)
    by_type.reset_index().to_excel(xl, sheet_name="InvestorTypeWise", index=False)

# CategorySummary.csv - category level.
by_category.reset_index().to_csv(out("CategorySummary.csv"), index=False)

# analysis summary json.
summary = {
    "transactions_analysed": int(len(df)),
    "funds": int(funds["FundID"].nunique()),
    "investors": int(investors["InvestorID"].nunique()),
    "total_investment": float(round(df["InvestmentAmount"].sum(), 2)),
    "total_current_value": float(round(df["CurrentValue"].sum(), 2)),
    "total_profit": float(round(df["Profit"].sum(), 2)),
    "overall_roi_pct": float(round(df["Profit"].sum() / df["InvestmentAmount"].sum() * 100, 2)),
    "best_fund_by_profit": str(top5_funds.iloc[0]["FundName"]),
    "worst_fund_by_roi": str(worst_fund.iloc[0]["FundName"]),
    "data_issues": issues,
}
with open(out("analysis_summary.json"), "w", encoding="utf-8") as fh:
    json.dump(summary, fh, indent=2)

print("Written to ./output/:")
print("  - TopFunds.xlsx")
print("  - InvestorSummary.xlsx")
print("  - CategorySummary.csv")
print("  - analysis_summary.json")


# ---------------------------------------------------------------------------
# EXPECTED OUTPUTS (final display)
# ---------------------------------------------------------------------------
header("EXPECTED OUTPUTS")
print(">> Top Performing Funds (by profit)")
show(top5_funds.round(2))
print("\n>> Worst Performing Fund (lowest ROI)")
show(worst_fund.round(2))
print("\n>> State-wise Investment")
print(by_state[["TotalInvestment"]].to_string())
print("\n>> AMC-wise Investment")
print(by_amc[["TotalInvestment"]].to_string())
print("\n>> Category-wise ROI")
print(by_category[["AvgROI"]].to_string())
