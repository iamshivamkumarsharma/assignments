package org.nbfc.loanemicalculator.repository;

import org.nbfc.loanemicalculator.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByLoanType(String loanType);

    List<Loan> findByInterestRateGreaterThan(Double rate);

    // --- Additional custom / derived query methods ---
    List<Loan> findByLoanStatus(String loanStatus);

    List<Loan> findByLoanTypeAndLoanStatus(String loanType, String loanStatus);

    Page<Loan> findByLoanType(String loanType, Pageable pageable);

    /**
     * Solves the N+1 problem: eagerly fetches every loan's EMI schedule in a single
     * SQL statement using a JOIN FETCH instead of one query per loan.
     */
    @Query("SELECT DISTINCT l FROM Loan l LEFT JOIN FETCH l.emiSchedules")
    List<Loan> findAllWithSchedules();

    /**
     * Same N+1 fix scoped to one loan, expressed with an entity graph.
     */
    @EntityGraph(attributePaths = "emiSchedules")
    Optional<Loan> findWithSchedulesByLoanId(Long loanId);

    @Query("SELECT l FROM Loan l WHERE l NOT IN " +
            "(SELECT e.loan FROM EmiSchedule e WHERE e.status = 'OVERDUE')")
    List<Loan> findLoansWithoutOverdue();

    @Modifying
    @Transactional
    @Query("UPDATE Loan l SET l.interestRate = l.interestRate + :amount WHERE l.loanType = :loanType")
    int increaseInterestRate(@Param("loanType") String loanType, @Param("amount") double amount);
}
