package org.spring.divas.order.feature.orderitem;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.spring.divas.order.feature.dish.DishResponseDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderItemMapperTest {

    private final OrderItemMapper mapper = new OrderItemMapper();

    @Test
    void shouldReturnNullWhenToResponseWithNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void shouldMapOrderItemToResponseDto() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .dishId(20L)
                .name("Tiramisu")
                .quantity(2)
                .price(new BigDecimal("7.50"))
                .build();

        OrderItemResponseDto dto = mapper.toResponse(item);

        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals(1L, dto.getId());
        assertEquals(20L, dto.dishId());
        assertEquals(20L, dto.getDishId());
        assertEquals("Tiramisu", dto.name());
        assertEquals("Tiramisu", dto.getName());
        assertEquals(2, dto.quantity());
        assertEquals(2, dto.getQuantity());
        assertEquals(new BigDecimal("7.50"), dto.price());
        assertEquals(new BigDecimal("7.50"), dto.getPrice());
    }

    @Test
    void shouldReturnNullWhenToEntityWithNullRequest() {
        DishResponseDto dish = DishResponseDto.builder()
                .id(5L)
                .name("Tea")
                .price(new BigDecimal("2.00"))
                .build();

        assertNull(mapper.toEntity(null, dish));
    }

    @Test
    void shouldMapOrderItemRequestDtoToEntity() {
        OrderItemRequestDto request = OrderItemRequestDto.builder()
                .dishId(5L)
                .quantity(3)
                .build();

        DishResponseDto dish = DishResponseDto.builder()
                .id(5L)
                .name("Tea")
                .price(new BigDecimal("2.00"))
                .build();

        OrderItem item = mapper.toEntity(request, dish);

        assertNotNull(item);
        assertEquals(5L, item.getDishId());
        assertEquals(3, item.getQuantity());
        assertEquals("Tea", item.getName());
        assertEquals(new BigDecimal("2.00"), item.getPrice());
    }

    @Test
    void shouldMapListToEntitiesSuccessfully() {
        OrderItemRequestDto item1 = OrderItemRequestDto.builder()
                .dishId(1L)
                .quantity(1)
                .build();
        OrderItemRequestDto item2 = OrderItemRequestDto.builder()
                .dishId(2L)
                .quantity(2)
                .build();

        DishResponseDto dish1 = DishResponseDto.builder()
                .id(1L)
                .name("D1")
                .price(new BigDecimal("5.00"))
                .build();
        DishResponseDto dish2 = DishResponseDto.builder()
                .id(2L)
                .name("D2")
                .price(new BigDecimal("10.00"))
                .build();

        List<OrderItem> items = mapper.toEntities(List.of(item1, item2), List.of(dish1, dish2));

        assertEquals(2, items.size());
        assertEquals("D1", items.get(0).getName());
        assertEquals("D2", items.get(1).getName());
    }

    @Test
    void shouldHandleDuplicateDishesInToEntities() {
        OrderItemRequestDto item = OrderItemRequestDto.builder()
                .dishId(1L)
                .quantity(1)
                .build();
        DishResponseDto dishFirst = DishResponseDto.builder()
                .id(1L)
                .name("First")
                .price(new BigDecimal("5.00"))
                .build();
        DishResponseDto dishSecond = DishResponseDto.builder()
                .id(1L)
                .name("Second")
                .price(new BigDecimal("6.00"))
                .build();

        List<OrderItem> items = mapper.toEntities(List.of(item), List.of(dishFirst, dishSecond));

        assertEquals(1, items.size());
        assertEquals("First", items.get(0).getName());
    }

    @Test
    void shouldThrowEntityNotFoundExceptionWhenDishMissing() {
        OrderItemRequestDto item = OrderItemRequestDto.builder().dishId(999L).quantity(1).build();

        List<OrderItemRequestDto> requests = List.of(item);
        List<DishResponseDto> dishes = List.of();

        assertThrows(EntityNotFoundException.class, () -> mapper.toEntities(requests, dishes));
    }

    @Test
    void shouldTestOrderItemDtosMethods() {
        OrderItemRequestDto defaultRequest = new OrderItemRequestDto();
        assertEquals(0L, defaultRequest.dishId());
        assertEquals(0L, defaultRequest.getDishId());
        assertEquals(1, defaultRequest.quantity());
        assertEquals(1, defaultRequest.getQuantity());

        OrderItemRequestDto customRequest = new OrderItemRequestDto(10L, 5);
        assertEquals(10L, customRequest.getDishId());
        assertEquals(5, customRequest.getQuantity());
        assertNotNull(customRequest.toString());
        assertNotEquals(defaultRequest, customRequest);

        OrderItemResponseDto defaultResponse = new OrderItemResponseDto();
        assertNull(defaultResponse.id());
        assertNull(defaultResponse.getId());
        assertNull(defaultResponse.dishId());
        assertNull(defaultResponse.getDishId());
        assertNull(defaultResponse.name());
        assertNull(defaultResponse.getName());
        assertNull(defaultResponse.quantity());
        assertNull(defaultResponse.getQuantity());
        assertNull(defaultResponse.price());
        assertNull(defaultResponse.getPrice());

        OrderItemResponseDto customResponse = new OrderItemResponseDto(
                1L, 2L, "Cake", 3, new BigDecimal("4.50")
        );
        assertEquals(1L, customResponse.getId());
        assertEquals(2L, customResponse.getDishId());
        assertEquals("Cake", customResponse.getName());
        assertEquals(3, customResponse.getQuantity());
        assertEquals(new BigDecimal("4.50"), customResponse.getPrice());
        assertNotNull(customResponse.toString());
        assertNotEquals(defaultResponse, customResponse);
    }
}
