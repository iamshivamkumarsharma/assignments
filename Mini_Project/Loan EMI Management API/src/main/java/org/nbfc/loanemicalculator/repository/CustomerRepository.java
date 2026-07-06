package org.nbfc.loanemicalculator.repository;

import org.nbfc.loanemicalculator.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByBranchName(String branchName);

    Optional<Customer> findByEmail(String email);

    /**
     * Eagerly loads a customer together with their EMI schedules to avoid a lazy-load
     * N+1 when the schedules are traversed outside the persistence context.
     */
    @EntityGraph(attributePaths = "emiSchedules")
    Optional<Customer> findWithEmiSchedulesByCustomerId(Long customerId);

    @Query("SELECT c FROM Customer c JOIN c.emiSchedules e GROUP BY c HAVING COUNT(DISTINCT e.loan) > :count")
    List<Customer> findHighValueBorrowers(long count);

    @Query("SELECT c FROM Customer c JOIN c.emiSchedules e GROUP BY c HAVING COUNT(DISTINCT e.loan.loanType) > 1")
    List<Customer> findCustomersWithMultipleLoanTypes();

    @Query("SELECT c.branchName, SUM(p.penaltyAmount) FROM Customer c " +
            "JOIN c.emiSchedules e JOIN e.loan l JOIN l.penaltyTransactions p GROUP BY c.branchName")
    List<Object[]> findBranchWisePenaltyCollection();

    @Query("SELECT c.branchName FROM Customer c JOIN c.emiSchedules e JOIN e.loan l JOIN l.penaltyTransactions p " +
            "GROUP BY c.branchName ORDER BY SUM(p.penaltyAmount) DESC")
    List<String> findTopBranches(Pageable pageable);

    @Query("SELECT c.customerName FROM Customer c JOIN c.emiSchedules e JOIN e.loan l JOIN l.penaltyTransactions p " +
            "GROUP BY c.customerId, c.customerName ORDER BY SUM(p.penaltyAmount) DESC")
    List<String> findTopPenaltyPayingCustomers(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT e.loan) FROM EmiSchedule e WHERE e.customer.customerId = :id")
    Long countLoans(Long id);

    @Query("SELECT COALESCE(SUM(p.penaltyAmount), 0) FROM PenaltyTransaction p WHERE p.loan IN " +
            "(SELECT DISTINCT e.loan FROM EmiSchedule e WHERE e.customer.customerId = :id)")
    Double totalPenaltyPaid(Long id);
}
