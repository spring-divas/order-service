package org.spring.divas.order.feature.dish;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DishResponseMapper {

    @Mapping(target = "venueId", ignore = true)
    DishResponseDto toDto(Dish dish);
}
