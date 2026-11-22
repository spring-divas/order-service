package org.spring.divas.order.feature.dish;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DishRequestDto(
        @NotNull Long dishId,
        @NotNull @NotBlank String name,
        @NotNull BigDecimal price
) {
    public DishRequestDto() {
        this(0L, "default", BigDecimal.ZERO);
    }

    public Long getDishId() {
        return dishId();
    }

    public String getName() {
        return name();
    }

    public BigDecimal getPrice() {
        return price();
    }
}
