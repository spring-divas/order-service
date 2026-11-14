/*package org.spring.divas.order;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.order.*;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;
import org.spring.divas.order.feature.outbox.OutboxEvent;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.spring.divas.order.feature.user.ResilientUserClient;
import org.spring.divas.order.feature.user.UserResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "outbox.poll-delay-ms=3600000"
})
@Transactional
@DirtiesContext
class OrderOutboxTraceContextIntegrationTest {

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private Tracer tracer;

    @Autowired
    private io.micrometer.tracing.propagation.Propagator propagator;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private DishService dishService;

    @MockitoBean
    private ResilientUserClient userClient;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderMapper orderMapper;

    @MockitoBean
    private OrderEventMapper orderEventMapper;

    @MockitoBean
    private OrderMetricsService orderMetricsService;

    @Test
    void shouldSaveOriginalTraceContextWhenCreatingOrder() {
        // Arrange: залежності, які не стосуються Outbox tracing
        Long userId = 10L;
        Long dishId = 20L;

        OrderItemRequestDto item = new OrderItemRequestDto();
        item.setDishId(dishId);

        OrderRequestDto request = new OrderRequestDto();
        request.setUserId(userId);
        request.setVenueId(30L);
        request.setTableId(40L);
        request.setItems(List.of(item));

        DishResponseDto dish = org.mockito.Mockito.mock(DishResponseDto.class);
        UserResponseDto user = org.mockito.Mockito.mock(UserResponseDto.class);
        Order order = org.mockito.Mockito.mock(Order.class);

        when(dishService.getAllByIds(List.of(dishId)))
                .thenReturn(List.of(dish));

        when(userClient.findById(userId))
                .thenReturn(user);

        when(orderMapper.toEntity(eq(request), anyList(), eq(user)))
                .thenReturn(order);

        when(orderRepository.save(order))
                .thenReturn(order);

        when(orderMapper.toResponse(order))
                .thenReturn(org.mockito.Mockito.mock(OrderResponseDto.class));

        UUID eventId = UUID.randomUUID();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("Order")
                .aggregateId("test-order")
                .eventType("OrderCreated")
                .payload("{\"orderId\":\"test-order\"}")
                .build();

        when(orderEventMapper.toOutboxEvent(order))
                .thenReturn(outboxEvent);

        Span parentSpan = tracer.nextSpan()
                .name("test-http-request")
                .start();

        try {
            String expectedTraceId = parentSpan.context().traceId();

            // Act: викликаємо реальний метод сервісу під активним спаном
            try (Tracer.SpanInScope ignored = tracer.withSpan(parentSpan)) {
                orderService.create(request);
            }

            // Перевіряємо, що сервіс записав trace context у подію
            assertThat(outboxEvent.getTraceparent())
                    .as("Outbox event повинен містити traceparent")
                    .isNotBlank();

            assertThat(outboxEvent.getTraceparent().split("-"))
                    .as("traceparent повинен містити коректний traceId")
                    .hasSize(4);

            assertThat(outboxEvent.getTraceparent().split("-")[1])
                    .isEqualTo(expectedTraceId);

            // Перевіряємо, що подія справді збережена в БД
            outboxRepository.flush();
            Long savedId = outboxEvent.getId();

            assertThat(savedId)
                    .as("Outbox event повинен отримати ID після збереження")
                    .isNotNull();

            entityManager.clear();

            OutboxEvent loaded = outboxRepository.findById(savedId)
                    .orElseThrow(() ->
                            new AssertionError("Outbox event was not found in DB"));

            assertThat(loaded.getEventId()).isEqualTo(eventId);
            assertThat(loaded.getTraceparent())
                    .isEqualTo(outboxEvent.getTraceparent());

            assertThat(loaded.getTraceparent().split("-")[1])
                    .isEqualTo(expectedTraceId);

            System.out.printf(
                    "Verified order outbox trace context: eventId=%s, traceId=%s, traceparent=%s%n",
                    loaded.getEventId(),
                    expectedTraceId,
                    loaded.getTraceparent()
            );
        } finally {
            parentSpan.end();
        }
    }
}*/