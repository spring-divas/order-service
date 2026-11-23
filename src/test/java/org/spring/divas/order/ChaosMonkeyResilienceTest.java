package org.spring.divas.order;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultException;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.spring.divas.order.feature.payment.PaymentResponseDto;
import org.spring.divas.order.feature.payment.ResilientPaymentClient;
import org.spring.divas.order.feature.venue.ResilientVenueClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "chaos.monkey.watcher.service=false",
        "chaos.monkey.watcher.rest-controller=false",
        "resilience4j.circuitbreaker.instances.venueClient.wait-duration-in-open-state=500ms",
        "resilience4j.circuitbreaker.instances.venueClient.permitted-number-of-calls-in-half-open-state=2"
})
@ActiveProfiles("chaos-monkey")
class ChaosMonkeyResilienceTest {

    private static final int LATENCY_MS = 1000;

    private static final int VENUE_BULKHEAD_SIZE = 5;

    @RegisterExtension
    static WireMockExtension downstream = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private ChaosMonkeySettings chaosMonkey;

    @Autowired
    private ResilientPaymentClient paymentClient;

    @Autowired
    private ResilientVenueClient venueClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @Autowired
    private RetryRegistry retries;

    @DynamicPropertySource
    static void downstreamUrls(DynamicPropertyRegistry registry) {
        registry.add("payment.service.url", downstream::baseUrl);
        registry.add("venue.service.url", downstream::baseUrl);
    }

    @BeforeEach
    void setUp() {
        downstream.resetAll();
        circuitBreakers.circuitBreaker("paymentClient").reset();
        circuitBreakers.circuitBreaker("venueClient").reset();

        AssaultProperties assaults = chaosMonkey.getAssaultProperties();
        assaults.setLevel(1);
        assaults.setDeterministic(true);
        assaults.setLatencyActive(false);
        assaults.setExceptionsActive(false);
        assaults.setException(new AssaultException());
    }

    @AfterEach
    void stopChaos() {
        chaosMonkey.getChaosMonkeyProperties().setEnabled(false);
    }

    @Test
    void paymentFallsBackToPendingWhenPaymentServiceFails() {
        downstream.stubFor(post(urlEqualTo("/api/payment")).willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 10, \"orderId\": 1, \"status\": \"PAID\", \"createdAt\": \"2026-11-22T12:00:00\"}")));

        assertThat(paymentClient.createPayment(1L, "chaos-steady").getStatus()).isEqualTo("PAID");

        unleashExceptions();
        long failedBefore = retries.retry("paymentClient").getMetrics().getNumberOfFailedCallsWithRetryAttempt();
        PaymentResponseDto response = paymentClient.createPayment(2L, "chaos-failure");

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getId()).isNull();
        assertThat(response.getOrderId()).isEqualTo(2L);
        assertThat(retries.retry("paymentClient").getMetrics().getNumberOfFailedCallsWithRetryAttempt())
                .isEqualTo(failedBefore + 1);
        downstream.verify(1, postRequestedFor(urlEqualTo("/api/payment")));
    }

    @Test
    void venueCircuitBreakerOpensUnderChaosAndRecoversAfterwards() {
        stubDishes();
        CircuitBreaker circuitBreaker = circuitBreakers.circuitBreaker("venueClient");

        assertThat(venueClient.getDishesByIds(List.of(1L))).hasSize(1);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        unleashExceptions();
        List<Long> dishIds = List.of(1L);
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> venueClient.getDishesByIds(dishIds))
                    .isInstanceOf(RuntimeException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        downstream.resetRequests();
        assertThatThrownBy(() -> venueClient.getDishesByIds(dishIds))
                .isInstanceOf(CallNotPermittedException.class);
        downstream.verify(0, postRequestedFor(urlEqualTo("/api/dish/batch")));

        chaosMonkey.getChaosMonkeyProperties().setEnabled(false);
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    assertThat(venueClient.getDishesByIds(List.of(1L))).hasSize(1);
                    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
                });
    }

    @Test
    void bulkheadLimitsConcurrentCallsToSlowVenueService() {
        stubDishes();
        AssaultProperties assaults = chaosMonkey.getAssaultProperties();
        assaults.setLatencyActive(true);
        assaults.setLatencyRangeStart(LATENCY_MS);
        assaults.setLatencyRangeEnd(LATENCY_MS);
        chaosMonkey.getChaosMonkeyProperties().setEnabled(true);

        int callers = VENUE_BULKHEAD_SIZE * 2;
        CountDownLatch start = new CountDownLatch(1);
        Callable<Throwable> call = () -> {
            start.await();
            try {
                venueClient.getDishesByIds(List.of(1L));
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        };
        List<Future<Throwable>> calls;
        try (ExecutorService executor = Executors.newFixedThreadPool(callers)) {
            calls = IntStream.range(0, callers)
                    .mapToObj(_ -> executor.submit(call))
                    .toList();
            start.countDown();
        }

        List<Throwable> failures = calls.stream()
                .map(Future::resultNow)
                .filter(Objects::nonNull)
                .toList();

        assertThat(callers - failures.size()).isEqualTo(VENUE_BULKHEAD_SIZE);
        assertThat(failures).allSatisfy(failure -> assertThat(failure).isInstanceOf(BulkheadFullException.class));
    }

    private void unleashExceptions() {
        chaosMonkey.getAssaultProperties().setExceptionsActive(true);
        chaosMonkey.getChaosMonkeyProperties().setEnabled(true);
    }

    private void stubDishes() {
        downstream.stubFor(post(urlEqualTo("/api/dish/batch")).willReturn(
                okJson("[{\"id\": 1, \"name\": \"Banosh\", \"price\": 670.00}]")));
    }
}
