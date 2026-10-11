package org.spring.divas.order.feature.dish;

import org.springframework.stereotype.Component;

@Component
public class DishEventMapper {

    public DishRequestDto toRequestDto(DishCreatedEvent event) {
        return DishRequestDto.builder()
                .dishId(event.dishId())
                .name(event.name())
                .price(event.price())
                .build();
    }

    public DishRequestDto toRequestDto(DishUpdatedEvent event) {
        return DishRequestDto.builder()
                .dishId(event.dishId())
                .name(event.name())
                .price(event.price())
                .build();
    }
}
