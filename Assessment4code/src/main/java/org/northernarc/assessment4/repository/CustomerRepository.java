package org.northernarc.assessment4.repository;

import org.northernarc.assessment4.dto.CustomerSummaryDTO;
import org.northernarc.assessment4.model.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Task 3: Derived Query Methods
    List<Customer> findByBranch(String branch);

    // Security Helpers
    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    // Task 4: Rich customers whose total account balance exceeds a threshold
    @Query("SELECT c FROM Customer c WHERE " +
            "(SELECT COALESCE(SUM(a.balance), 0.0) FROM Account a WHERE a.customer = c) > :threshold")
    List<Customer> findRichCustomers(@Param("threshold") double threshold);

    // Task 4: Total balance per branch (branch, totalBalance)
    @Query("SELECT c.branch, SUM(a.balance) FROM Customer c JOIN c.accounts a GROUP BY c.branch")
    List<Object[]> findTotalBalancePerBranch();

    // Task 4: Customers holding more than one account
    @Query("SELECT c FROM Customer c WHERE " +
            "(SELECT COUNT(a) FROM Account a WHERE a.customer = c) > 1")
    List<Customer> findCustomersWithMultipleAccounts();

    // Task 7: DTO projection for a single customer summary
    @Query("SELECT new org.northernarc.assessment4.dto.CustomerSummaryDTO(" +
            "c.customerName, c.branch, COUNT(a), COALESCE(SUM(a.balance), 0.0)) " +
            "FROM Customer c LEFT JOIN c.accounts a " +
            "WHERE c.customerId = :customerId GROUP BY c.customerName, c.branch")
    Optional<CustomerSummaryDTO> findCustomerSummary(@Param("customerId") Long customerId);

    // Final Challenge: branch with the highest aggregated balance
    @Query("SELECT c.branch FROM Customer c JOIN c.accounts a GROUP BY c.branch ORDER BY SUM(a.balance) DESC")
    List<String> findTopBranches(Pageable pageable);

    // Final Challenge: customer holding the highest aggregated balance
    @Query("SELECT c.customerName FROM Customer c JOIN c.accounts a GROUP BY c.customerName ORDER BY SUM(a.balance) DESC")
    List<String> findTopCustomers(Pageable pageable);
}
