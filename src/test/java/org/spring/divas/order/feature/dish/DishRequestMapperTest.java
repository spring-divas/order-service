package org.spring.divas.order.feature.dish;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DishRequestMapperTest {

    private final DishRequestMapper mapper = Mappers.getMapper(DishRequestMapper.class);

    @Test
    void shouldMapDtoToDish() {
        DishRequestDto dto = DishRequestDto.builder()
                .dishId(1L)
                .name("Pizza Margherita")
                .price(new BigDecimal("15.50"))
                .build();

        Dish dish = mapper.toDish(dto);

        assertNotNull(dish);
        assertEquals(1L, dish.getId());
        assertEquals("Pizza Margherita", dish.getName());
        assertEquals(new BigDecimal("15.50"), dish.getPrice());
    }

    @Test
    void shouldTestDishRequestDtoMethods() {
        DishRequestDto defaultDto = new DishRequestDto();
        assertEquals(0L, defaultDto.dishId());
        assertEquals(0L, defaultDto.getDishId());
        assertEquals("default", defaultDto.name());
        assertEquals("default", defaultDto.getName());
        assertEquals(BigDecimal.ZERO, defaultDto.price());
        assertEquals(BigDecimal.ZERO, defaultDto.getPrice());

        DishRequestDto customDto = new DishRequestDto(2L, "Soup", new BigDecimal("5.00"));
        assertEquals(2L, customDto.dishId());
        assertEquals(2L, customDto.getDishId());
        assertEquals("Soup", customDto.name());
        assertEquals("Soup", customDto.getName());
        assertEquals(new BigDecimal("5.00"), customDto.price());
        assertEquals(new BigDecimal("5.00"), customDto.getPrice());

        DishRequestDto builtDto = DishRequestDto.builder()
                .dishId(2L)
                .name("Soup")
                .price(new BigDecimal("5.00"))
                .build();
        assertEquals(customDto, builtDto);
        assertEquals(customDto.hashCode(), builtDto.hashCode());
        assertNotNull(customDto.toString());
        assertNotEquals(defaultDto, customDto);
    }
}
