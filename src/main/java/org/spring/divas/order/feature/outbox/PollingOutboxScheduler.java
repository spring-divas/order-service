package org.spring.divas.order.feature.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.util.HashMap;
import java.util.Map;
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
    private final Tracer tracer;
    private final Propagator propagator;

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAscIdAsc();

        for (OutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            Map<String, String> traceHeaders = new HashMap<>();

            if (event.getTraceparent() != null) {
                traceHeaders.put("traceparent", event.getTraceparent());
            }

            if (event.getTracestate() != null) {
                traceHeaders.put("tracestate", event.getTracestate());
            }

            Span publishSpan = propagator.extract(traceHeaders, Map::get)
                    .name("outbox.publish")
                    .start();

            try (Tracer.SpanInScope ignored = tracer.withSpan(publishSpan)) {
                kafkaTemplate.send(
                        TOPIC,
                        event.getAggregateId(),
                        event.getPayload()
                ).get(10, TimeUnit.SECONDS);

                outboxStatusService.markPublished(event.getId());

                log.info(
                        "Published outbox event: eventId={}, orderId={}",
                        event.getEventId(),
                        event.getAggregateId()
                );
            } finally {
                publishSpan.end();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            log.warn(
                    "Interrupted while publishing outbox event id={}, eventId={}",
                    event.getId(),
                    event.getEventId(),
                    e
            );
        } catch (Exception e) {
            outboxStatusService.recordFailure(event.getId(), e);

            log.error(
                    "Failed to publish outbox event id={}, eventId={}",
                    event.getId(),
                    event.getEventId(),
                    e
            );
        }
    }
}