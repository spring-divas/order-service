package org.spring.divas.order;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderAccessTokens;
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
import org.spring.divas.payment.v1.PaymentStreamResponse;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(orderMapper.toEntity(request, List.of(dish), userDto, 10L)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderEventMapper.toOutboxEvent(order)).thenReturn(outboxEvent);
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponseDto result = orderService.create(request);

        assertNotNull(result);
        assertNull(result.accessToken());
        assertNull(order.getAccessTokenHash());
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
        when(orderMapper.toEntity(request, List.of(dish), null, null)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderEventMapper.toOutboxEvent(order)).thenReturn(outboxEvent);
        when(orderMapper.toResponse(order)).thenReturn(response);

        OrderResponseDto result = orderService.create(request);

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertTrue(OrderAccessTokens.matches(result.accessToken(), order.getAccessTokenHash()));
        verify(userClient, never()).findById(any());
        verify(orderRepository).save(order);
        verify(outboxRepository).save(outboxEvent);
    }

    @Test
    void pay_shouldCallGrpcOutsideTransactionAndRereadOrder() {
        Order updatedOrder = Order.builder()
                .id(1L)
                .status(OrderStatus.PAID)
                .build();
        OrderResponseDto updatedResponse = new OrderResponseDto();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order))
                .thenReturn(Optional.of(updatedOrder));
        when(paymentGrpcClient.processRealtimePaymentWithRetry(order, false))
                .thenAnswer(invocation -> {
                    assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                            "gRPC payment must be executed outside of any transaction");
                    return CompletableFuture.completedFuture(null);
                });
        when(orderMapper.toResponse(updatedOrder))
                .thenReturn(updatedResponse);

        OrderResponseDto result = orderService.pay(1L, false);

        assertSame(updatedResponse, result);
        InOrder inOrder = inOrder(orderRepository, paymentGrpcClient);
        inOrder.verify(orderRepository).findById(1L);
        inOrder.verify(paymentGrpcClient).processRealtimePaymentWithRetry(order, false);
        inOrder.verify(orderRepository).findById(1L);
    }

    @Test
    void pay_whenGrpcThrowsException_shouldCatchAndStillRereadOrder() {
        Order updatedOrder = Order.builder()
                .id(1L)
                .status(OrderStatus.PAYMENT_FAILED)
                .build();
        OrderResponseDto failedResponse = new OrderResponseDto();

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order))
                .thenReturn(Optional.of(updatedOrder));
        CompletableFuture<PaymentStreamResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Payment service unavailable"));
        when(orderMapper.toResponse(updatedOrder))
                .thenReturn(failedResponse);
        when(paymentGrpcClient.processRealtimePaymentWithRetry(order, false))
                .thenReturn(failedFuture);

        OrderResponseDto result = orderService.pay(1L, false);

        assertSame(failedResponse, result);
        verify(orderRepository, times(2)).findById(1L);
    }

    @Test
    void pay_whenOrderNotFound_shouldThrowOrderNotFoundException() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.pay(999L, false));

        verify(paymentGrpcClient, never()).processRealtimePaymentWithRetry(any(), anyBoolean());
    }

    @Test
    void pay_whenUpdatedOrderNotFound_shouldThrowOrderNotFoundException() {
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order))
                .thenReturn(Optional.empty());
        when(paymentGrpcClient.processRealtimePaymentWithRetry(order, false))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThrows(OrderNotFoundException.class,
                () -> orderService.pay(1L, false));
    }

    @Test
    void shouldAcceptAccessTokenOfGuestOrder() {
        String token = OrderAccessTokens.generate();
        Order guestOrder = Order.builder().id(1L)
                .accessTokenHash(OrderAccessTokens.hash(token)).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(guestOrder));

        assertTrue(orderService.hasGuestAccess(1L, token));
        assertFalse(orderService.hasGuestAccess(1L, OrderAccessTokens.generate()));
    }

    @Test
    void shouldRejectAccessTokenForOrderOfRegisteredUser() {
        String token = OrderAccessTokens.generate();
        Order userOrder = Order.builder().id(1L).userId(10L)
                .accessTokenHash(OrderAccessTokens.hash(token)).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(userOrder));

        assertFalse(orderService.hasGuestAccess(1L, token));
    }
}
