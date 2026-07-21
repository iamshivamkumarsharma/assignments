"""
Case Study 3 - Loan Processing & Loan Repayment Analytics (ABCBank)
==================================================================

Datasets available in this workspace:
  customers.csv        : CustomerID, CustomerName, Age, Gender, PAN, Aadhaar, Salary, City, State
  loan_application.csv : LoanID, CustomerID, LoanType, LoanAmount, InterestRate, Tenure,
                         ApplicationDate, BranchID, LoanStatus
  loan_payments.csv    : LoanID, EMIAmount, PaidEMIs, PendingEMIs, LastPaymentDate

Adaptations made because the supplied files differ slightly from the brief
--------------------------------------------------------------------------
1. CreditScore is NOT present in customers.csv. It is generated here (seeded and
   salary-correlated) only so the credit-related analysis can run. It is clearly
   synthetic - swap in a real column and every downstream step still works.
2. The payments file is installment/count based (PaidEMIs / PendingEMIs) instead of
   a single AmountPaid / PaymentStatus. We therefore DERIVE:
       AmountPaid           = PaidEMIs            * EMIAmount   (collected so far)
       TotalPayable         = (PaidEMIs+PendingEMIs) * EMIAmount
       EMIDue (outstanding) = PendingEMIs         * EMIAmount   (= TotalPayable - AmountPaid)
       PaymentCompletion %  = AmountPaid / TotalPayable * 100
       PaymentStatus        = Paid / Partial / Pending
   (The brief's "EMI Amount" in the Part-4 formulas is read as this total EMI value,
   so the numbers stay financially meaningful.)
3. Join keys are inconsistent and are normalised before merging:
       customers.CustomerID 'C101'  vs loan_application.CustomerID 101  -> prefix 'C'
       loan_application.LoanID 'L1001' vs loan_payments.LoanID 'L101'   -> match on the
       trailing sequence number (mod 100).
"""

import os

import numpy as np
import pandas as pd

pd.set_option("display.max_columns", None)
pd.set_option("display.width", 160)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LAKH = 100_000  # 1 Lakh = 1,00,000


def p(name: str) -> str:
    """Absolute path to a file that lives next to this script."""
    return os.path.join(BASE_DIR, name)


def header(title: str) -> None:
    print("\n" + "=" * 80)
    print(title)
    print("=" * 80)


def show(df: pd.DataFrame, n: int = 10) -> None:
    print("(none)" if len(df) == 0 else df.head(n).to_string(index=False))


# ---------------------------------------------------------------------------
# PART 1 - READ DATA
# ---------------------------------------------------------------------------
header("PART 1 - READ DATA")
customers = pd.read_csv(p("customers.csv"))
loans = pd.read_csv(p("loan_application.csv"))
payments = pd.read_csv(p("loan_payments.csv"))
print("customers        :", customers.shape)
print("loan_application :", loans.shape)
print("loan_payments    :", payments.shape)


# ---------------------------------------------------------------------------
# PART 2 - DATA CLEANING
# ---------------------------------------------------------------------------
header("PART 2 - DATA CLEANING")

# CreditScore is missing from the source -> generate a seeded, salary-correlated column.
if "CreditScore" not in customers.columns:
    rng = np.random.default_rng(42)
    s = customers["Salary"].astype(float)
    base = 600 + (s - s.min()) / (s.max() - s.min()) * 180  # ~600..780 by salary
    noise = rng.normal(0, 45, len(customers))
    customers["CreditScore"] = np.clip(np.round(base + noise), 300, 900).astype(int)
    print("Note: 'CreditScore' was absent -> generated a seeded, salary-correlated column.")

print("\nMissing values before cleaning:")
print("  customers :", customers.isna().sum().sum())
print("  loans     :", loans.isna().sum().sum())
print("  payments  :", payments.isna().sum().sum())

# Remove duplicate records + duplicate Loan IDs.
before = len(customers) + len(loans) + len(payments)
customers = customers.drop_duplicates()
loans = loans.drop_duplicates().drop_duplicates(subset="LoanID")
payments = payments.drop_duplicates().drop_duplicates(subset="LoanID")
print(f"\nDuplicate rows removed: {before - (len(customers) + len(loans) + len(payments))}")

