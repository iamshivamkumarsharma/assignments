package org.nbfc.assignment3.repository;

import org.nbfc.assignment3.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository backed by PostgreSQL.
 * JpaRepository already gives us save, findById, findAll,
 * existsById, deleteById, deleteAll and count.
 */
@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {



}
