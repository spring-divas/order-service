package org.spring.divas.order.feature.payment;

import org.spring.divas.payment.v1.Money;

import java.math.BigDecimal;

public final class PaymentProtoMapper {

    private PaymentProtoMapper() {
    }

    public static Money toMoney(BigDecimal amount, String currency) {
        if (amount == null) {
            return Money.newBuilder()
                    .setCurrencyCode(currency != null ? currency : "USD")
                    .build();
        }
        long units = amount.longValue();
        int nanos = amount.remainder(BigDecimal.ONE).movePointRight(9).intValue();
        return Money.newBuilder()
                .setCurrencyCode(currency != null ? currency : "USD")
                .setUnits(units)
                .setNanos(nanos)
                .build();
    }

    public static BigDecimal toBigDecimal(Money money) {
        if (money == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(money.getUnits())
                .add(BigDecimal.valueOf(money.getNanos(), 9));
    }
}
