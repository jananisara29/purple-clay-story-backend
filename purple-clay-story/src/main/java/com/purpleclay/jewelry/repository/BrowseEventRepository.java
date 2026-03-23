package com.purpleclay.jewelry.repository;

import com.purpleclay.jewelry.model.entity.BrowseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BrowseEventRepository extends JpaRepository<BrowseEvent, Long> {

    // Recent browse events for a user (last 30 days)
    @Query("""
        SELECT b FROM BrowseEvent b
        WHERE b.userId = :userId
        AND b.createdAt >= :since
        ORDER BY b.createdAt DESC
        """)
    List<BrowseEvent> findRecentByUserId(
        @Param("userId") Long userId,
        @Param("since") LocalDateTime since
    );

    // Most viewed categories by user
    @Query("""
        SELECT b.categoryName, COUNT(b) as cnt
        FROM BrowseEvent b
        WHERE b.userId = :userId
        GROUP BY b.categoryName
        ORDER BY cnt DESC
        """)
    List<Object[]> findTopCategoriesByUserId(@Param("userId") Long userId);

    // Most viewed products by user (avoid recommending already-seen too much)
    @Query("""
        SELECT b.productId FROM BrowseEvent b
        WHERE b.userId = :userId
        GROUP BY b.productId
        ORDER BY COUNT(b) DESC
        """)
    List<Long> findMostViewedProductIds(@Param("userId") Long userId);
}
