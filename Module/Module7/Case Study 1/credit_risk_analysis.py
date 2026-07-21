"""
Case Study 1 - Credit Risk & Loan Portfolio Analysis (Hard)
===========================================================

Business scenario
-----------------
A bank wants to identify high-risk customers and calculate portfolio risk
metrics from its loan book.

Datasets (live next to this script)
-----------------------------------
  customers.csv     : CustomerID, Age, Salary, City
  credit_scores.csv : CustomerID, CreditScore
  loans.csv         : LoanID, CustomerID, LoanAmount, InterestRate, Tenure,
                      EMI, PaidEMIs, DefaultFlag

What this script does (mapped to the brief)
-------------------------------------------
  Python  : reads multiple CSVs with exception handling, functions, and an OOP `Loan` class.
  NumPy   : mean loan amount, median salary, interest-rate percentiles,
            Salary vs LoanAmount correlation, standard deviation.
  Pandas  : merges the three tables, ranks the Top-20 risky customers and
            filters customers by the risk rules in the brief.
  Cleaning: Salary -> median, CreditScore -> mean, InterestRate -> previous value.
  Outliers: drops loans with LoanAmount above the 99th percentile.
  Finance : Debt-to-Income, Loan Utilization, Default %, NPA %, Average EMI, Expected Loss.
  Output  : risk_report.xlsx, high_risk_customers.csv, summary.json.

Documented assumptions
----------------------
  * Debt-to-Income  = (EMI * 12) / Salary          (annual EMI burden vs annual income)
  * Loan Utilization= Outstanding / TotalPayable   (share of the loan still owed)
  * NPA %           = value of defaulted loans / total loan value   (value weighted)
  * Default %       = count of defaulted loans / number of loans    (count weighted)
  * Expected Loss   = PD * LGD * EAD, with PD = portfolio default rate,
                      LGD = 0.45 (45% loss given default), EAD = outstanding amount.
"""

import json
import os

import numpy as np
import pandas as pd

pd.set_option("display.max_columns", None)
pd.set_option("display.width", 160)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(BASE_DIR, "output")
LAKH = 100_000
LGD = 0.45  # Loss Given Default assumption for Expected Loss


def p(name: str) -> str:
    """Absolute path to a file that lives next to this script."""
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
        print(f"  loaded {filename:20} -> {df.shape[0]:>4} rows x {df.shape[1]} cols")
        return df
    except FileNotFoundError:
        raise SystemExit(f"ERROR: required file not found -> {path}")
    except pd.errors.EmptyDataError:
        raise SystemExit(f"ERROR: no data in file -> {path}")
    except pd.errors.ParserError as exc:
        raise SystemExit(f"ERROR: could not parse (corrupted?) -> {path}\n{exc}")


class Loan:
    """OOP view of a single loan used for per-loan risk calculations."""

    def __init__(self, loan_id, amount, emi, tenure, paid_emis, rate,
                 salary, credit_score, default_flag):
        self.loan_id = loan_id
        self.amount = float(amount)
        self.emi = float(emi)
        self.tenure = int(tenure)
        self.paid_emis = int(paid_emis)
        self.rate = float(rate)
        self.salary = float(salary)
        self.credit_score = float(credit_score)
        self.default_flag = int(default_flag)

    @property
    def total_payable(self) -> float:
        return self.emi * self.tenure

    @property
    def amount_paid(self) -> float:
        return self.emi * self.paid_emis

    @property
    def outstanding(self) -> float:
        return max(self.total_payable - self.amount_paid, 0.0)

    @property
    def debt_to_income(self) -> float:
        return (self.emi * 12) / self.salary if self.salary else np.nan

    @property
    def loan_utilization(self) -> float:
        return self.outstanding / self.total_payable if self.total_payable else np.nan

    @property
    def expected_loss(self) -> float:
        """Realised expected loss on this exposure (LGD * outstanding if defaulted)."""
        return self.default_flag * LGD * self.outstanding

    def risk_score(self) -> int:
        """Simple additive risk score (higher = riskier)."""
        score = 0
        score += 2 if self.default_flag == 1 else 0
        score += 1 if self.credit_score < 650 else 0
        score += 1 if self.salary < 60_000 else 0
        score += 1 if self.amount > 10 * LAKH else 0
        score += 1 if self.debt_to_income > 0.5 else 0
        return score


