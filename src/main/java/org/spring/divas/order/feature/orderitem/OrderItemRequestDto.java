package org.spring.divas.order.feature.orderitem;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record OrderItemRequestDto(
        @NotNull Long dishId,
        @NotNull @Positive Integer quantity
) {
    public OrderItemRequestDto() {
        this(0L, 1);
    }

    public Long getDishId() {
        return dishId();
    }

    public Integer getQuantity() {
        return quantity();
    }
}
