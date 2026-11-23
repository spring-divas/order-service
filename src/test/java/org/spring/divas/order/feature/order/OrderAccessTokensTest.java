package org.spring.divas.order.feature.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderAccessTokensTest {

    @Test
    void shouldGenerateDistinctUrlSafeTokens() {
        String first = OrderAccessTokens.generate();
        String second = OrderAccessTokens.generate();

        assertNotEquals(first, second);
        assertEquals(43, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void shouldHashTokenDeterministicallyWithSha256() {
        String hash = OrderAccessTokens.hash("token");

        assertEquals(hash, OrderAccessTokens.hash("token"));
        assertEquals(64, hash.length());
        assertNotEquals(hash, OrderAccessTokens.hash("other-token"));
    }

    @Test
    void shouldMatchOnlyTokenWithSameHash() {
        String token = OrderAccessTokens.generate();
        String hash = OrderAccessTokens.hash(token);

        assertTrue(OrderAccessTokens.matches(token, hash));
        assertFalse(OrderAccessTokens.matches(OrderAccessTokens.generate(), hash));
        assertFalse(OrderAccessTokens.matches(null, hash));
        assertFalse(OrderAccessTokens.matches(" ", hash));
        assertFalse(OrderAccessTokens.matches(token, null));
    }
}