# Replace missing Salary with median, missing Credit Score with mean.
sal_missing = int(customers["Salary"].isna().sum())
cs_missing = int(customers["CreditScore"].isna().sum())
customers["Salary"] = customers["Salary"].fillna(customers["Salary"].median())
customers["CreditScore"] = customers["CreditScore"].fillna(round(customers["CreditScore"].mean()))
print(f"Missing Salary filled with median   : {sal_missing}")
print(f"Missing CreditScore filled with mean: {cs_missing}")

# Convert ApplicationDate and PaymentDate to datetime (source calls it LastPaymentDate).
loans["ApplicationDate"] = pd.to_datetime(loans["ApplicationDate"], errors="coerce")
payments = payments.rename(columns={"LastPaymentDate": "PaymentDate"})
payments["PaymentDate"] = pd.to_datetime(payments["PaymentDate"], errors="coerce")

# Remove negative loan amounts, invalid EMIs and future payment dates.
neg = int((loans["LoanAmount"] < 0).sum())
loans = loans[loans["LoanAmount"] >= 0]

bad_emi = int((~(payments["EMIAmount"] > 0)).sum())
payments = payments[payments["EMIAmount"] > 0]

today = pd.Timestamp.today().normalize()
future = int((payments["PaymentDate"] > today).sum())
payments = payments[payments["PaymentDate"].isna() | (payments["PaymentDate"] <= today)]
print(f"Removed -> negative LoanAmounts: {neg} | invalid EMIs: {bad_emi} | future dates: {future}")


# ---------------------------------------------------------------------------
# PART 3 - MERGE DATASETS
# ---------------------------------------------------------------------------
header("PART 3 - MERGE DATASETS")

# Normalise CustomerID: 101 -> C101 to match the customers table.
loans["CustomerID"] = loans["CustomerID"].apply(
    lambda x: str(x) if str(x).startswith("C") else f"C{x}"
)

# Normalise LoanID: application 'L1001' <-> payment 'L101' via trailing sequence number.
loans["LoanKey"] = loans["LoanID"].str.extract(r"(\d+)", expand=False).astype(int) % 100
payments["LoanKey"] = payments["LoanID"].str.extract(r"(\d+)", expand=False).astype(int) % 100
assert loans["LoanKey"].is_unique and payments["LoanKey"].is_unique, "Loan sequence keys collided"

merged = (
    customers.merge(loans, on="CustomerID", how="inner")
    .merge(payments, on="LoanKey", how="inner", suffixes=("", "_pay"))
)

# Derive PaymentStatus from the installment counts.
merged["PaymentStatus"] = np.select(
    [merged["PendingEMIs"].eq(0), merged["PaidEMIs"].eq(0)],
    ["Paid", "Pending"],
    default="Partial",
)

print("Merged shape:", merged.shape)
core_cols = [
    "CustomerName", "City", "LoanType", "LoanAmount", "CreditScore",
    "Salary", "LoanStatus", "EMIAmount", "PaymentStatus",
]
print("\nSingle unified dataframe (requested columns):")
show(merged[core_cols])


# ---------------------------------------------------------------------------
# PART 4 - CREATE NEW COLUMNS
# ---------------------------------------------------------------------------
header("PART 4 - CREATE NEW COLUMNS")
merged["MonthlyIncome"] = merged["Salary"] / 12
merged["DebtToIncome"] = merged["LoanAmount"] / merged["Salary"]

merged["TotalEMIs"] = merged["PaidEMIs"] + merged["PendingEMIs"]
merged["TotalPayable"] = merged["TotalEMIs"] * merged["EMIAmount"]
merged["AmountPaid"] = merged["PaidEMIs"] * merged["EMIAmount"]
merged["EMIDue"] = merged["PendingEMIs"] * merged["EMIAmount"]
merged["PaymentCompletion"] = merged["AmountPaid"] / merged["TotalPayable"] * 100

show(
    merged[
        ["CustomerName", "MonthlyIncome", "DebtToIncome", "AmountPaid", "EMIDue", "PaymentCompletion"]
    ].round(2)
)


