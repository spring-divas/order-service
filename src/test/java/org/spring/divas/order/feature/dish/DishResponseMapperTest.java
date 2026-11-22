package org.spring.divas.order.feature.dish;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DishResponseMapperTest {

    private final DishResponseMapper mapper = new DishResponseMapper();

    @Test
    void shouldMapDishToDto() {
        Dish dish = Dish.builder()
                .id(10L)
                .name("Carbonara")
                .price(new BigDecimal("18.00"))
                .build();

        DishResponseDto dto = mapper.toDto(dish);

        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals(10L, dto.getId());
        assertEquals("Carbonara", dto.name());
        assertEquals("Carbonara", dto.getName());
        assertEquals(new BigDecimal("18.00"), dto.price());
        assertEquals(new BigDecimal("18.00"), dto.getPrice());
        assertNull(dto.venueId());
        assertNull(dto.getVenueId());
    }

    @Test
    void shouldTestDishResponseDtoMethods() {
        DishResponseDto defaultDto = new DishResponseDto();
        assertNull(defaultDto.id());
        assertNull(defaultDto.getId());
        assertNull(defaultDto.venueId());
        assertNull(defaultDto.getVenueId());
        assertNull(defaultDto.name());
        assertNull(defaultDto.getName());
        assertNull(defaultDto.price());
        assertNull(defaultDto.getPrice());

        DishResponseDto customDto = new DishResponseDto(5L, 3L, "Burger", new BigDecimal("12.00"));
        assertEquals(5L, customDto.id());
        assertEquals(5L, customDto.getId());
        assertEquals(3L, customDto.venueId());
        assertEquals(3L, customDto.getVenueId());
        assertEquals("Burger", customDto.name());
        assertEquals("Burger", customDto.getName());
        assertEquals(new BigDecimal("12.00"), customDto.price());
        assertEquals(new BigDecimal("12.00"), customDto.getPrice());

        DishResponseDto builtDto = DishResponseDto.builder()
                .id(5L)
                .venueId(3L)
                .name("Burger")
                .price(new BigDecimal("12.00"))
                .build();
        assertEquals(customDto, builtDto);
        assertEquals(customDto.hashCode(), builtDto.hashCode());
        assertNotNull(customDto.toString());
        assertNotEquals(defaultDto, customDto);
    }
}
