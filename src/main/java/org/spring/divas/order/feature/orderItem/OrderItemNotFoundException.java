package org.spring.divas.order.feature.orderItem;

public class OrderItemNotFoundException extends RuntimeException {

    public OrderItemNotFoundException(Long id) {
        super("Order Item with id " + id + " not found");
    }
}