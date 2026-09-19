package org.spring.divas.order.feature.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponseDto create(OrderRequestDto dto) {

        Order order = orderMapper.toEntity(dto);

        Order saved = orderRepository.save(order);

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAll() {

        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getById(Long id) {

        Order found = orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(id)
                );

        return orderMapper.toResponse(found);
    }

    @Override
    public void delete(Long id) {

        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }

        orderRepository.deleteById(id);
    }

    @Override
    public OrderResponseDto update(Long id, OrderRequestDto dto) {

        Order found = orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(id)
                );

        found.setUserId(dto.getUserId());
        found.setTableId(dto.getTableId());

        //TODO: name + price

        Order saved = orderRepository.save(found);
        return orderMapper.toResponse(saved);
    }
}