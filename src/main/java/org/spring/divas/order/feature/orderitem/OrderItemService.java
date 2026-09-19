package org.spring.divas.order.feature.orderitem;

import java.util.List;

public interface OrderItemService {

    OrderItemResponseDto create(OrderItemRequestDto dto);

    List<OrderItemResponseDto> getAll();

    OrderItemResponseDto getById(Long id);

    void delete(Long id);
}