package org.spring.divas.order.feature.orderItem;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderItemMapper {

    public OrderItemResponseDto toResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return new OrderItemResponseDto(
                orderItem.getId(),
                orderItem.getDishId(),
                orderItem.getName(),
                orderItem.getQuantity(),
                orderItem.getPrice()
        );
    }

    public OrderItem toEntity(OrderItemRequestDto request) {
        if (request == null) {
            return null;
        }

        return OrderItem.builder()
                .dishId(request.getDishId())
                .quantity(request.getQuantity())
                .name("Temporary dish")
                .price(BigDecimal.ZERO)
                .build();
    }
}