# ---------------------------------------------------------------------------
# PART 5 - NUMPY TASKS (on Loan Amount)
# ---------------------------------------------------------------------------
header("PART 5 - NUMPY LOAN AMOUNT STATISTICS")
la = merged["LoanAmount"].to_numpy()
loan_stats = {
    "Average": np.mean(la),
    "Median": np.median(la),
    "Maximum": np.max(la),
    "Minimum": np.min(la),
    "Std Deviation": np.std(la),
    "Variance": np.var(la),
    "25th Percentile": np.percentile(la, 25),
    "75th Percentile": np.percentile(la, 75),
}
for k, v in loan_stats.items():
    print(f"  {k:16}: {v:,.2f}")


# ---------------------------------------------------------------------------
# PART 6 - PANDAS ANALYSIS
# ---------------------------------------------------------------------------
header("PART 6 - PANDAS ANALYSIS")

top10_loan = merged.nlargest(10, "LoanAmount")[["CustomerName", "City", "LoanType", "LoanAmount"]]
top10_salary = merged.nlargest(10, "Salary")[["CustomerName", "City", "Salary"]]
low_credit = merged[merged["CreditScore"] < 650][["CustomerName", "City", "CreditScore"]]
big_loans = merged[merged["LoanAmount"] > 20 * LAKH][["CustomerName", "LoanType", "LoanAmount"]]
pending_pay = merged[merged["PendingEMIs"] > 0][
    ["CustomerName", "LoanID", "EMIAmount", "PendingEMIs", "EMIDue", "PaymentStatus"]
]
fully_paid = merged[merged["PendingEMIs"] == 0][["CustomerName", "LoanID", "EMIAmount"]]

print("Top 10 highest-loan customers:")
show(top10_loan)
print("\nTop 10 customers by salary:")
show(top10_salary)
print("\nCustomers with Credit Score below 650:")
show(low_credit)
print("\nCustomers with Loan Amount > Rs 20 Lakhs:")
show(big_loans)
print("\nLoans with pending payments:")
show(pending_pay)
print("\nFully paid loans:")
show(fully_paid)


# ---------------------------------------------------------------------------
# PART 7 - GROUP BY
# ---------------------------------------------------------------------------
header("PART 7 - GROUP BY")

city_summary = (
    merged.groupby("City")
    .agg(
        Customers=("CustomerID", "nunique"),
        AvgSalary=("Salary", "mean"),
        TotalLoanAmount=("LoanAmount", "sum"),
    )
    .sort_values("TotalLoanAmount", ascending=False)
    .round(2)
)

type_summary = (
    merged.groupby("LoanType")
    .agg(
        NumberOfLoans=("LoanID", "count"),
        AvgLoanAmount=("LoanAmount", "mean"),
        TotalLoanAmount=("LoanAmount", "sum"),
    )
    .sort_values("TotalLoanAmount", ascending=False)
    .round(2)
)

status_counts = merged["LoanStatus"].value_counts()
approved = int(status_counts.get("Approved", 0))
pending_loans = int(status_counts.get("Pending", 0))
rejected = int(status_counts.get("Rejected", 0))

paystatus_summary = merged.groupby("PaymentStatus").agg(
    Count=("LoanID", "count"),
    TotalAmountPaid=("AmountPaid", "sum"),
)

print("Group by City:")
print(city_summary.to_string())
print("\nGroup by Loan Type:")
print(type_summary.to_string())
print("\nGroup by Loan Status:")
print(f"  Approved: {approved} | Pending: {pending_loans} | Rejected: {rejected}")
print("\nGroup by Payment Status (count & total amount paid):")
print(paystatus_summary.to_string())


# ---------------------------------------------------------------------------
# PART 8 - BUSINESS RULES (flags)
# ---------------------------------------------------------------------------
header("PART 8 - BUSINESS RULE FLAGS")
merged["Flag_HighLoan"] = merged["LoanAmount"] > 30 * LAKH
merged["Flag_LowCredit"] = merged["CreditScore"] < 650
merged["Flag_LowSalary"] = merged["Salary"] < 30_000
merged["Flag_HighDTI"] = merged["DebtToIncome"] > 5
merged["Flag_HighEMIDue"] = merged["EMIDue"] > 10_000
merged["Flag_PaymentPending"] = merged["PaymentStatus"] == "Pending"
merged["Flag_Rejected"] = merged["LoanStatus"] == "Rejected"

