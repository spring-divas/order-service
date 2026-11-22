package org.spring.divas.order.feature.orderitem;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItemResponseDto(
        Long id,
        Long dishId,
        String name,
        Integer quantity,
        BigDecimal price
) {
    public OrderItemResponseDto() {
        this(null, null, null, null, null);
    }

    public Long getId() {
        return id();
    }

    public Long getDishId() {
        return dishId();
    }

    public String getName() {
        return name();
    }

    public Integer getQuantity() {
        return quantity();
    }

    public BigDecimal getPrice() {
        return price();
    }
}
