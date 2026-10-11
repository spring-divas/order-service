package org.spring.divas.order.feature.dish;

import org.springframework.stereotype.Component;

@Component
public class DishResponseMapper {
    public DishResponseDto toDto(Dish dish) {
        return DishResponseDto.builder()
                .id(dish.getId())
                .name(dish.getName())
                .price(dish.getPrice())
                .build();
    }
}
