package org.spring.divas.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.spring.divas.order.feature.outbox.OutboxEvent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OutboxTraceContextIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void shouldDisplayTraceContextForLatestOutboxEvents() {
        List<OutboxEvent> latestEvents = outboxRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(10)
                .toList();

        assertThat(latestEvents)
                .as("Останні записи Outbox")
                .isNotNull();

        latestEvents.forEach(event -> {
            System.out.printf(
                    "id=%s, eventId=%s, aggregateId=%s, traceparent=%s, tracestate=%s%n",
                    event.getId(),
                    event.getEventId(),
                    event.getAggregateId(),
                    event.getTraceparent(),
                    event.getTracestate()
            );
        });
    }
}