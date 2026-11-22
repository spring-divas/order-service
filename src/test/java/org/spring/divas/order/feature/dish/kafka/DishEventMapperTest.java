package org.spring.divas.order.feature.dish.kafka;

import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.dish.DishRequestDto;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DishEventMapperTest {

    private final DishEventMapper mapper = new DishEventMapper();

    @Test
    void shouldMapDishCreatedEventToRequestDto() {
        UUID eventId = UUID.randomUUID();
        DishCreatedEvent event = new DishCreatedEvent(
                eventId,
                11L,
                2L,
                "Caesar Salad",
                new BigDecimal("9.99")
        );

        DishRequestDto dto = mapper.toRequestDto(event);

        assertNotNull(dto);
        assertEquals(11L, dto.getDishId());
        assertEquals("Caesar Salad", dto.getName());
        assertEquals(new BigDecimal("9.99"), dto.getPrice());

        assertEquals(eventId, event.eventId());
        assertEquals(11L, event.dishId());
        assertEquals(2L, event.venueId());
        assertEquals("Caesar Salad", event.name());
        assertEquals(new BigDecimal("9.99"), event.price());
    }

    @Test
    void shouldMapDishUpdatedEventToRequestDto() {
        UUID eventId = UUID.randomUUID();
        DishUpdatedEvent event = new DishUpdatedEvent(
                eventId,
                11L,
                2L,
                "Caesar Salad Pro",
                new BigDecimal("11.50")
        );

        DishRequestDto dto = mapper.toRequestDto(event);

        assertNotNull(dto);
        assertEquals(11L, dto.getDishId());
        assertEquals("Caesar Salad Pro", dto.getName());
        assertEquals(new BigDecimal("11.50"), dto.getPrice());

        assertEquals(eventId, event.eventId());
        assertEquals(11L, event.dishId());
        assertEquals(2L, event.venueId());
        assertEquals("Caesar Salad Pro", event.name());
        assertEquals(new BigDecimal("11.50"), event.price());
    }
}
