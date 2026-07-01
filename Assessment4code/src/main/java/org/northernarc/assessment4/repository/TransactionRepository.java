package org.northernarc.assessment4.repository;

import org.northernarc.assessment4.model.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Task 3: Derived Query Method
    List<Transaction> findByTransactionType(String transactionType);

    // Task 4: Latest transaction(s) ordered by date descending (use Pageable to limit to 1)
    @Query("SELECT t FROM Transaction t ORDER BY t.transactionDate DESC, t.transactionId DESC")
    List<Transaction> findLatestTransaction(Pageable pageable);
}
