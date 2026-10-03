package org.spring.divas.order;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderCreationIntegrationTest {

    @RegisterExtension
    static WireMockExtension venueWireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    @RegisterExtension
    static WireMockExtension paymentWireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        registry.add(
                "venue.service.url",
                () -> "http://localhost:" + venueWireMock.getPort()
        );

        registry.add(
                "payment.service.url",
                () -> "http://localhost:" + paymentWireMock.getPort()
        );
    }

    @BeforeEach
    void setUp() {
        venueWireMock.resetAll();
        paymentWireMock.resetAll();

        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderAndCreatePayment() throws Exception {

        venueWireMock.stubFor(
                post(urlEqualTo("/api/dish/batch"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                [
                                                    {
                                                        "id": 1,
                                                        "name": "Pizza",
                                                        "price": 250.00
                                                    },
                                                    {
                                                        "id": 2,
                                                        "name": "Burger",
                                                        "price": 180.00
                                                    }
                                                ]
                                                """)
                        )
        );

        paymentWireMock.stubFor(
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
                                                    "id": 100,
                                                    "orderId": 1,
                                                    "status": "PENDING",
                                                    "createdAt": "2026-10-03T10:00:00"
                                                }
                                                """)
                        )
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/order")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                   "tableId": 1,
                                                   "venueId": 1,
                                                   "items": [
                                                     {
                                                       "dishId": 1,
                                                       "quantity": 2
                                                     },
                                                     {
                                                       "dishId": 2,
                                                       "quantity": 1
                                                     }
                                                   ]
                                                 }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        venueWireMock.verify(
                1,
                postRequestedFor(urlEqualTo("/api/dish/batch"))
        );

        paymentWireMock.verify(
                1,
                postRequestedFor(urlEqualTo("/api/payment"))
                        .withHeader(
                                "Idempotency-Key",
                                matching("order-\\d+")
                        )
        );

        paymentWireMock.verify(
                postRequestedFor(urlEqualTo("/api/payment"))
                        .withRequestBody(
                                equalToJson("""
                                        {
                                            "orderId": 1
                                        }
                                        """)
                        )
        );

        assert orderRepository.count() == 1;
    }


    @Test
    void shouldCreateOrderWhenPaymentServiceFailsAndUseFallback() throws Exception {
        // Venue Service
        venueWireMock.stubFor(
                post(urlEqualTo("/api/dish/batch"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                [
                                                    {
                                                        "id": 1,
                                                        "name": "Pizza",
                                                        "price": 250.00
                                                    },
                                                    {
                                                        "id": 2,
                                                        "name": "Burger",
                                                        "price": 180.00
                                                    }
                                                ]
                                                """)
                        )
        );

        paymentWireMock.stubFor(
                post(String.valueOf(urlEqualTo("/api/payment")))
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
                                                    "detail": "Payment service unavailable"
                                                }
                                                """)
                        )
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/order")
                                .contextPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                    {
                                                       "tableId": 1,
                                                       "venueId": 1,
                                                       "items": [
                                                         {
                                                           "dishId": 1,
                                                           "quantity": 2
                                                         },
                                                         {
                                                           "dishId": 2,
                                                           "quantity": 1
                                                         }
                                                       ]
                                                     }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());


        assertEquals(1, orderRepository.count());

        Order savedOrder = orderRepository.findAll().get(0);
        assertThat(savedOrder.getId()).isNotNull();

        venueWireMock.verify(
                1,
                postRequestedFor(urlEqualTo("/api/dish/batch"))
        );

        // ---------------------------------------------------------
        // 6. Payment був викликаний 3 рази через Retry
        // ---------------------------------------------------------

        paymentWireMock.verify(
                3,
                postRequestedFor(urlEqualTo("/api/payment"))
        );


        paymentWireMock.verify(
                postRequestedFor(urlEqualTo("/api/payment"))
                        .withRequestBody(
                                equalToJson("""
                                        {
                                            "orderId": %d
                                        }
                                        """.formatted(savedOrder.getId()))
                        )
        );


        paymentWireMock.verify(
                postRequestedFor(urlEqualTo("/api/payment"))
                        .withHeader(
                                "Idempotency-Key",
                                equalTo("order-" + savedOrder.getId())
                        )
        );
    }
}