package org.spring.divas.order.feature.order;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Long orderId,
        Instant occurredAt
) { }