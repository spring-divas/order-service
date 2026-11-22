package org.spring.divas.order.feature.dish.grpc;

import org.junit.jupiter.api.Test;
import org.spring.divas.dish.v1.DishChange;
import org.spring.divas.order.feature.dish.DishRequestDto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DishProtoMapperTest {

    private final DishProtoMapper mapper = new DishProtoMapper();

    @Test
    void shouldMapDishChangeToRequestDto() {
        DishChange change = DishChangeFixture.borscht();

        DishRequestDto dto = mapper.toRequestDto(change);

        assertNotNull(dto);
        assertEquals(7L, dto.getDishId());
        assertEquals("Borscht", dto.getName());
        assertEquals(new BigDecimal("125.50"), dto.getPrice());
    }
}
