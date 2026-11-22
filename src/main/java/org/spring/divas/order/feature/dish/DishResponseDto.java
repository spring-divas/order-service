package org.spring.divas.order.feature.dish;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DishResponseDto(
        Long id,
        Long venueId,
        String name,
        BigDecimal price
) {
    public DishResponseDto() {
        this(null, null, null, null);
    }

    public Long getId() {
        return id();
    }

    public Long getVenueId() {
        return venueId();
    }

    public String getName() {
        return name();
    }

    public BigDecimal getPrice() {
        return price();
    }
}
