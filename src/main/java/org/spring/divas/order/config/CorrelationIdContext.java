package org.spring.divas.order.config;

public final class CorrelationIdContext {

    private static final ThreadLocal<String> CORRELATION_ID =
            new ThreadLocal<>();

    private CorrelationIdContext() {
    }

    public static void set(String id) {
        CORRELATION_ID.set(id);
    }

    public static String get() {
        return CORRELATION_ID.get();
    }

    public static void clear() {
        CORRELATION_ID.remove();
    }
}