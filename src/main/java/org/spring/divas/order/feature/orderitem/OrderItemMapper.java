package org.spring.divas.order.feature.orderitem;

import jakarta.persistence.EntityNotFoundException;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.spring.divas.order.feature.dish.DishResponseDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class OrderItemMapper {

    public abstract OrderItemResponseDto toResponse(OrderItem orderItem);

    public OrderItem toEntity(OrderItemRequestDto request, DishResponseDto dish) {
        if (request == null) {
            return null;
        }
        return mapToEntity(request, dish);
    }

    public List<OrderItem> toEntities(
            List<OrderItemRequestDto> dto,
            @Context List<DishResponseDto> dishes
    ) {
        Map<Long, DishResponseDto> dishesMap = dishes.stream()
                .collect(Collectors.toMap(
                        DishResponseDto::getId,
                        Function.identity(),
                        (existing, _) -> existing));
        return dto.stream().map(itemDto -> {
            DishResponseDto dish = Optional.ofNullable(dishesMap.get(itemDto.getDishId()))
                    .orElseThrow(() -> new EntityNotFoundException("Dish not found."));
            return toEntity(itemDto, dish);
        }).toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "dishId", source = "request.dishId")
    @Mapping(target = "quantity", source = "request.quantity")
    @Mapping(target = "name", source = "dish.name")
    @Mapping(target = "price", source = "dish.price")
    protected abstract OrderItem mapToEntity(OrderItemRequestDto request, DishResponseDto dish);
}
