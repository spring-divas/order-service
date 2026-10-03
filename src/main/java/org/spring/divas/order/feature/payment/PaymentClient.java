package org.spring.divas.order.feature.payment;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PostExchange;

public interface PaymentClient {

    @PostExchange("/api/payment")
    PaymentResponseDto createPayment(
            @RequestBody PaymentRequestDto request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    );
}