package org.spring.divas.order.feature.orderitem;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;

    @Override
    public OrderItemResponseDto create(OrderItemRequestDto dto) {

        OrderItem orderItem = orderItemMapper.toEntity(dto);

        // Temporary stub until integration with venue-service.
        orderItem.setName("Temporary dish");
        orderItem.setPrice(BigDecimal.ZERO);

        OrderItem saved = orderItemRepository.save(orderItem);

        return orderItemMapper.toResponse(saved);
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