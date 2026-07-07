package org.nbfc.loanemicalculator.repository;

import org.nbfc.loanemicalculator.entity.PenaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PenaltyTransactionRepository extends JpaRepository<PenaltyTransaction, Long> {

    List<PenaltyTransaction> findByPaymentMode(String paymentMode);

    Page<PenaltyTransaction> findByPaymentMode(String paymentMode, Pageable pageable);

    List<PenaltyTransaction> findByPenaltyAmountGreaterThan(Double amount);

    @Query("SELECT p FROM PenaltyTransaction p ORDER BY p.paymentDate DESC")
    List<PenaltyTransaction> findLatest(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.penaltyAmount), 0) FROM PenaltyTransaction p")
    Double getTotalPenaltyCollected();
}
