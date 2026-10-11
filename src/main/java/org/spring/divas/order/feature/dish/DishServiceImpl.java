package org.spring.divas.order.feature.dish;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class DishServiceImpl implements DishService {
    private final DishRepository dishRepository;
    private final DishResponseMapper dishResponseMapper;
    private final DishRequestMapper dishRequestMapper;

    @Override
    public DishResponseDto getById(Long id) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new DishNotFoundException(id));
        return dishResponseMapper.toDto(dish);
    }

    @Override
    public List<DishResponseDto> getAllByIds(List<Long> ids) {
        return dishRepository.findAllById(ids)
                .stream()
                .map(dishResponseMapper::toDto)
                .toList();
    }

    @Override
    public DishResponseDto create(DishRequestDto dto) {
        Dish dish = dishRequestMapper.toDish(dto);
        Dish saved = dishRepository.save(dish);
        return dishResponseMapper.toDto(saved);
    }

    @Override
    public DishResponseDto update(DishRequestDto dto) {
        Dish dish = dishRepository.findById(dto.getDishId())
                .orElseThrow(() -> new DishNotFoundException(dto.getDishId()));
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        Dish saved = dishRepository.save(dish);
        return dishResponseMapper.toDto(saved);
    }
}
