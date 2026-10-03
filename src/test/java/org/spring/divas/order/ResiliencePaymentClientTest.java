package org.spring.divas.order;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.spring.divas.order.config.CorrelationIdContext;
import org.spring.divas.order.feature.payment.PaymentResponseDto;
import org.spring.divas.order.feature.payment.ResilientPaymentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
class ResiliencePaymentClientTest {

    @RegisterExtension
    static WireMockExtension wireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    @Autowired
    private ResilientPaymentClient resilientPaymentClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "payment.service.url",
                () -> "http://localhost:" + wireMock.getPort()
        );
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();

        circuitBreakerRegistry
                .circuitBreaker("paymentClient")
                .reset();
    }

    @Test
    void shouldRetryPaymentRequestThreeTimes() {
        wireMock.stubFor(
                post(urlEqualTo("/api/payment"))
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                                        .withHeader("Content-Type", "application/problem+json")
                                        .withBody("""
                                            {
                                              "type": "about:blank",
                                              "title": "Internal Server Error",
                                              "status": 500,
                                              "detail": "Payment service unavailable"
                                            }
                                            """)
                        )
        );

        PaymentResponseDto response =
                resilientPaymentClient.createPayment(1L, "retry-test");

        assertEquals("PENDING", response.getStatus());

        wireMock.verify(
                3,
                postRequestedFor(urlEqualTo("/api/payment"))
        );
    }

    @Test
    void shouldOpenCircuitBreakerAfterFailureThreshold() {
        wireMock.stubFor(
                post(urlEqualTo("/api/payment"))
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                                        .withHeader("Content-Type", "application/problem+json")
                                        .withBody("""
                                            {
                                              "type": "about:blank",
                                              "title": "Internal Server Error",
                                              "status": 500,
                                              "detail": "Payment service unavailable"
                                            }
                                            """)
                        )
        );

        for (int i = 0; i < 10; i++) {
            resilientPaymentClient.createPayment(
                    1L,
                    "cb-test-" + i
            );
        }

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("paymentClient");

        assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );
    }

    @Test
    void shouldUseFallbackWhenCircuitBreakerIsOpen() {
        openPaymentCircuitBreaker();

        wireMock.resetRequests();

        PaymentResponseDto response =
                resilientPaymentClient.createPayment(
                        1L,
                        "fallback-test"
                );

        assertEquals("PENDING", response.getStatus());
        assertNull(response.getId());
        assertEquals(1L, response.getOrderId());

        wireMock.verify(
                0,
                postRequestedFor(urlEqualTo("/api/payment"))
        );
    }

    @Test
    void shouldPropagateCorrelationId() {

        wireMock.stubFor(
                post(urlEqualTo("/api/payment"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "id": 10,
                                                  "orderId": 1,
                                                  "status": "PENDING",
                                                  "createdAt": "2026-10-03T02:00:00"
                                                }
                                                """)
                        )
        );

        // CorrelationIdContext is ThreadLocal, therefore set it
        // before calling the client.
        CorrelationIdContext.set("test-123");

        try {
            var response = resilientPaymentClient.createPayment(
                    1L,
                    "test-correlation-001"
            );

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(10L);

            wireMock.verify(
                    1,
                    postRequestedFor(urlEqualTo("/api/payment"))
                            .withHeader(
                                    "X-Correlation-Id",
                                    equalTo("test-123")
                            )
            );

        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void shouldRetryWhenPaymentReturnsProblemDetail() {

        wireMock.stubFor(
                post(urlEqualTo("/api/payment"))
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                                        .withHeader(
                                                "Content-Type",
                                                "application/problem+json"
                                        )
                                        .withBody("""
                                                {
                                                  "type": "about:blank",
                                                  "title": "Internal Server Error",
                                                  "status": 500,
                                                  "detail": "Payment service failed"
                                                }
                                                """)
                        )
        );

        var response = resilientPaymentClient.createPayment(
                50L,
                "test-problem-detail-001"
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getOrderId()).isEqualTo(50L);
        assertThat(response.getStatus()).isEqualTo("PENDING");

        // ProblemDetail response has status 500,
        // therefore Resilience4j treats it as a failed call
        // and Retry performs three attempts.
        wireMock.verify(
                3,
                postRequestedFor(urlEqualTo("/api/payment"))
        );
    }

    private void openPaymentCircuitBreaker() {
        wireMock.stubFor(
                post(urlEqualTo("/api/payment"))
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                                        .withHeader("Content-Type", "application/problem+json")
                                        .withBody("""
                                            {
                                              "type": "about:blank",
                                              "title": "Internal Server Error",
                                              "status": 500,
                                              "detail": "Payment service unavailable"
                                            }
                                            """)
                        )
        );

        for (int i = 0; i < 10; i++) {
            resilientPaymentClient.createPayment(
                    1L,
                    "fallback-open-" + i
            );
        }

        assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreakerRegistry
                        .circuitBreaker("paymentClient")
                        .getState()
        );
    }
}