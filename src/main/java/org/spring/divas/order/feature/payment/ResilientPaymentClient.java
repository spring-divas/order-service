package org.spring.divas.order.feature.payment;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ResilientPaymentClient {

    private final PaymentClient paymentClient;

    @Bulkhead(name = "paymentClient")
    @CircuitBreaker(name = "paymentClient", fallbackMethod = "createPaymentFallback")
    @Retry(name = "paymentClient")
    public PaymentResponseDto createPayment(Long orderId, String idempotencyKey) {

        return paymentClient.createPayment(new PaymentRequestDto(orderId), idempotencyKey);

    }

    private PaymentResponseDto createPaymentFallback(Long orderId, String idempotencyKey,
                                                     Throwable throwable) {

        return new PaymentResponseDto(null, orderId, "PENDING", LocalDateTime.now());

    }
}