package org.spring.divas.order;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.venue.DishResponseDto;
import org.spring.divas.order.feature.venue.ResilientVenueClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {"venue.service.url=http://localhost:8067"})
@WireMockTest(httpPort = 8067)
@Import(TestClientConfig.class)
public class ResilienceVenueClientTest {

    @Autowired
    private ResilientVenueClient venueClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setup() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("venueClient");
        circuitBreaker.reset();
    }

    @Test
    void shouldReturnDishesWhenServiceIsHealthy() {
        List<DishResponseDto> dish = List.of(
                new DishResponseDto(1L, "Banosh", new BigDecimal("670.00")));
        stubFor(post(urlEqualTo("/api/dish/batch"))
                .willReturn(okJson(objectMapper.writeValueAsString(dish))));
        List<DishResponseDto> response = venueClient.getDishesByIds(List.of(1L));
        assertEquals(1, response.size());
        assertEquals(dish.getFirst().name(), response.getFirst().name());
        assertEquals(dish.getFirst().price(), response.getFirst().price());
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void shouldReturnFallbackWhenServiceFails() {
        stubFor(post(urlEqualTo("/api/dish/batch"))
                .willReturn(serverError()));
        List<DishResponseDto> response = venueClient.getDishesByIds(List.of(1L));
        assertEquals(1, response.size());
        assertEquals("Temporally inaccessible", response.getFirst().name());
        assertEquals(BigDecimal.ZERO, response.getFirst().price());
    }

    @Test
    void shouldOpenCircuitBreakerOnFailureStorm() {
        stubFor(post(urlEqualTo("/api/dish/batch"))
                .willReturn(serverError()));
        for (int i = 0; i < 20; i++)
            venueClient.getDishesByIds(List.of(1L));
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }
}
