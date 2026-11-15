package org.spring.divas.order.feature.dish.grpc;

import com.google.protobuf.ByteString;
import lombok.experimental.UtilityClass;
import org.spring.divas.dish.v1.DecimalValue;
import org.spring.divas.dish.v1.DishChange;

import java.math.BigDecimal;
import java.math.BigInteger;

@UtilityClass
class DishChangeFixture {
    static DishChange borscht() {
        BigDecimal price = new BigDecimal("125.50");
        return DishChange.newBuilder()
                .setId(7L)
                .setName("Borscht")
                .setPrice(DecimalValue.newBuilder()
                        .setScale(price.scale())
                        .setPrecision(price.precision())
                        .setValue(ByteString.copyFrom(price.unscaledValue().toByteArray()))
                        .build())
                .build();
    }

    static BigDecimal priceOf(DishChange change) {
        DecimalValue price = change.getPrice();
        return new BigDecimal(new BigInteger(price.getValue().toByteArray()), price.getScale());
    }

    static long idOf(DishChange change) {
        return change.getId();
    }

    static String nameOf(DishChange change) {
        return change.getName();
    }
}
