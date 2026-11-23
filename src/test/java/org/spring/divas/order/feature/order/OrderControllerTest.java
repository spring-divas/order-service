package org.spring.divas.order.feature.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.order.common.GlobalExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                                && parameter.getParameterType().equals(Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return Jwt.withTokenValue("test")
                                .header("alg", "none")
                                .subject("10")
                                .claim("roles", List.of("ADMIN"))
                                .build();
                    }
                })
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateOrderWithUserId() throws Exception {
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(1L)
                .userId(10L)
                .venueId(2L)
                .tableId(3L)
                .status(OrderStatus.NEW)
                .build();

        when(orderService.create(any(OrderRequestDto.class), eq(10L))).thenReturn(responseDto);

        String json =
                """
                {
                  "userId": 10,
                  "venueId": 2,
                  "tableId": 3,
                  "items": [
                    {
                      "dishId": 1,
                      "quantity": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10));

        verify(orderService).create(any(OrderRequestDto.class), eq(10L));
    }

    @Test
    void shouldCreateOrderWithoutUserId() throws Exception {
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(2L)
                .userId(null)
                .venueId(2L)
                .tableId(3L)
                .status(OrderStatus.NEW)
                .build();

        when(orderService.create(any(OrderRequestDto.class), eq(10L))).thenReturn(responseDto);

        String json =
                """
                {
                  "venueId": 2,
                  "tableId": 3,
                  "items": [
                    {
                      "dishId": 1,
                      "quantity": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.userId").doesNotExist());

        verify(orderService).create(any(OrderRequestDto.class), eq(10L));
    }

    @Test
    void shouldGetAllOrders() throws Exception {
        OrderResponseDto dto = OrderResponseDto.builder().id(1L).build();
        when(orderService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(orderService).getAll();
    }

    @Test
    void shouldGetOrderById() throws Exception {
        OrderResponseDto dto = OrderResponseDto.builder().id(5L).build();
        when(orderService.getById(5L)).thenReturn(dto);

        mockMvc.perform(get("/order/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(orderService).getById(5L);
    }

    @Test
    void shouldDeleteOrder() throws Exception {
        mockMvc.perform(delete("/order/5"))
                .andExpect(status().isOk());

        verify(orderService).delete(5L);
    }

    @Test
    void shouldUpdateOrder() throws Exception {
        OrderResponseDto dto = OrderResponseDto.builder().id(5L).tableId(10L).build();
        when(orderService.update(eq(5L), any(OrderRequestDto.class))).thenReturn(dto);

        String json =
                """
                {
                  "venueId": 2,
                  "tableId": 10,
                  "items": [
                    {
                      "dishId": 1,
                      "quantity": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/order/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.tableId").value(10));

        verify(orderService).update(eq(5L), any(OrderRequestDto.class));
    }

    @Test
    void shouldCancelOrder() throws Exception {
        OrderResponseDto dto = OrderResponseDto.builder().id(5L).status(OrderStatus.CANCELLED).build();
        when(orderService.cancel(5L, "User cancelled")).thenReturn(dto);

        mockMvc.perform(patch("/order/5/cancel")
                        .param("reason", "User cancelled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancel(5L, "User cancelled");
    }

    @Test
    void shouldPayOrder() throws Exception {
        OrderResponseDto dto = OrderResponseDto.builder().id(5L).status(OrderStatus.PAID).build();
        when(orderService.pay(5L, false)).thenReturn(dto);

        mockMvc.perform(post("/order/5/pay")
                        .param("simulateFailure", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(orderService).pay(5L, false);
    }

    @Test
    void shouldCheckHasUserOrderedDish() throws Exception {
        when(orderService.hasUserOrderedDish(10L, 20L)).thenReturn(true);

        mockMvc.perform(get("/order/exists/dish")
                        .param("userId", "10")
                        .param("dishId", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService).hasUserOrderedDish(10L, 20L);
    }

    @Test
    void shouldCheckHasUserBeenToVenue() throws Exception {
        when(orderService.hasUserBeenToVenue(10L, 30L)).thenReturn(false);

        mockMvc.perform(get("/order/exists/venue")
                        .param("userId", "10")
                        .param("venueId", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(orderService).hasUserBeenToVenue(10L, 30L);
    }
}
