package org.spring.divas.order.feature.dish;

import org.spring.divas.dish.v1.DishChange;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

@Component
public class DishProtoMapper {
    public DishRequestDto toRequestDto(DishChange change) {
        return DishRequestDto.builder()
                .dishId(change.getId())
                .name(change.getName())
                .price(toBigDecimal(change))
                .build();
    }

    private BigDecimal toBigDecimal(DishChange change) {
        var price = change.getPrice();
        return new BigDecimal(new BigInteger(price.getValue().toByteArray()), price.getScale());
    }
}
