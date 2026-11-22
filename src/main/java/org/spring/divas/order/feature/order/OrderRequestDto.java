package org.spring.divas.order.feature.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;

import java.util.List;

@Builder
public record OrderRequestDto(
        Long userId,
        @NotNull Long venueId,
        @NotNull Long tableId,
        OrderStatus status,
        @NotEmpty List<@Valid OrderItemRequestDto> items
) {
    public OrderRequestDto() {
        this(null, 0L, 0L, null, List.of());
    }

    public Long getUserId() {
        return userId();
    }

    public Long getVenueId() {
        return venueId();
    }

    public Long getTableId() {
        return tableId();
    }

    public OrderStatus getStatus() {
        return status();
    }

    public List<OrderItemRequestDto> getItems() {
        return items();
    }
}
