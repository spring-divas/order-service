package org.spring.divas.order.feature.orderitem;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.spring.divas.order.feature.venue.DishResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
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

    public OrderItem toEntity(
            OrderItemRequestDto request,
            DishResponseDto dish
    ) {
        if (request == null) {
            return null;
        }

        return OrderItem.builder()
                .dishId(request.getDishId())
                .quantity(request.getQuantity())
                .name(dish.name())
                .price(dish.price())
                .build();
    }

    public List<OrderItem> toEntities(
            List<OrderItemRequestDto> dto,
            List<DishResponseDto> dishes
    ) {
        Map<Long, DishResponseDto> dishesMap = dishes.stream()
                .collect(Collectors.toMap(
                        DishResponseDto::id,
                        Function.identity(),
                        (existing, _) -> existing));
        return dto.stream().map(itemDto -> {
            DishResponseDto dish = Optional.ofNullable(dishesMap.get(itemDto.getDishId()))
                    .orElseThrow(() -> new EntityNotFoundException("Dish not found."));
            return toEntity(itemDto, dish);
        }).toList();
    }

}