package org.spring.divas.order.feature.orderItem;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequestDto(
        @NotNull
        Long dishId,

        @NotNull
        @Positive
        Integer quantity
) {
}