# ---------------------------------------------------------------------------
# PART 1 - READ DATA
# ---------------------------------------------------------------------------
header("PART 1 - READ DATA")
customers = read_csv_safe("customers.csv")
credit = read_csv_safe("credit_scores.csv")
loans = read_csv_safe("loans.csv")


# ---------------------------------------------------------------------------
# PART 2 - DATA CLEANING (missing values + duplicates)
# ---------------------------------------------------------------------------
header("PART 2 - DATA CLEANING")

before = len(customers) + len(credit) + len(loans)
customers = customers.drop_duplicates()
credit = credit.drop_duplicates(subset="CustomerID")
loans = loans.drop_duplicates().drop_duplicates(subset="LoanID")
print(f"Duplicate rows removed: {before - (len(customers) + len(credit) + len(loans))}")

print("\nMissing values before cleaning:")
print("  customers :", int(customers.isna().sum().sum()))
print("  credit    :", int(credit.isna().sum().sum()))
print("  loans     :", int(loans.isna().sum().sum()))

# Salary -> median, CreditScore -> mean, InterestRate -> previous value (forward fill).
customers["Salary"] = customers["Salary"].fillna(customers["Salary"].median())
credit["CreditScore"] = credit["CreditScore"].fillna(round(credit["CreditScore"].mean()))
loans = loans.sort_values("LoanID")
loans["InterestRate"] = loans["InterestRate"].ffill().bfill()
print("\nApplied: Salary->median | CreditScore->mean | InterestRate->previous value (ffill)")


# ---------------------------------------------------------------------------
# PART 3 - OUTLIER REMOVAL (LoanAmount > 99th percentile)
# ---------------------------------------------------------------------------
header("PART 3 - OUTLIER REMOVAL")
p99 = np.percentile(loans["LoanAmount"], 99)
outliers = int((loans["LoanAmount"] > p99).sum())
loans = loans[loans["LoanAmount"] <= p99]
print(f"99th percentile LoanAmount : {p99:,.2f}")
print(f"Outlier loans removed      : {outliers}")


# ---------------------------------------------------------------------------
# PART 4 - MERGE DATASETS
# ---------------------------------------------------------------------------
header("PART 4 - MERGE DATASETS")
df = (
    loans.merge(customers, on="CustomerID", how="inner")
    .merge(credit, on="CustomerID", how="inner")
)
print("Merged shape:", df.shape)
core = ["LoanID", "CustomerID", "City", "Salary", "CreditScore",
        "LoanAmount", "InterestRate", "EMI", "DefaultFlag"]
show(df[core])


# ---------------------------------------------------------------------------
# PART 5 - NUMPY STATISTICS
# ---------------------------------------------------------------------------
header("PART 5 - NUMPY STATISTICS")
loan_amt = df["LoanAmount"].to_numpy()
salary = df["Salary"].to_numpy()
rate = df["InterestRate"].to_numpy()

stats = {
    "Mean Loan Amount": float(np.mean(loan_amt)),
    "Median Salary": float(np.median(salary)),
    "Interest Rate 25th pct": float(np.percentile(rate, 25)),
    "Interest Rate 50th pct": float(np.percentile(rate, 50)),
    "Interest Rate 75th pct": float(np.percentile(rate, 75)),
    "Interest Rate 90th pct": float(np.percentile(rate, 90)),
    "Corr(Salary, LoanAmount)": float(np.corrcoef(salary, loan_amt)[0, 1]),
    "LoanAmount Std Dev": float(np.std(loan_amt)),
}
for k, v in stats.items():
    print(f"  {k:26}: {v:,.4f}" if "Corr" in k else f"  {k:26}: {v:,.2f}")


