package org.spring.divas.order.feature.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.spring.divas.order.feature.orderItem.OrderItemRequestDto;

import java.util.List;

public record OrderRequestDto(
        Long userId,

        @NotNull
        Long tableId,

        @NotEmpty
        List<@Valid OrderItemRequestDto> items
) {
}