package com.purpleclay.jewelry.repository;

import com.purpleclay.jewelry.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:customizable IS NULL OR p.customizable = :customizable)
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Product> searchProducts(
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("customizable") Boolean customizable,
        @Param("search") String search,
        Pageable pageable
    );

    // For recommendation engine (Module 7)
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND p.id != :excludeId ORDER BY p.createdAt DESC")
    List<Product> findRelatedProducts(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId, Pageable pageable);

    List<Product> findTop10ByActiveTrueOrderByCreatedAtDesc();

    boolean existsByNameAndCategoryId(String name, Long categoryId);
}
