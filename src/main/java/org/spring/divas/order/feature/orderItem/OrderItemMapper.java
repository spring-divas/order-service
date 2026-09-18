package org.spring.divas.order.feature.orderItem;

import org.springframework.stereotype.Component;

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
                .build();
    }
}