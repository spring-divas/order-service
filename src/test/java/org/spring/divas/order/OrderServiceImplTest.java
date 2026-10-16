package org.spring.divas.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.order.OrderMapper;
import org.spring.divas.order.feature.order.OrderMetricsService;
import org.spring.divas.order.feature.order.OrderRepository;
import org.spring.divas.order.feature.order.OrderEventMapper;
import org.spring.divas.order.feature.order.OrderServiceImpl;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderResponseDto;
import org.spring.divas.order.feature.order.OrderStatus;
import org.spring.divas.order.feature.order.OrderNotFoundException;

import org.spring.divas.order.feature.outbox.OutboxRepository;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private DishService dishService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OrderEventMapper orderEventMapper;

    @Mock
    private OrderMetricsService orderMetricsService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderResponseDto response;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .status(OrderStatus.NEW)
                .build();

        response = new OrderResponseDto();
    }

    @Test
    void cancel_shouldSetCancelledStatusAndIncrementCounter() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(order))
                .thenReturn(order);
        when(orderMapper.toResponse(order))
                .thenReturn(response);

        OrderResponseDto result = orderService.cancel(1L);

        assertSame(response, result);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        verify(orderRepository).save(order);
        verify(orderMetricsService).incrementCancelledOrders();
    }

    @Test
    void cancel_shouldNotIncrementCounterWhenAlreadyCancelled() {
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order))
                .thenReturn(response);

        OrderResponseDto result = orderService.cancel(1L);

        assertSame(response, result);

        verify(orderRepository, never()).save(any());
        verify(orderMetricsService, never()).incrementCancelledOrders();
    }

    @Test
    void cancel_shouldRejectReadyOrder() {
        order.setStatus(OrderStatus.READY);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.cancel(1L));

        verify(orderRepository, never()).save(any());
        verify(orderMetricsService, never()).incrementCancelledOrders();
    }

    @Test
    void cancel_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.cancel(999L));

        verify(orderRepository, never()).save(any());
        verify(orderMetricsService, never()).incrementCancelledOrders();
    }

    @Test
    void refreshNotReadyOrdersMetric_shouldUpdateGaugeValue() {
        when(orderRepository.countByStatusNotIn(List.of(OrderStatus.READY, OrderStatus.CANCELLED)))
                .thenReturn(7L);

        orderService.refreshNotReadyOrdersMetric();
        verify(orderMetricsService).setNotReadyOrders(7);
    }
}
