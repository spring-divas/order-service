package org.spring.divas.order.feature.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderMetricsService {

    private final AtomicInteger notReadyOrders = new AtomicInteger(0);
    private final Counter cancelledOrdersCounter;
    private final DistributionSummary orderPriceDistributionSummary;

    public OrderMetricsService(MeterRegistry registry) {
        Gauge.builder("orders.not.ready", notReadyOrders, AtomicInteger::get)
                .description("Current number of not ready yet orders")
                .register(registry);

        this.cancelledOrdersCounter = Counter.builder("orders.cancelled")
                .description("Total number of cancelled orders")
                .register(registry);

        this.orderPriceDistributionSummary = DistributionSummary.builder("orders.price")
            .description("Order's summary price distribution summary")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);
    }

    public void setNotReadyOrders(int count) {
        notReadyOrders.set(Math.max(0, count));
    }

    public void incrementCancelledOrders() {
        cancelledOrdersCounter.increment();
    }

    public void recordOrderPrice(BigDecimal price) {
        orderPriceDistributionSummary.record(price.doubleValue());
    }
}