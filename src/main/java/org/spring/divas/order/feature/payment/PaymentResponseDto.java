package org.spring.divas.order.feature.payment;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PaymentResponseDto(
        Long id,
        Long orderId,
        String status,
        LocalDateTime createdAt
) {
    public PaymentResponseDto() {
        this(null, null, null, null);
    }

    public Long getId() {
        return id();
    }

    public Long getOrderId() {
        return orderId();
    }

    public String getStatus() {
        return status();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt();
    }
}
