package org.nbfc.loanemicalculator.repository;

import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {

    List<EmiSchedule> findByStatus(String status);

    Page<EmiSchedule> findByStatus(String status, Pageable pageable);

    List<EmiSchedule> findByCustomer_CustomerId(Long customerId);

    /**
     * Avoids N+1 selects by fetching the associated customer and loan up front.
     */
    @EntityGraph(attributePaths = {"customer", "loan"})
    List<EmiSchedule> findWithCustomerAndLoanByStatus(String status);

    @Query("SELECT COUNT(e) FROM EmiSchedule e WHERE e.status = 'OVERDUE'")
    Long countOverdue();
}
