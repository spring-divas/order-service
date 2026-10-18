package org.spring.divas.order;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.order.OrderMetricsService;

import static org.junit.jupiter.api.Assertions.*;

class OrderMetricsServiceTest {

    @Test
    void shouldRegisterGaugeAndCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetricsService metrics = new OrderMetricsService(registry);

        assertNotNull(registry.find("orders.not.ready").gauge());
        assertNotNull(registry.find("orders.cancelled").counter());
    }

    @Test
    void gaugeShouldReflectLatestValue() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetricsService metrics = new OrderMetricsService(registry);

        metrics.setNotReadyOrders(5);
        assertEquals(5.0, registry.get("orders.not.ready").gauge().value());

        metrics.setNotReadyOrders(2);
        assertEquals(2.0, registry.get("orders.not.ready").gauge().value());
    }

    @Test
    void gaugeShouldNotBecomeNegative() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetricsService metrics = new OrderMetricsService(registry);

        metrics.setNotReadyOrders(-3);

        assertEquals(0.0, registry.get("orders.not.ready").gauge().value());
    }

    @Test
    void counterShouldIncreaseOnlyWhenIncremented() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetricsService metrics = new OrderMetricsService(registry);

        metrics.incrementCancelledOrders();
        metrics.incrementCancelledOrders();

        assertEquals(2.0, registry.get("orders.cancelled").counter().count());
    }
}
