package org.spring.divas.order.feature.order;

import org.spring.divas.order.feature.outbox.OutboxEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventMapper {

    private final ObjectMapper objectMapper;

    public OrderEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEvent toOutboxEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), order.getId(), Instant.now());

        try {
            String payload = objectMapper.writeValueAsString(event);

            return OutboxEvent.builder()
                    .eventId(event.eventId())
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("OrderCreated")
                    .payload(payload)
                    .build();

        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialize OrderCreatedEvent for order " + order.getId(), e);
        }
    }
}