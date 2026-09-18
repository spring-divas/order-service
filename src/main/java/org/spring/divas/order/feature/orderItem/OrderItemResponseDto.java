package org.spring.divas.order.feature.orderItem;

import java.math.BigDecimal;

public record OrderItemResponseDto(
        Long id,
        Long dishId,
        String name,
        Integer quantity,
        BigDecimal price
) {
}