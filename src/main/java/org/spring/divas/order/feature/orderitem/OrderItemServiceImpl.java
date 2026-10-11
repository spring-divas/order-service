package org.spring.divas.order.feature.orderitem;

import lombok.AllArgsConstructor;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;

    private final DishService dishService;

    @Override
    public OrderItemResponseDto create(OrderItemRequestDto dto) {
        DishResponseDto response = dishService.getById(dto.getDishId());
        OrderItem orderItem = orderItemMapper.toEntity(dto, response);
        OrderItem saved = orderItemRepository.save(orderItem);
        return orderItemMapper.toResponse(saved);
    }

    @Override
    public List<OrderItemResponseDto> createAll(List<OrderItemRequestDto> dto) {
        List<Long> ids = dto.stream()
                .map(OrderItemRequestDto::getDishId)
                .distinct()
                .toList();
        List<DishResponseDto> dishes = dishService.getAllByIds(ids);
        List<OrderItem> items = orderItemMapper.toEntities(dto, dishes);
        List<OrderItem> savedItems = orderItemRepository.saveAll(items);
        return savedItems.stream()
                .map(orderItemMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderItemResponseDto> getAll() {
        return orderItemRepository.findAll()
                .stream()
                .map(orderItemMapper::toResponse)
                .toList();
    }

    @Override
    public OrderItemResponseDto getById(Long id) {
        OrderItem found = orderItemRepository.findById(id)
                .orElseThrow(() ->
                        new OrderItemNotFoundException(id)
                );

        return orderItemMapper.toResponse(found);
    }

    @Override
    public void delete(Long id) {

        if (!orderItemRepository.existsById(id)) {
            throw new OrderItemNotFoundException(id);
        }

        orderItemRepository.deleteById(id);
    }
}