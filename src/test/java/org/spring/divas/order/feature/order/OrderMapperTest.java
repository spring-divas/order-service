package org.spring.divas.order.feature.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.orderitem.OrderItem;
import org.spring.divas.order.feature.orderitem.OrderItemMapper;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;
import org.spring.divas.order.feature.orderitem.OrderItemResponseDto;
import org.spring.divas.order.feature.user.UserResponseDto;
import org.spring.divas.order.feature.user.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMapperTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;

    @BeforeEach
    void setUp() {
        orderItemMapper = new OrderItemMapper();
        orderMapper = new OrderMapper(orderItemMapper);
    }

    @Test
    void shouldReturnNullWhenToResponseWithNullOrder() {
        assertNull(orderMapper.toResponse(null));
    }

    @Test
    void shouldMapOrderToResponseDto() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 22, 12, 0);
        Order order = Order.builder()
                .id(1L)
                .userId(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .venueId(2L)
                .tableId(5L)
                .status(OrderStatus.NEW)
                .createdAt(createdAt)
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(100L)
                .order(order)
                .dishId(50L)
                .name("Pizza")
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build();
        order.getItems().add(item);

        OrderResponseDto response = orderMapper.toResponse(order);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(1L, response.getId());
        assertEquals(10L, response.userId());
        assertEquals(10L, response.getUserId());
        assertEquals("John", response.firstName());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.lastName());
        assertEquals("Doe", response.getLastName());
        assertEquals("john@example.com", response.email());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(2L, response.venueId());
        assertEquals(2L, response.getVenueId());
        assertEquals(5L, response.tableId());
        assertEquals(5L, response.getTableId());
        assertEquals(OrderStatus.NEW, response.status());
        assertEquals(OrderStatus.NEW, response.getStatus());
        assertEquals(createdAt, response.createdAt());
        assertEquals(createdAt, response.getCreatedAt());
        assertEquals(1, response.items().size());
        assertEquals(1, response.getItems().size());
        assertEquals("Pizza", response.items().get(0).name());
    }

    @Test
    void shouldReturnNullWhenToEntityWithNullRequest() {
        UserResponseDto user = UserResponseDto.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        assertNull(orderMapper.toEntity(null, List.of(), user));
    }

    @Test
    void shouldMapOrderRequestDtoToEntity() {
        UserResponseDto user = UserResponseDto.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        DishResponseDto dish = DishResponseDto.builder()
                .id(7L)
                .name("Pasta")
                .price(new BigDecimal("12.00"))
                .build();

        OrderItemRequestDto itemRequest = OrderItemRequestDto.builder()
                .dishId(7L)
                .quantity(3)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .userId(1L)
                .venueId(10L)
                .tableId(20L)
                .status(OrderStatus.NEW)
                .items(List.of(itemRequest))
                .build();

        Order entity = orderMapper.toEntity(request, List.of(dish), user);

        assertNotNull(entity);
        assertEquals(1L, entity.getUserId());
        assertEquals("Jane", entity.getFirstName());
        assertEquals("Doe", entity.getLastName());
        assertEquals("jane@example.com", entity.getEmail());
        assertEquals(10L, entity.getVenueId());
        assertEquals(20L, entity.getTableId());
        assertEquals(OrderStatus.NEW, entity.getStatus());
        assertNotNull(entity.getItems());
        assertEquals(1, entity.getItems().size());

        OrderItem itemEntity = entity.getItems().get(0);
        assertEquals(entity, itemEntity.getOrder());
        assertEquals(7L, itemEntity.getDishId());
        assertEquals("Pasta", itemEntity.getName());
        assertEquals(3, itemEntity.getQuantity());
        assertEquals(new BigDecimal("12.00"), itemEntity.getPrice());
    }

    @Test
    void shouldTestOrderRequestAndResponseDtoMethods() {
        OrderRequestDto defaultRequest = new OrderRequestDto();
        assertNull(defaultRequest.userId());
        assertNull(defaultRequest.getUserId());
        assertEquals(0L, defaultRequest.venueId());
        assertEquals(0L, defaultRequest.getVenueId());
        assertEquals(0L, defaultRequest.tableId());
        assertEquals(0L, defaultRequest.getTableId());
        assertNull(defaultRequest.status());
        assertNull(defaultRequest.getStatus());
        assertNotNull(defaultRequest.items());
        assertNotNull(defaultRequest.getItems());
        assertEquals(0, defaultRequest.items().size());

        OrderRequestDto customRequest = new OrderRequestDto(
                5L, 3L, 8L, OrderStatus.NEW, List.of()
        );
        assertEquals(5L, customRequest.getUserId());
        assertEquals(3L, customRequest.getVenueId());
        assertEquals(8L, customRequest.getTableId());
        assertEquals(OrderStatus.NEW, customRequest.getStatus());
        assertNotNull(customRequest.toString());
        assertNotEquals(defaultRequest, customRequest);

        OrderResponseDto defaultResponse = new OrderResponseDto();
        assertNull(defaultResponse.id());
        assertNull(defaultResponse.getId());
        assertNull(defaultResponse.userId());
        assertNull(defaultResponse.getUserId());
        assertNull(defaultResponse.firstName());
        assertNull(defaultResponse.getFirstName());
        assertNull(defaultResponse.lastName());
        assertNull(defaultResponse.getLastName());
        assertNull(defaultResponse.email());
        assertNull(defaultResponse.getEmail());
        assertNull(defaultResponse.venueId());
        assertNull(defaultResponse.getVenueId());
        assertNull(defaultResponse.tableId());
        assertNull(defaultResponse.getTableId());
        assertNull(defaultResponse.status());
        assertNull(defaultResponse.getStatus());
        assertNull(defaultResponse.createdAt());
        assertNull(defaultResponse.getCreatedAt());
        assertNull(defaultResponse.items());
        assertNull(defaultResponse.getItems());

        OrderResponseDto builtResponse = OrderResponseDto.builder()
                .id(1L)
                .userId(2L)
                .firstName("A")
                .lastName("B")
                .email("a@b.com")
                .venueId(3L)
                .tableId(4L)
                .status(OrderStatus.READY)
                .createdAt(LocalDateTime.now())
                .items(List.of())
                .build();
        assertNotNull(builtResponse.toString());
        assertNotEquals(defaultResponse, builtResponse);
    }
}
