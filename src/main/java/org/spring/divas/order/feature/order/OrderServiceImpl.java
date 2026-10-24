package org.spring.divas.order.feature.order;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.user.ResilientUserClient;
import org.spring.divas.order.feature.user.UserResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final DishService dishService;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    private final OutboxRepository outboxRepository;
    private final OrderEventMapper orderEventMapper;

    private final OrderMetricsService orderMetricsService;

    private final ResilientUserClient userClient;

    @Override
    @Transactional
    public OrderResponseDto create(OrderRequestDto dto) {
        log.info("Creating order: {}", dto);

        List<Long> ids = dto.getItems().stream()
                .map(OrderItemRequestDto::getDishId)
                .toList();
        List<DishResponseDto> dishes = dishService.getAllByIds(ids);
        UserResponseDto user = userClient.findById(dto.getUserId());

        Order order = orderMapper.toEntity(dto, dishes, user);
        Order saved = orderRepository.save(order);

        log.info("Order saved with id={}", saved.getId());

        outboxRepository.save(orderEventMapper.toOutboxEvent(saved));
        recordTotalPrice(saved);
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

    @Override
    public boolean hasUserBeenToVenue(Long userId, Long venueId) {
        return orderRepository.existsByUserIdAndVenueId(userId, venueId);
    }

    @Override
    @Transactional
    public OrderResponseDto cancel(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return orderMapper.toResponse(order);
        }

        if (order.getStatus() == OrderStatus.READY) {
            throw new IllegalStateException(
                    "You cannot cancel a READY order"
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        orderMetricsService.incrementCancelledOrders();

        return orderMapper.toResponse(saved);
    }


    @Scheduled(fixedDelay = 10000)
    @Transactional(readOnly = true)
    public void refreshNotReadyOrdersMetric() {
        long count = orderRepository.countByStatusNotIn(
                List.of(OrderStatus.READY, OrderStatus.CANCELLED)
        );

        orderMetricsService.setNotReadyOrders(Math.toIntExact(count));
    }

    private void recordTotalPrice(Order order) {
        try {
            BigDecimal sum = order.getItems().stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        orderMetricsService.recordOrderPrice(sum);
                    } catch (Exception e) {
                        log.warn("Failed to record order price metric", e);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Failed to record order price metric", e);
        }
    }
}