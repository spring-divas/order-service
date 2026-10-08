package org.spring.divas.order.feature.dish;

public class DishNotFoundException extends RuntimeException {
    public DishNotFoundException(Long id) {
        super("Dish with id " + id + " not found");
    }
}
