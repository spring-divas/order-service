package org.spring.divas.order.feature.orderitem;

import lombok.AllArgsConstructor;
import org.spring.divas.order.feature.venue.DishResponseDto;
import org.spring.divas.order.feature.venue.VenueClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper orderItemMapper;

    private final VenueClient venueClient;

    @Override
    public OrderItemResponseDto create(OrderItemRequestDto dto) {

        DishResponseDto response = venueClient.getDishById(dto.getDishId());

        OrderItem orderItem = orderItemMapper.toEntity(dto);
        orderItem.setName(response.name());
        orderItem.setPrice(response.price());

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