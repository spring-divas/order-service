package org.spring.divas.order.feature.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findById(Long id);

    @Query(
            """
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

    boolean existsByUserIdAndVenueId(Long userId, Long venueId);

    long countByStatusNotIn(List<OrderStatus> statuses);
}
