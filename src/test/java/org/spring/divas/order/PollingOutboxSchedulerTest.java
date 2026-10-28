package org.spring.divas.order;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.order.feature.outbox.OutboxEvent;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.spring.divas.order.feature.outbox.OutboxStatusService;
import org.spring.divas.order.feature.outbox.PollingOutboxScheduler;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingOutboxSchedulerTest {

    private static final String TOPIC = "order-created";
    private static final String TRACEPARENT =
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
    private static final String TRACESTATE = "vendor=value";
    private static final String PAYLOAD = "{\"orderId\":42}";

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxStatusService outboxStatusService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private Tracer tracer;

    @Mock
    private Propagator propagator;

    @Mock
    private Span.Builder spanBuilder;

    @Mock
    private Span publishSpan;

    @Mock
    private Tracer.SpanInScope spanInScope;

    private PollingOutboxScheduler scheduler;
    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        scheduler = new PollingOutboxScheduler(outboxRepository, outboxStatusService, kafkaTemplate, tracer, propagator);

        event = OutboxEvent.builder()
                .id(1L)
                .eventId(java.util.UUID.randomUUID())
                .aggregateType("Order")
                .aggregateId("42")
                .eventType("OrderCreated")
                .payload(PAYLOAD)
                .traceparent(TRACEPARENT)
                .tracestate(TRACESTATE)
                .build();
    }

    private void mockTracing() {
        when(propagator.extract(anyMap(), any()))
                .thenReturn(spanBuilder);

        when(spanBuilder.name("outbox.publish"))
                .thenReturn(spanBuilder);

        when(spanBuilder.start())
                .thenReturn(publishSpan);

        when(tracer.withSpan(publishSpan))
                .thenReturn(spanInScope);
    }

    @Test
    void shouldRestoreTraceContextAndPublishEvent() throws Exception {
        mockTracing();

        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(TOPIC, "42", PAYLOAD))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingEvents();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(propagator).extract(headersCaptor.capture(), any());

        assertEquals(
                TRACEPARENT,
                headersCaptor.getValue().get("traceparent")
        );
        assertEquals(
                TRACESTATE,
                headersCaptor.getValue().get("tracestate")
        );

        InOrder inOrder = inOrder(tracer, kafkaTemplate);

        inOrder.verify(tracer).withSpan(publishSpan);
        inOrder.verify(kafkaTemplate).send(TOPIC, "42", PAYLOAD);

        verify(publishSpan).end();
        verify(outboxStatusService).markPublished(1L);
        verify(outboxStatusService, never())
                .recordFailure(eq(1L), any(Exception.class));
    }

    @Test
    void shouldPublishEventWhenTraceContextIsMissing() throws Exception {
        mockTracing();

        event.setTraceparent(null);
        event.setTracestate(null);

        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(TOPIC, "42", PAYLOAD))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.publishPendingEvents();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(propagator).extract(headersCaptor.capture(), any());

        assertEquals(Map.of(), headersCaptor.getValue());

        verify(kafkaTemplate).send(TOPIC, "42", PAYLOAD);
        verify(outboxStatusService).markPublished(1L);
        verify(publishSpan).end();
    }

    @Test
    void shouldRecordFailureWhenKafkaSendFails() {
        mockTracing();

        when(outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(TOPIC, "42", PAYLOAD))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable")
                ));

        scheduler.publishPendingEvents();

        verify(outboxStatusService).recordFailure(
                eq(1L),
                any(Exception.class)
        );

        verify(outboxStatusService, never()).markPublished(1L);
        verify(publishSpan).end();
    }

    @Test
    void shouldDoNothingWhenThereAreNoPendingEvents() {
        when(outboxRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(List.of());

        scheduler.publishPendingEvents();

        verifyNoInteractions(kafkaTemplate, outboxStatusService, propagator, tracer);
    }
}
