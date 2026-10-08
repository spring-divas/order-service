package org.spring.divas.order.feature.dish;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.divas.order.feature.processed.ProcessedEvent;
import org.spring.divas.order.feature.processed.ProcessedEventRepository;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@AllArgsConstructor
public class DishEventListener {
    private final DishEventMapper dishEventMapper;
    private final DishService dishService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(DishEventListener.class);

    @RetryableTopic(
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @Transactional
    @KafkaListener(topics = "dish-events", groupId = "dish-events-group")
    public void handleDishEvent(
            @Payload String payload,
            @Header("eventId") UUID eventId,
            @Header("eventType") String eventType
    ) {
        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate event detected, skipping: {}", eventId);
            return;
        }
        switch (eventType) {
            case "DishCreated" -> handleDishCreated(payload);
            case "DishUpdated" -> handleDishUpdated(payload);
        }
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .build());
    }

    @DltHandler
    public void handleDlt(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String msg
    ) {
        log.error("Dead letter: event failed all retries and routed to {}. Error: {}, Payload: {}",
                topic, msg, payload);
    }

    private void handleDishCreated(String payload) {
        DishCreatedEvent event = objectMapper.readValue(payload, DishCreatedEvent.class);
        dishService.create(dishEventMapper.toRequestDto(event));
    }

    private void handleDishUpdated(String payload) {
        DishUpdatedEvent event = objectMapper.readValue(payload, DishUpdatedEvent.class);
        dishService.update(dishEventMapper.toRequestDto(event));
    }
}
