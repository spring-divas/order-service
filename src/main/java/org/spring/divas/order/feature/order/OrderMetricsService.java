package org.spring.divas.order.feature.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderMetricsService {

    private final AtomicInteger notReadyOrders = new AtomicInteger(0);
    private final Counter cancelledOrdersCounter;

    public OrderMetricsService(MeterRegistry registry) {
        Gauge.builder("orders.not.ready", notReadyOrders, AtomicInteger::get)
                .description("Current number of not ready yet orders")
                .register(registry);

        this.cancelledOrdersCounter = Counter.builder("orders.cancelled")
                .description("Total number of cancelled orders")
                .register(registry);
    }

    public void setNotReadyOrders(int count) {
        notReadyOrders.set(Math.max(0, count));
    }

    public void incrementCancelledOrders() {
        cancelledOrdersCounter.increment();
    }
}