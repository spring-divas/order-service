package org.spring.divas.order.feature.payment;

import lombok.Builder;

@Builder
public record PaymentRequestDto(
        Long orderId
) {
    public PaymentRequestDto() {
        this(null);
    }

    public Long getOrderId() {
        return orderId();
    }
}