# ---------------------------------------------------------------------------
# PART 6 - NEW COLUMNS / PER-LOAN FINANCE METRICS (via the Loan class)
# ---------------------------------------------------------------------------
header("PART 6 - PER-LOAN FINANCE METRICS")
loan_objects = [
    Loan(r.LoanID, r.LoanAmount, r.EMI, r.Tenure, r.PaidEMIs, r.InterestRate,
         r.Salary, r.CreditScore, r.DefaultFlag)
    for r in df.itertuples(index=False)
]
df["TotalPayable"] = [lo.total_payable for lo in loan_objects]
df["AmountPaid"] = [lo.amount_paid for lo in loan_objects]
df["Outstanding"] = [lo.outstanding for lo in loan_objects]
df["DebtToIncome"] = [lo.debt_to_income for lo in loan_objects]
df["LoanUtilization"] = [lo.loan_utilization for lo in loan_objects]
df["ExpectedLoss"] = [lo.expected_loss for lo in loan_objects]
df["RiskScore"] = [lo.risk_score() for lo in loan_objects]

show(df[["LoanID", "DebtToIncome", "LoanUtilization", "Outstanding",
         "ExpectedLoss", "RiskScore"]].round(3))


# ---------------------------------------------------------------------------
# PART 7 - PANDAS ANALYSIS (risky customers + rule based filters)
# ---------------------------------------------------------------------------
header("PART 7 - PANDAS ANALYSIS")

top20_risky = df.sort_values(
    ["RiskScore", "ExpectedLoss", "LoanAmount"], ascending=False
).head(20)
print("Top 20 risky customers:")
show(top20_risky[["CustomerID", "LoanID", "City", "CreditScore", "Salary",
                  "LoanAmount", "DefaultFlag", "RiskScore", "ExpectedLoss"]].round(2), n=20)

rule_hits = df[
    (df["CreditScore"] < 650)
    & (df["Salary"] < 60_000)
    & (df["LoanAmount"] > 10 * LAKH)
    & (df["DefaultFlag"] == 1)
]
print("\nCustomers matching ALL brief rules "
      "(CreditScore<650 & Salary<60k & Loan>10L & Default=1):")
show(rule_hits[["CustomerID", "LoanID", "CreditScore", "Salary", "LoanAmount", "DefaultFlag"]])

# Individual rule breakdowns (useful context).
print("\nRule breakdown (row counts):")
print(f"  CreditScore < 650      : {(df['CreditScore'] < 650).sum()}")
print(f"  Salary < 60,000        : {(df['Salary'] < 60_000).sum()}")
print(f"  LoanAmount > 10 Lakhs  : {(df['LoanAmount'] > 10 * LAKH).sum()}")
print(f"  DefaultFlag == 1       : {(df['DefaultFlag'] == 1).sum()}")


# ---------------------------------------------------------------------------
# PART 8 - PORTFOLIO FINANCE METRICS
# ---------------------------------------------------------------------------
header("PART 8 - PORTFOLIO FINANCE METRICS")
n_loans = len(df)
total_loan_value = df["LoanAmount"].sum()
defaulted = df[df["DefaultFlag"] == 1]

default_pct = len(defaulted) / n_loans * 100
npa_pct = defaulted["LoanAmount"].sum() / total_loan_value * 100
avg_emi = df["EMI"].mean()
avg_dti = df["DebtToIncome"].mean()

# Portfolio Expected Loss = PD * LGD * EAD (sum of outstanding exposure).
pd_rate = len(defaulted) / n_loans
portfolio_ead = df["Outstanding"].sum()
portfolio_expected_loss = pd_rate * LGD * portfolio_ead

