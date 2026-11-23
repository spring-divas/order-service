package org.spring.divas.order.feature.order;

import org.junit.jupiter.api.Test;
import org.spring.divas.order.config.JwtSecurityConfig;
import org.spring.divas.order.config.RateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrderController.class,
        properties = "security.jwt.enabled=true",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class)
)
@Import({JwtSecurityConfig.class, OrderControllerSecurityTest.MethodSecurity.class})
class OrderControllerSecurityTest {

    private static final String ORDER_JSON =
            """
            {"venueId": 2, "tableId": 3, "items": [{"dishId": 1, "quantity": 1}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean(name = "jwtPublicKey")
    private RSAPublicKey jwtPublicKey;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldAllowGuestToCreateOrderWithoutUserId() throws Exception {
        mockMvc.perform(createOrder()).andExpect(status().isCreated());

        verify(orderService).create(any(OrderRequestDto.class), isNull());
    }

    @Test
    void shouldCreateCustomerOrderWithUserIdFromToken() throws Exception {
        mockMvc.perform(createOrder().with(customer())).andExpect(status().isCreated());

        verify(orderService).create(any(OrderRequestDto.class), eq(10L));
    }

    @Test
    void shouldRejectOrderTrackingForGuest() throws Exception {
        mockMvc.perform(get("/order")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/order/{id}", 1L)).andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldForbidCustomerFromViewingGuestOrder() throws Exception {
        when(orderService.getById(1L)).thenReturn(OrderResponseDto.builder()
                .id(1L).venueId(2L).tableId(3L).status(OrderStatus.NEW).build());

        mockMvc.perform(get("/order/{id}", 1L).with(customer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowGuestToPayAndCancelOrderWithAccessToken() throws Exception {
        when(orderService.hasGuestAccess(1L, "guest-token")).thenReturn(true);

        mockMvc.perform(post("/order/{id}/pay", 1L)
                        .header(OrderController.ORDER_TOKEN_HEADER, "guest-token"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/order/{id}/cancel", 1L)
                        .header(OrderController.ORDER_TOKEN_HEADER, "guest-token"))
                .andExpect(status().isOk());

        verify(orderService).pay(1L, false);
        verify(orderService).cancel(eq(1L), any());
    }

    @Test
    void shouldForbidGuestPaymentWithoutValidAccessToken() throws Exception {
        when(orderService.hasGuestAccess(1L, "wrong-token")).thenReturn(false);

        mockMvc.perform(post("/order/{id}/pay", 1L)).andExpect(status().isForbidden());
        mockMvc.perform(post("/order/{id}/pay", 1L)
                        .header(OrderController.ORDER_TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).pay(any(), anyBoolean());
    }

    @Test
    void shouldForbidCustomerFromPayingGuestOrderWithoutAccessToken() throws Exception {
        when(orderService.getById(1L)).thenReturn(OrderResponseDto.builder()
                .id(1L).venueId(2L).tableId(3L).status(OrderStatus.NEW).build());

        mockMvc.perform(post("/order/{id}/pay", 1L).with(customer()))
                .andExpect(status().isForbidden());

        verify(orderService, never()).pay(any(), anyBoolean());
    }

    private static MockHttpServletRequestBuilder createOrder() {
        return post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ORDER_JSON);
    }

    private static RequestPostProcessor customer() {
        return jwt()
                .jwt(token -> token.subject("10").claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class MethodSecurity {
    }
}
