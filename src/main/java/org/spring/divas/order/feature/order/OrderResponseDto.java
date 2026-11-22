package org.spring.divas.order.feature.order;

import lombok.Builder;
import org.spring.divas.order.feature.orderitem.OrderItemResponseDto;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderResponseDto(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        Long venueId,
        Long tableId,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponseDto> items
) {
    public OrderResponseDto() {
        this(null, null, null, null, null, null, null, null, null, null);
    }

    public Long getId() {
        return id();
    }

    public Long getUserId() {
        return userId();
    }

    public String getFirstName() {
        return firstName();
    }

    public String getLastName() {
        return lastName();
    }

    public String getEmail() {
        return email();
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

    public LocalDateTime getCreatedAt() {
        return createdAt();
    }

    public List<OrderItemResponseDto> getItems() {
        return items();
    }
}
