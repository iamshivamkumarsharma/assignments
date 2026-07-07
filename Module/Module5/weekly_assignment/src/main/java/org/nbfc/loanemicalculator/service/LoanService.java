package org.nbfc.loanemicalculator.service;

import org.nbfc.loanemicalculator.dto.CreateLoanRequest;
import org.nbfc.loanemicalculator.dto.CustomerSummaryDTO;
import org.nbfc.loanemicalculator.dto.DashboardDTO;
import org.nbfc.loanemicalculator.dto.LoanDetailDTO;
import org.nbfc.loanemicalculator.dto.PayEmiRequest;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.nbfc.loanemicalculator.entity.Loan;
import org.nbfc.loanemicalculator.entity.PenaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanService {
    List<Loan> findByLoanType(String loanType);
    List<Customer> findByBranchName(String branchName);
    List<PenaltyTransaction> findByPaymentMode(String paymentMode);
    List<Loan> findByInterestRateGreaterThan(Double rate);
    List<EmiSchedule> findByStatus(String status);
    List<Customer> findHighValueBorrowers(long count);
    List<Object[]> findBranchWisePenaltyCollection();
    List<Customer> findCustomersWithMultipleLoanTypes();
    PenaltyTransaction findLatestPenalty();
    List<Loan> findLoansWithoutOverdue();
    int increaseInterestRate(String loanType, double amount);
    Page<Loan> getLoans(Pageable pageable);
    CustomerSummaryDTO getCustomerSummary(Long customerId);
    void deleteLoan(Long loanId);
    void deletePenalty(Long transactionId);
    DashboardDTO getDashboard();

    // --- Added: advanced fetching, pagination and write operations ---
    List<LoanDetailDTO> getLoanDetails();
    Page<EmiSchedule> getEmisByStatus(String status, Pageable pageable);
    Page<PenaltyTransaction> getPenaltiesByMode(String paymentMode, Pageable pageable);
    Loan createLoan(CreateLoanRequest request);
    EmiSchedule payEmi(Long emiId, PayEmiRequest request);
}