finance = pd.DataFrame(
    {
        "Metric": [
            "Number of Loans", "Total Loan Value", "Default %", "NPA %",
            "Average EMI", "Average Debt-to-Income", "Probability of Default",
            "Portfolio Exposure (EAD)", "Portfolio Expected Loss (PD*LGD*EAD)",
        ],
        "Value": [
            n_loans, round(total_loan_value, 2), round(default_pct, 2), round(npa_pct, 2),
            round(avg_emi, 2), round(avg_dti, 4), round(pd_rate, 4),
            round(portfolio_ead, 2), round(portfolio_expected_loss, 2),
        ],
    }
)
print(finance.to_string(index=False))


# ---------------------------------------------------------------------------
# PART 9 - GROUP BY (city-wise risk view)
# ---------------------------------------------------------------------------
header("PART 9 - CITY-WISE RISK SUMMARY")
city_summary = (
    df.groupby("City")
    .agg(
        Loans=("LoanID", "count"),
        AvgSalary=("Salary", "mean"),
        AvgCreditScore=("CreditScore", "mean"),
        TotalLoan=("LoanAmount", "sum"),
        Defaults=("DefaultFlag", "sum"),
        ExpectedLoss=("ExpectedLoss", "sum"),
    )
    .assign(DefaultRatePct=lambda x: (x["Defaults"] / x["Loans"] * 100))
    .sort_values("ExpectedLoss", ascending=False)
    .round(2)
)
print(city_summary.to_string())


# ---------------------------------------------------------------------------
# PART 10 - EXPORT REPORTS (automation)
# ---------------------------------------------------------------------------
header("PART 10 - EXPORT REPORTS")
os.makedirs(OUTPUT_DIR, exist_ok=True)


def out(name: str) -> str:
    return os.path.join(OUTPUT_DIR, name)


# High-risk customers = risk score of 2 or more.
high_risk = df[df["RiskScore"] >= 2].sort_values("RiskScore", ascending=False)
high_risk_cols = ["CustomerID", "LoanID", "City", "Salary", "CreditScore",
                  "LoanAmount", "EMI", "DefaultFlag", "DebtToIncome",
                  "ExpectedLoss", "RiskScore"]
high_risk[high_risk_cols].round(2).to_csv(out("high_risk_customers.csv"), index=False)

with pd.ExcelWriter(out("risk_report.xlsx"), engine="openpyxl") as xl:
    pd.DataFrame(stats.items(), columns=["Metric", "Value"]).to_excel(
        xl, sheet_name="NumpyStats", index=False)
    finance.to_excel(xl, sheet_name="PortfolioMetrics", index=False)
    city_summary.reset_index().to_excel(xl, sheet_name="CityWise", index=False)
    top20_risky[high_risk_cols].round(2).to_excel(
        xl, sheet_name="Top20Risky", index=False)
    high_risk[high_risk_cols].round(2).to_excel(
        xl, sheet_name="HighRiskCustomers", index=False)

summary = {
    "loans_analysed": int(n_loans),
    "total_loan_value": float(round(total_loan_value, 2)),
    "default_pct": float(round(default_pct, 2)),
    "npa_pct": float(round(npa_pct, 2)),
    "average_emi": float(round(avg_emi, 2)),
    "average_debt_to_income": float(round(avg_dti, 4)),
    "portfolio_expected_loss": float(round(portfolio_expected_loss, 2)),
    "high_risk_customers": int(len(high_risk)),
    "outliers_removed": int(outliers),
}
with open(out("summary.json"), "w", encoding="utf-8") as fh:
    json.dump(summary, fh, indent=2)

print("Written to ./output/:")
print("  - risk_report.xlsx")
print("  - high_risk_customers.csv")
print("  - summary.json")


# ---------------------------------------------------------------------------
# EXPECTED OUTPUTS (final display)
# ---------------------------------------------------------------------------
header("EXPECTED OUTPUTS")
print(">> Top 20 Risky Customers")
show(top20_risky[["CustomerID", "City", "CreditScore", "LoanAmount",
                  "DefaultFlag", "RiskScore"]], n=20)
print("\n>> Portfolio Risk Report")
print(finance.to_string(index=False))
print(f"\n>> High-risk customers exported: {len(high_risk)}")
