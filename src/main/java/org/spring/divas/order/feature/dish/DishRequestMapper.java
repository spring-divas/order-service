package org.spring.divas.order.feature.dish;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DishRequestMapper {

    @Mapping(target = "id", source = "dishId")
    Dish toDish(DishRequestDto dto);
}
