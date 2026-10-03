package org.spring.divas.order.feature.order;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;
import org.spring.divas.order.feature.payment.ResilientPaymentClient;
import org.spring.divas.order.feature.venue.DishResponseDto;
import org.spring.divas.order.feature.venue.ResilientVenueClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    private final ResilientPaymentClient paymentClient;
    private final ResilientVenueClient venueClient;

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);


    @Override
    public OrderResponseDto create(OrderRequestDto dto) {
        log.info("Creating order: {}", dto);
        List<Long> ids = dto.getItems().stream()
                .map(OrderItemRequestDto::getDishId)
                .toList();
        List<DishResponseDto> dishes = venueClient.getDishesByIds(ids);
        Order order = orderMapper.toEntity(dto, dishes);
        Order saved = orderRepository.save(order);
        log.info("Order saved with id={}", saved.getId());
        String idempotencyKey = "order-" + saved.getId();
        paymentClient.createPayment(saved.getId(), idempotencyKey);
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

        Order found = orderRepository.findById(id).orElseThrow(() ->
                new OrderNotFoundException(id));

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
        Order found = orderRepository.findById(id).orElseThrow(() ->
                new OrderNotFoundException(id));
        found.setTableId(dto.getTableId());
        if (dto.getStatus() != null) {
            found.setStatus(dto.getStatus());
        }
        Order saved = orderRepository.save(found);
        return orderMapper.toResponse(saved);
    }

    @Override
    public boolean hasUserOrderedDish(Long userId, Long dishId) {
        return orderRepository.existsByUserIdAndDishId(userId, dishId, OrderStatus.READY);
    }
}