package org.spring.divas.order.feature.order;

import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.outbox.OutboxEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderEventMapperTest {

    @Test
    void shouldMapOrderToOutboxEvent() {
        ObjectMapper objectMapper = new JsonMapper();
        OrderEventMapper mapper = new OrderEventMapper(objectMapper);

        Order order = Order.builder()
                .id(42L)
                .build();

        OutboxEvent outboxEvent = mapper.toOutboxEvent(order);

        assertNotNull(outboxEvent);
        assertNotNull(outboxEvent.getEventId());
        assertEquals("Order", outboxEvent.getAggregateType());
        assertEquals("42", outboxEvent.getAggregateId());
        assertEquals("OrderCreated", outboxEvent.getEventType());
        assertTrue(outboxEvent.getPayload().contains("\"orderId\":42"));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenSerializationFails() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(mock(JacksonException.class));

        OrderEventMapper mapper = new OrderEventMapper(mockMapper);
        Order order = Order.builder()
                .id(99L)
                .build();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> mapper.toOutboxEvent(order)
        );
        assertTrue(ex.getMessage().contains("Could not serialize OrderCreatedEvent"));
    }

    @Test
    void shouldTestOrderCreatedEventRecord() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        OrderCreatedEvent event = new OrderCreatedEvent(eventId, 15L, now);

        assertEquals(eventId, event.eventId());
        assertEquals(15L, event.orderId());
        assertEquals(now, event.occurredAt());
    }
}
