package org.spring.divas.order.feature.order;

import lombok.AllArgsConstructor;
import org.spring.divas.order.feature.orderitem.OrderItem;
import org.spring.divas.order.feature.orderitem.OrderItemMapper;
import org.spring.divas.order.feature.venue.DishResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
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
                order.getVenueId(),
                order.getTableId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems()
                        .stream()
                        .map(orderItemMapper::toResponse)
                        .collect(Collectors.toList())
        );
    }

    public Order toEntity(OrderRequestDto request, List<DishResponseDto> dishes) {
        if (request == null) {
            return null;
        }

        Order order = Order.builder()
                .userId(request.getUserId())
                .venueId(request.getVenueId())
                .tableId(request.getTableId())
                .status(OrderStatus.NEW)
                .build();

        List<OrderItem> items = orderItemMapper.toEntities(request.getItems(), dishes);
        items.forEach(item -> item.setOrder(order));
        order.setItems(items);

        return order;
    }
}