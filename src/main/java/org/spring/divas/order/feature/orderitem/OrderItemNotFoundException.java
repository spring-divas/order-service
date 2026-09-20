package org.spring.divas.order.feature.orderitem;

public class OrderItemNotFoundException extends RuntimeException {

    public OrderItemNotFoundException(Long id) {
        super("Order Item with id " + id + " not found");
    }
}