flag_cols = [c for c in merged.columns if c.startswith("Flag_")]
merged["RiskFlags"] = merged[flag_cols].sum(axis=1)

print("Flag counts across the portfolio:")
print(merged[flag_cols].sum().to_string())
print("\nFlagged customers (risk flag count per loan):")
show(merged[["CustomerName", "LoanID", *flag_cols, "RiskFlags"]], n=25)


# ---------------------------------------------------------------------------
# PART 9 - FINANCE METRICS
# ---------------------------------------------------------------------------
header("PART 9 - FINANCE METRICS")
total_portfolio = merged["LoanAmount"].sum()
total_collected = merged["AmountPaid"].sum()
outstanding = total_portfolio - total_collected
recovery_pct = total_collected / total_portfolio * 100
default_pct = pending_loans / len(merged) * 100
avg_emi = merged["EMIAmount"].mean()
avg_credit = merged["CreditScore"].mean()

finance = pd.DataFrame(
    {
        "Metric": [
            "Total Loan Portfolio",
            "Total Amount Collected",
            "Outstanding Amount",
            "Loan Recovery %",
            "Default % (pending loans)",
            "Average EMI",
            "Average Credit Score",
        ],
        "Value": [
            total_portfolio,
            total_collected,
            outstanding,
            round(recovery_pct, 2),
            round(default_pct, 2),
            round(avg_emi, 2),
            round(avg_credit, 2),
        ],
    }
)
print(finance.to_string(index=False))


# ---------------------------------------------------------------------------
# PART 10 - EXPORT REPORTS
# ---------------------------------------------------------------------------
header("PART 10 - EXPORT REPORTS")

with pd.ExcelWriter(p("LoanSummary.xlsx"), engine="openpyxl") as xl:
    pd.DataFrame(loan_stats.items(), columns=["Metric", "LoanAmount"]).to_excel(
        xl, sheet_name="LoanAmountStats", index=False
    )
    city_summary.reset_index().to_excel(xl, sheet_name="CityWise", index=False)
    type_summary.reset_index().to_excel(xl, sheet_name="LoanTypeWise", index=False)
    status_counts.rename_axis("LoanStatus").reset_index(name="Count").to_excel(
        xl, sheet_name="LoanStatus", index=False
    )
    paystatus_summary.reset_index().to_excel(xl, sheet_name="PaymentStatus", index=False)
    finance.to_excel(xl, sheet_name="FinanceMetrics", index=False)

report_cols = [
    "CustomerID", "CustomerName", "City", "State", "LoanID", "LoanType", "LoanAmount",
    "InterestRate", "Tenure", "LoanStatus", "CreditScore", "Salary", "MonthlyIncome",
    "DebtToIncome", "EMIAmount", "PaidEMIs", "PendingEMIs", "AmountPaid", "EMIDue",
    "PaymentCompletion", "PaymentStatus", "RiskFlags",
]
merged[report_cols].round(2).to_excel(p("CustomerLoanReport.xlsx"), index=False)

pending_pay.to_csv(p("PendingPayments.csv"), index=False)

print("Written:")
print("  - LoanSummary.xlsx")
print("  - CustomerLoanReport.xlsx")
print("  - PendingPayments.csv")


# ---------------------------------------------------------------------------
# EXPECTED OUTPUTS (final display)
# ---------------------------------------------------------------------------
header("EXPECTED OUTPUTS")

print(">> Top 10 Loan Customers")
show(top10_loan)

print("\n>> Customers with Low Credit Score (< 650)")
show(low_credit)

print("\n>> Pending Loan Payments")
show(pending_pay)

print("\n>> City-wise Loan Summary")
print(city_summary.to_string())

print("\n>> Loan Type Summary")
print(type_summary.to_string())

print("\n>> Loan Recovery Report")
print(f"   Total Loan Portfolio   : Rs {total_portfolio:,.2f}")
print(f"   Total Amount Collected : Rs {total_collected:,.2f}")
print(f"   Outstanding Amount     : Rs {outstanding:,.2f}")
print(f"   Loan Recovery %        : {recovery_pct:.2f}%")
print(f"   Default % (pending)    : {default_pct:.2f}%")
