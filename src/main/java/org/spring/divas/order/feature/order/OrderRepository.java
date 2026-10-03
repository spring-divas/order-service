package org.spring.divas.order.feature.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
                SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
                FROM Order o
                JOIN o.items i
                WHERE o.userId = :userId
                  AND i.dishId = :dishId
                  AND o.status = :status
            """)
    boolean existsByUserIdAndDishId(
            @Param("userId") Long userId,
            @Param("dishId") Long dishId,
            @Param("status") OrderStatus status
    );
}