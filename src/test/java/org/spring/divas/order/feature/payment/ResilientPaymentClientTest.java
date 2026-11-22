package org.spring.divas.order.feature.payment;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResilientPaymentClientTest {

    private final PaymentClient paymentClient = mock(PaymentClient.class);
    private final ResilientPaymentClient client = new ResilientPaymentClient(paymentClient);

    @Test
    void shouldCreatePaymentSuccessfully() {
        PaymentResponseDto expected = new PaymentResponseDto(1L, 100L, "PAID", LocalDateTime.now());
        when(paymentClient.createPayment(any(PaymentRequestDto.class), eq("idemp-key-1")))
                .thenReturn(expected);

        PaymentResponseDto actual = client.createPayment(100L, "idemp-key-1");

        assertNotNull(actual);
        assertEquals(expected, actual);
        verify(paymentClient).createPayment(new PaymentRequestDto(100L), "idemp-key-1");
    }

    @Test
    void shouldExecuteFallbackWithThrowable() {
        Throwable error = new RuntimeException("Payment service unavailable");

        PaymentResponseDto fallbackResponse = ReflectionTestUtils.invokeMethod(
                client,
                "createPaymentFallback",
                100L,
                "idemp-key-2",
                error
        );

        assertNotNull(fallbackResponse);
        assertNull(fallbackResponse.id());
        assertEquals(100L, fallbackResponse.orderId());
        assertEquals("PENDING", fallbackResponse.status());
        assertNotNull(fallbackResponse.createdAt());
    }

    @Test
    void shouldExecuteFallbackWithNullThrowable() {
        PaymentResponseDto fallbackResponse = ReflectionTestUtils.invokeMethod(
                client,
                "createPaymentFallback",
                100L,
                "idemp-key-3",
                (Throwable) null
        );

        assertNotNull(fallbackResponse);
        assertNull(fallbackResponse.id());
        assertEquals(100L, fallbackResponse.orderId());
        assertEquals("PENDING", fallbackResponse.status());
        assertNotNull(fallbackResponse.createdAt());
    }
}
