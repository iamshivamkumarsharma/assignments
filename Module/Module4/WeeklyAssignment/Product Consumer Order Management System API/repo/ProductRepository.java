package org.nbfc.productwa.repository;

import org.nbfc.productwa.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.deleted = false")
    Page<Product> findAllActive(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deleted = false")
    java.util.Optional<Product> findActiveById(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND LOWER(p.category) = LOWER(:category)")
    Page<Product> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.price BETWEEN :min AND :max")
    Page<Product> findByPriceRange(@Param("min") BigDecimal min,
                                   @Param("max") BigDecimal max,
                                   Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.deleted = false
              AND (:keyword IS NULL
                   OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.deleted = false
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> filter(@Param("category") String category,
                         @Param("brand") String brand,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false ORDER BY p.price ASC")
    List<Product> findAllSortedByPriceAsc();
}
