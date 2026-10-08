package org.spring.divas.order.feature.dish;

import org.springframework.stereotype.Component;

@Component
public class DishRequestMapper {
    public Dish toDish(DishRequestDto dto) {
        return Dish.builder()
                .id(dto.getDishId())
                .name(dto.getName())
                .price(dto.getPrice())
                .build();
    }
}
