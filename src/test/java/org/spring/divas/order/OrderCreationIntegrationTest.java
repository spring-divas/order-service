package org.spring.divas.order;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.spring.divas.order.feature.dish.Dish;
import org.spring.divas.order.feature.dish.DishRepository;
import org.spring.divas.order.feature.order.Order;
import org.spring.divas.order.feature.order.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"dish-events", "dish-events-dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class OrderCreationIntegrationTest {

    @RegisterExtension
    static WireMockExtension paymentWireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DishRepository dishRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "payment.service.url",
                () -> "http://localhost:" + paymentWireMock.getPort()
        );
    }

    @BeforeEach
    void setUp() {
        paymentWireMock.resetAll();
        orderRepository.deleteAll();
        dishRepository.deleteAll();
        dishRepository.save(new Dish(1L, "Pizza", BigDecimal.valueOf(250.00)));
        dishRepository.save(new Dish(2L,  "Burger", BigDecimal.valueOf(180.00)));
    }

    @Test
    void shouldCreateOrderAndCreatePayment() throws Exception {
        paymentWireMock.stubFor(
                post(urlEqualTo("/api/payment"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "application/json")
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

        paymentWireMock.verify(
                1,
                postRequestedFor(urlEqualTo("/api/payment"))
                        .withHeader("Idempotency-Key", matching("order-\\d+"))
        );

        assertEquals(1, orderRepository.count());
    }

    @Test
    void shouldCreateOrderWhenPaymentServiceFailsAndUseFallback() throws Exception {
        paymentWireMock.stubFor(
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
        Order savedOrder = orderRepository.findAll().getFirst();
        assertThat(savedOrder.getId()).isNotNull();

        paymentWireMock.verify(
                3,
                postRequestedFor(urlEqualTo("/api/payment"))
        );
    }
}