package org.spring.divas.order.feature.orderitem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.order.feature.dish.DishResponseDto;
import org.spring.divas.order.feature.dish.DishService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private DishService dishService;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    @Test
    void shouldCreateOrderItem() {
        OrderItemRequestDto request = new OrderItemRequestDto(5L, 2);
        DishResponseDto dish = DishResponseDto.builder().id(5L).build();
        OrderItem item = OrderItem.builder().dishId(5L).build();
        OrderItemResponseDto response = new OrderItemResponseDto();
        when(dishService.getById(5L)).thenReturn(dish);
        when(orderItemMapper.toEntity(request, dish)).thenReturn(item);
        when(orderItemRepository.save(item)).thenReturn(item);
        when(orderItemMapper.toResponse(item)).thenReturn(response);

        assertSame(response, orderItemService.create(request));

        verify(dishService).getById(5L);
        verify(orderItemRepository).save(item);
    }

    @Test
    void shouldCreateAllOrderItemsWithDistinctDishIds() {
        List<OrderItemRequestDto> requests = List.of(
                new OrderItemRequestDto(1L, 1),
                new OrderItemRequestDto(2L, 2),
                new OrderItemRequestDto(1L, 3)
        );
        List<DishResponseDto> dishes = List.of(
                DishResponseDto.builder().id(1L).build(),
                DishResponseDto.builder().id(2L).build()
        );
        List<OrderItem> items = List.of(OrderItem.builder().build(), OrderItem.builder().build());
        List<OrderItemResponseDto> responses = List.of(
                new OrderItemResponseDto(1L, 1L, "First", 1, null),
                new OrderItemResponseDto(2L, 2L, "Second", 2, null)
        );
        when(dishService.getAllByIds(List.of(1L, 2L))).thenReturn(dishes);
        when(orderItemMapper.toEntities(requests, dishes)).thenReturn(items);
        when(orderItemRepository.saveAll(items)).thenReturn(items);
        when(orderItemMapper.toResponse(items.get(0))).thenReturn(responses.get(0));
        when(orderItemMapper.toResponse(items.get(1))).thenReturn(responses.get(1));

        assertEquals(responses, orderItemService.createAll(requests));

        verify(dishService).getAllByIds(List.of(1L, 2L));
        verify(orderItemRepository).saveAll(items);
    }

    @Test
    void shouldReturnAllOrderItems() {
        OrderItem first = OrderItem.builder().build();
        OrderItem second = OrderItem.builder().build();
        OrderItemResponseDto firstResponse = new OrderItemResponseDto();
        OrderItemResponseDto secondResponse = new OrderItemResponseDto();
        when(orderItemRepository.findAll()).thenReturn(List.of(first, second));
        when(orderItemMapper.toResponse(first)).thenReturn(firstResponse);
        when(orderItemMapper.toResponse(second)).thenReturn(secondResponse);

        assertEquals(List.of(firstResponse, secondResponse), orderItemService.getAll());
    }

    @Test
    void shouldReturnOrderItemById() {
        OrderItem item = OrderItem.builder().id(1L).build();
        OrderItemResponseDto response = new OrderItemResponseDto();
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderItemMapper.toResponse(item)).thenReturn(response);

        assertSame(response, orderItemService.getById(1L));
    }

    @Test
    void shouldThrowWhenOrderItemIsMissing() {
        when(orderItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(OrderItemNotFoundException.class, () -> orderItemService.getById(1L));
        verify(orderItemMapper, never()).toResponse(any());
    }

    @Test
    void shouldDeleteExistingOrderItem() {
        when(orderItemRepository.existsById(1L)).thenReturn(true);

        orderItemService.delete(1L);

        verify(orderItemRepository).deleteById(1L);
    }

    @Test
    void shouldNotDeleteMissingOrderItem() {
        when(orderItemRepository.existsById(1L)).thenReturn(false);

        assertThrows(OrderItemNotFoundException.class, () -> orderItemService.delete(1L));
        verify(orderItemRepository, never()).deleteById(1L);
    }
}
