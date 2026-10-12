package org.spring.divas.order;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.dish.DishCreatedEvent;
import org.spring.divas.order.feature.dish.DishRequestDto;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.processed.ProcessedEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DishEventListenerTest extends KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @MockitoSpyBean
    private DishService dishService;

    @AfterEach
    void tearDown() {
        processedEventRepository.deleteAll();
    }

    @Test
    void consumesEventAndSavesProcessedMarker() {
        var eventId = UUID.randomUUID();
        send(eventId, event(eventId, 101L));
        await().atMost(Duration.ofSeconds(5))
                .until(() -> processedEventRepository.existsByEventId(eventId));
        verify(dishService, times(1)).create(any(DishRequestDto.class));
    }

    @Test
    void ignoresDuplicateEvent() {
        var eventId = UUID.randomUUID();
        var event = event(eventId, 102L);
        send(eventId, event);
        await().atMost(Duration.ofSeconds(5))
                .until(() -> processedEventRepository.existsByEventId(eventId));
        send(eventId, event);
        await().pollDelay(Duration.ofMillis(300)).atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            verify(dishService, times(1)).create(any(DishRequestDto.class));
            assertThat(processedEventRepository.count()).isEqualTo(1);
        });
    }

    @Test
    void routesToDltOnFailure() {
        var eventId = UUID.randomUUID();
        doThrow(new RuntimeException())
                .when(dishService).create(any(DishRequestDto.class));
        try (var dltConsumer = consumer("dish-events-dlt")) {
            send(eventId, event(eventId, 103L));
            var record = awaitRecord(dltConsumer, eventId.toString());
            assertThat(record.value()).contains(eventId.toString());
            assertThat(processedEventRepository.existsByEventId(eventId)).isFalse();
        }
    }

    private void send(UUID eventId, Object payload) {
        try {
            var record = new ProducerRecord<>(
                    "dish-events", eventId.toString(), objectMapper.writeValueAsString(payload));
            record.headers().add("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8));
            record.headers().add("eventType", "DishCreated".getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(record).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DishCreatedEvent event(UUID eventId, Long dishId) {
        return new DishCreatedEvent(eventId, dishId, 1L, "Dish", BigDecimal.valueOf(150));
    }
}