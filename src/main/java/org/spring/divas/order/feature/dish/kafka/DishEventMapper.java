package org.spring.divas.order.feature.dish.kafka;

import org.mapstruct.Mapper;
import org.spring.divas.order.feature.dish.DishRequestDto;

@Mapper(componentModel = "spring")
public interface DishEventMapper {

    DishRequestDto toRequestDto(DishCreatedEvent event);

    DishRequestDto toRequestDto(DishUpdatedEvent event);
}
