package org.spring.divas.order.feature.dish;

import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DishEventListener {
    private final DishEventMapper dishEventMapper;
    private final DishService dishService;

    @KafkaListener(topics = "dish-events", groupId = "dish-creation-group")
    public void handleDishCreated(
            @Payload DishCreatedEvent event
    ) {
        DishRequestDto dto = dishEventMapper.toRequestDto(event);
        dishService.create(dto);
    }

    @KafkaListener(topics = "dish-events", groupId = "dish-update-group")
    public void handleDishUpdate(
            @Payload DishUpdatedEvent event
    ) {
        DishRequestDto dto = dishEventMapper.toRequestDto(event);
        dishService.update(dto);
    }

}
