package org.spring.divas.order.feature.dish;

import java.util.List;

public interface DishService {
    DishResponseDto getById(Long id);
    List<DishResponseDto> getAllByIds(List<Long> ids);
    DishResponseDto create(DishRequestDto dto);
    DishResponseDto update(DishRequestDto dto);
}
