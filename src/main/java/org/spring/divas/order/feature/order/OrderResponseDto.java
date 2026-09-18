package org.spring.divas.order.feature.order;

import org.spring.divas.order.feature.orderItem.OrderItemResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
        Long id,
        Long userId,
        Long tableId,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponseDto> items
) {
}