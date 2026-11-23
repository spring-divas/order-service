package org.spring.divas.order.feature.order;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.AbstractToxiproxyIntegrationTest;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.dish.DishService;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;
import org.spring.divas.order.feature.outbox.OutboxRepository;
import org.spring.divas.order.feature.user.ResilientUserClient;
import org.spring.divas.order.feature.user.UserResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderToxiproxyIntegrationTest extends AbstractToxiproxyIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private DishService dishService;

    @MockitoBean
    private ResilientUserClient userClient;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
        when(dishService.getAllByIds(List.of(7L)))
                .thenReturn(List.of(new DishResponseDto(7L, 42L, "Borscht", BigDecimal.TEN)));
        when(userClient.findById(1L))
                .thenReturn(new UserResponseDto(
                        1L, "Alice", "Smith",
                        "alice@example.com", null));
    }

    @Test
    void createsOrderThroughPostgresProxy() throws Exception {
        mockMvc.perform(post("/order").with(jwt().jwt(token -> token.subject("1").claim("roles", List.of("CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(order())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.venueId").value(42))
                .andExpect(jsonPath("$.items[0].name").value("Borscht"));
    }

    @Test
    void createsOrderReturnsServerErrorWhenPostgresResponseTimesOut() throws Exception {
        POSTGRES_PROXY.toxics().timeout("postgres-response-timeout", ToxicDirection.DOWNSTREAM, 0);
        evictConnections();
        mockMvc.perform(post("/order").with(jwt().jwt(token -> token.subject("1").claim("roles", List.of("CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(order())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"));
    }

    private OrderRequestDto order() {
        return new OrderRequestDto(
                1L, 42L, 3L, null,
                List.of(new OrderItemRequestDto(7L, 2))
        );
    }
}
