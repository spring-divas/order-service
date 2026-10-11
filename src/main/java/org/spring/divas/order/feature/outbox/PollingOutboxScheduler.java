package org.spring.divas.order.feature.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollingOutboxScheduler {

    private static final String TOPIC = "order-created";

    private final OutboxRepository outboxRepository;
    private final OutboxStatusService outboxStatusService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAscIdAsc();

        for (OutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
            outboxStatusService.markPublished(event.getId());
            log.info("Published outbox event: eventId={}, orderId={}", event.getEventId(), event.getAggregateId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing outbox event id={}, eventId={}", event.getId(), event.getEventId(), e);
        } catch (Exception e) {
            outboxStatusService.recordFailure(event.getId(), e);
            log.error("Failed to publish outbox event id={}, eventId={}", event.getId(), event.getEventId(), e);
        }
    }
}