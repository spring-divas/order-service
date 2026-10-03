package org.spring.divas.order.feature.venue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DishResponseDto(
        Long id,
        String name,
        BigDecimal price
) {}
