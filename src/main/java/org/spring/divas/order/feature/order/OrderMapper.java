package org.spring.divas.order.feature.order;

import lombok.AllArgsConstructor;
import org.spring.divas.order.feature.orderItem.OrderItemMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderResponseDto toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return new OrderResponseDto(
                order.getId(),
                order.getUserId(),
                order.getTableId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems()
                        .stream()
                        .map(orderItemMapper::toResponse)
                        .collect(Collectors.toList())
        );
    }

    public Order toEntity(OrderRequestDto request) {
        if (request == null) {
            return null;
        }

        Order order = Order.builder()
                .userId(request.getUserId())
                .tableId(request.getTableId())
                .status(OrderStatus.NEW)
                .build();

        order.setItems(
                request.getItems()
                        .stream()
                        .map(orderItemMapper::toEntity)
                        .toList()
        );

        order.getItems().forEach(item -> item.setOrder(order));

        return order;
    }
}