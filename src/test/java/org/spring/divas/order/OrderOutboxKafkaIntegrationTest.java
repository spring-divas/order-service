package org.spring.divas.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.order.*;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;
import org.spring.divas.order.feature.outbox.OutboxEvent;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.spring.divas.order.feature.outbox.OutboxStatusService;
import org.spring.divas.order.feature.outbox.PollingOutboxScheduler;
import org.spring.divas.order.feature.user.ResilientUserClient;
import org.spring.divas.order.feature.user.UserResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest()
class OrderOutboxKafkaIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxStatusService outboxStatusService;

    @Autowired
    private PollingOutboxScheduler scheduler;

    @Autowired
    private OrderEventMapper orderEventMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DishService dishService;

    @MockitoBean
    private OrderMapper orderMapper;

    @MockitoBean
    private ResilientUserClient userClient;

    @MockitoSpyBean
    private OutboxRepository outboxRepositorySpy;


    @BeforeEach
    void cleanOutbox() {
        outboxRepository.deleteAll();
        outboxRepository.flush();
    }

    @Test
    void shouldRollbackOrderWhenOutboxSaveFails() {
        long ordersBefore = orderRepository.count();
        long outboxBefore = outboxRepository.count();

        OrderRequestDto request = validRequest();
        when(dishService.getAllByIds(any()))
                .thenReturn(List.of());

        when(userClient.findById(any()))
                .thenReturn(UserResponseDto.builder()
                        .firstName("First Name")
                        .lastName("Last Name")
                        .email("Email")
                        .build());

        when(orderMapper.toEntity(eq(request), any(), any()))
                .thenReturn(Order.builder()
                        .userId(10L)
                        .firstName("First name")
                        .lastName("Last name")
                        .email("Email")
                        .venueId(20L)
                        .tableId(30L)
                        .status(OrderStatus.NEW)
                        .items(new ArrayList<>())
                        .build());

        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(new OrderResponseDto());

        doThrow(new RuntimeException("Outbox save failed"))
                .when(outboxRepositorySpy)
                .save(any(OutboxEvent.class));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Outbox save failed");

        assertThat(orderRepository.count())
                .isEqualTo(ordersBefore);

        assertThat(outboxRepository.count())
                .isEqualTo(outboxBefore);
    }


    @Test
    void shouldSelectOnlyUnpublishedEventsInCreationOrder() {
        OutboxEvent pendingFirst = outboxRepository.saveAndFlush(
                createOutboxEvent(UUID.randomUUID(), 911L)
        );

        OutboxEvent pendingSecond = outboxRepository.saveAndFlush(
                createOutboxEvent(UUID.randomUUID(), 912L)
        );

        OutboxEvent published = outboxRepository.saveAndFlush(
                createOutboxEvent(UUID.randomUUID(), 913L)
        );

        outboxStatusService.markPublished(published.getId());

        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAscIdAsc();

        assertThat(pending)
                .extracting(OutboxEvent::getId)
                .contains(pendingFirst.getId(), pendingSecond.getId())
                .doesNotContain(published.getId());

        assertThat(pending)
                .extracting(OutboxEvent::getId)
                .containsSubsequence(
                        pendingFirst.getId(),
                        pendingSecond.getId()
                );
    }

    @Test
    void shouldNotRepublishAnAlreadyPublishedEvent() {
        OutboxEvent event = outboxRepository.saveAndFlush(
                createOutboxEvent(UUID.randomUUID(), 914L)
        );
        outboxStatusService.markPublished(event.getId());

        scheduler.publishPendingEvents();
        OutboxEvent persisted = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(persisted.getPublishedAt()).isNotNull();
    }


    @Test
    void shouldCreateOutboxEventWithValidOrderCreatedJson(){
        long orderId = 1234L;

        Order order = Order.builder()
                .id(orderId)
                .userId(10L)
                .venueId(20L)
                .tableId(30L)
                .status(OrderStatus.NEW)
                .items(new ArrayList<>())
                .build();

        Instant beforeMapping = Instant.now();
        OutboxEvent outboxEvent = orderEventMapper.toOutboxEvent(order);
        Instant afterMapping = Instant.now();

        assertThat(outboxEvent.getEventId()).isNotNull();
        assertThat(outboxEvent.getAggregateType()).isEqualTo("Order");
        assertThat(outboxEvent.getAggregateId()).isEqualTo("1234");
        assertThat(outboxEvent.getEventType()).isEqualTo("OrderCreated");
        assertThat(outboxEvent.getPayload()).isNotBlank();

        JsonNode json = objectMapper.readTree(outboxEvent.getPayload());

        assertThat(json.get("eventId").stringValue())
                .isEqualTo(outboxEvent.getEventId().toString());

        assertThat(json.get("orderId").longValue())
                .isEqualTo(orderId);

        Instant occurredAt = Instant.parse(json.get("occurredAt").stringValue());

        assertThat(occurredAt)
                .isBetween(beforeMapping, afterMapping);
    }

    private OrderRequestDto validRequest() {
        OrderItemRequestDto item = mock(OrderItemRequestDto.class);
        when(item.getDishId()).thenReturn(1L);

        OrderRequestDto request = new OrderRequestDto();
        request.setUserId(10L);
        request.setVenueId(20L);
        request.setTableId(30L);
        request.setItems(List.of(item));

        return request;
    }

    private OutboxEvent createOutboxEvent(UUID eventId, Long orderId) {
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": %d,
                  "occurredAt": "%s"
                }
                """.formatted(eventId, orderId, Instant.now());

        return OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("Order")
                .aggregateId(orderId.toString())
                .eventType("OrderCreated")
                .payload(payload)
                .attempts(0)
                .build();
    }
}
