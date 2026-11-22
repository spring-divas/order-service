package org.spring.divas.order;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderEventMapper;
import org.spring.divas.order.feature.order.OrderMapper;
import org.spring.divas.order.feature.order.OrderMetricsService;
import org.spring.divas.order.feature.order.OrderNotFoundException;
import org.spring.divas.order.feature.order.OrderRepository;
import org.spring.divas.order.feature.order.OrderRequestDto;
import org.spring.divas.order.feature.order.OrderResponseDto;
import org.spring.divas.order.feature.order.OrderServiceImpl;
import org.spring.divas.order.feature.order.OrderStatus;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;
import org.spring.divas.order.feature.outbox.OutboxEvent;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.spring.divas.order.feature.payment.PaymentGrpcClient;
import org.spring.divas.order.feature.user.ResilientUserClient;
import org.spring.divas.order.feature.user.UserResponseDto;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private PaymentGrpcClient paymentGrpcClient;

    @Mock
    private ResilientUserClient userClient;

    @Mock
    private Tracer tracer;

    @Mock
    private Propagator propagator;

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

    @Test
    void create_shouldCreateOrderWithUserWhenUserIdIsProvided() {
        OrderItemRequestDto itemDto = new OrderItemRequestDto(5L, 2);
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(10L)
                .venueId(1L)
                .tableId(2L)
                .items(List.of(itemDto))
                .build();

        DishResponseDto dish = DishResponseDto.builder().id(5L).build();
        UserResponseDto userDto = UserResponseDto.builder().id(10L).firstName("John").build();
        OutboxEvent outboxEvent = OutboxEvent.builder().id(99L).build();

        when(dishService.getAllByIds(List.of(5L))).thenReturn(List.of(dish));
        when(userClient.findById(10L)).thenReturn(userDto);
        when(orderMapper.toEntity(request, List.of(dish), userDto)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderEventMapper.toOutboxEvent(order)).thenReturn(outboxEvent);
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponseDto result = orderService.create(request);

        assertNotNull(result);
        verify(userClient).findById(10L);
        verify(orderRepository).save(order);
        verify(outboxRepository).save(outboxEvent);
    }

    @Test
    void create_shouldCreateOrderWithoutUserWhenUserIdIsNull() {
        OrderItemRequestDto itemDto = new OrderItemRequestDto(5L, 2);
        OrderRequestDto request = OrderRequestDto.builder()
                .userId(null)
                .venueId(1L)
                .tableId(2L)
                .items(List.of(itemDto))
                .build();

        DishResponseDto dish = DishResponseDto.builder().id(5L).build();
        OutboxEvent outboxEvent = OutboxEvent.builder().id(99L).build();

        when(dishService.getAllByIds(List.of(5L))).thenReturn(List.of(dish));
        when(orderMapper.toEntity(request, List.of(dish), null)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderEventMapper.toOutboxEvent(order)).thenReturn(outboxEvent);
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponseDto result = orderService.create(request);

        assertNotNull(result);
        verify(userClient, never()).findById(any());
        verify(orderRepository).save(order);
        verify(outboxRepository).save(outboxEvent);
    }
}
