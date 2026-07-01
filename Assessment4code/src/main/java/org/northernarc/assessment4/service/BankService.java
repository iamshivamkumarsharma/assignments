package org.northernarc.assessment4.service;


import org.northernarc.assessment4.dto.CustomerSummaryDTO;
import org.northernarc.assessment4.dto.DashboardResponse;
import org.northernarc.assessment4.model.Account;
import org.northernarc.assessment4.model.Customer;
import org.northernarc.assessment4.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface BankService {

    // Core Entity Operations & Validation (Task 1 & Task 2)
    Customer saveCustomer(Customer customer);
    Account saveAccount(Account account);
    void deleteAccount(String accountNumber);

    // Spring Data JPA Derived Queries (Task 3)
    List<Account> getAccountsByType(String accountType);
    List<Customer> getCustomersByBranch(String branch);
    List<Transaction> getTransactionsByType(String transactionType);
    List<Account> getAccountsWithBalanceGreaterThan(double amount);

    // JPQL Custom Queries (Task 4)
    List<Customer> getRichCustomers(double threshold);
    Map<String, Double> getTotalBalancePerBranch();
    List<Customer> getCustomersWithMultipleAccounts();
    Transaction getLatestTransaction();
    List<Account> getAccountsWithNoTransactions();

    // JPQL Update Query (Task 5)
    void increaseAccountBalance(String accountNumber, double amount);

    // Pagination & Sorting (Task 6)
    Page<Account> getAllAccountsPaginated(Pageable pageable);

    // DTO Projection Mapping Layer (Task 7)
    CustomerSummaryDTO getCustomerSummary(Long customerId);

    // Final Challenge Metrics (Optimized, Single Query Strategy)
    DashboardResponse getDashboardMetrics();
}
