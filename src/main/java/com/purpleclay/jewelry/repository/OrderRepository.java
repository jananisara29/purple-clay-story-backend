package com.purpleclay.jewelry.repository;

import com.purpleclay.jewelry.model.entity.Order;
import com.purpleclay.jewelry.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    // Categories the user has already purchased from
    @Query("""
        SELECT DISTINCT oi.product.category.name
        FROM OrderItem oi
        WHERE oi.order.user.id = :userId
        AND oi.order.status NOT IN ('CANCELLED', 'REFUNDED')
        """)
    List<String> findPurchasedCategoriesByUserId(@Param("userId") Long userId);

    // Product IDs user has purchased
    @Query("""
        SELECT oi.product.id
        FROM OrderItem oi
        WHERE oi.order.user.id = :userId
        AND oi.order.status NOT IN ('CANCELLED', 'REFUNDED')
        """)
    List<Long> findPurchasedProductIdsByUserId(@Param("userId") Long userId);

    // Product names + categories for purchase history context
    @Query("""
        SELECT oi.product.name, oi.product.category.name
        FROM OrderItem oi
        WHERE oi.order.user.id = :userId
        AND oi.order.status NOT IN ('CANCELLED', 'REFUNDED')
        ORDER BY oi.order.createdAt DESC
        """)
    List<Object[]> findPurchaseHistorySummary(@Param("userId") Long userId);
}
