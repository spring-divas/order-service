package org.spring.divas.order.feature.order;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.orderitem.OrderItemMapper;
import org.spring.divas.order.feature.user.UserResponseDto;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = OrderItemMapper.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public abstract class OrderMapper {

    @Mapping(target = "accessToken", ignore = true)
    public abstract OrderResponseDto toResponse(Order order);

    public Order toEntity(
            OrderRequestDto request, List<DishResponseDto> dishes, UserResponseDto user, Long userId
    ) {
        if (request == null) {
            return null;
        }
        return mapToEntity(request, user, userId, dishes);
    }

    public Order toEntity(OrderRequestDto request, List<DishResponseDto> dishes, UserResponseDto user) {
        if (request == null) {
            return null;
        }
        return toEntity(request, dishes, user, request.getUserId());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "accessTokenHash", ignore = true)
    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "venueId", source = "request.venueId")
    @Mapping(target = "tableId", source = "request.tableId")
    @Mapping(target = "items", source = "request.items")
    protected abstract Order mapToEntity(
            OrderRequestDto request, UserResponseDto user, Long userId, @Context List<DishResponseDto> dishes
    );

    @AfterMapping
    protected void linkItemsToOrder(@MappingTarget Order order) {
        if (order.getItems() != null) {
            order.getItems().forEach(item -> item.setOrder(order));
        }
    }
}
