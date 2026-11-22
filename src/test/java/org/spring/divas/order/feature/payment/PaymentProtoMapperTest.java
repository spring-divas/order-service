package org.spring.divas.order.feature.payment;

import org.junit.jupiter.api.Test;
import org.spring.divas.payment.v1.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentProtoMapperTest {

    @Test
    void shouldMapNullAmountToDefaultMoney() {
        Money money = PaymentProtoMapper.toMoney(null, null);

        assertNotNull(money);
        assertEquals("USD", money.getCurrencyCode());
        assertEquals(0L, money.getUnits());
        assertEquals(0, money.getNanos());
    }

    @Test
    void shouldMapNullAmountWithCustomCurrency() {
        Money money = PaymentProtoMapper.toMoney(null, "EUR");

        assertNotNull(money);
        assertEquals("EUR", money.getCurrencyCode());
        assertEquals(0L, money.getUnits());
        assertEquals(0, money.getNanos());
    }

    @Test
    void shouldMapAmountToMoneyCorrectly() {
        BigDecimal amount = new BigDecimal("123.456000000");

        Money money = PaymentProtoMapper.toMoney(amount, "USD");

        assertNotNull(money);
        assertEquals("USD", money.getCurrencyCode());
        assertEquals(123L, money.getUnits());
        assertEquals(456000000, money.getNanos());
    }

    @Test
    void shouldReturnZeroBigDecimalWhenMoneyIsNull() {
        BigDecimal result = PaymentProtoMapper.toBigDecimal(null);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldMapMoneyToBigDecimalCorrectly() {
        Money money = Money.newBuilder()
                .setCurrencyCode("USD")
                .setUnits(123L)
                .setNanos(456000000)
                .build();

        BigDecimal result = PaymentProtoMapper.toBigDecimal(money);

        assertEquals(0, new BigDecimal("123.456").compareTo(result));
    }

    @Test
    void shouldTestPaymentRequestAndResponseDtoMethods() {
        PaymentRequestDto defaultRequest = new PaymentRequestDto();
        assertNull(defaultRequest.orderId());
        assertNull(defaultRequest.getOrderId());

        PaymentRequestDto customRequest = new PaymentRequestDto(42L);
        assertEquals(42L, customRequest.orderId());
        assertEquals(42L, customRequest.getOrderId());
        assertNotNull(customRequest.toString());
        assertNotEquals(defaultRequest, customRequest);

        PaymentResponseDto defaultResponse = new PaymentResponseDto();
        assertNull(defaultResponse.id());
        assertNull(defaultResponse.getId());
        assertNull(defaultResponse.orderId());
        assertNull(defaultResponse.getOrderId());
        assertNull(defaultResponse.status());
        assertNull(defaultResponse.getStatus());
        assertNull(defaultResponse.createdAt());
        assertNull(defaultResponse.getCreatedAt());

        LocalDateTime now = LocalDateTime.now();
        PaymentResponseDto customResponse = new PaymentResponseDto(1L, 42L, "SUCCESS", now);
        assertEquals(1L, customResponse.id());
        assertEquals(1L, customResponse.getId());
        assertEquals(42L, customResponse.orderId());
        assertEquals(42L, customResponse.getOrderId());
        assertEquals("SUCCESS", customResponse.status());
        assertEquals("SUCCESS", customResponse.getStatus());
        assertEquals(now, customResponse.createdAt());
        assertEquals(now, customResponse.getCreatedAt());
        assertNotNull(customResponse.toString());
        assertNotEquals(defaultResponse, customResponse);
    }
}
