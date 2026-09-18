package org.spring.divas.order.feature.order;

import java.util.List;

public interface OrderService {

    OrderResponseDto create(OrderRequestDto dto);

    List<OrderResponseDto> getAll();

    OrderResponseDto getById(Long id);

    void delete(Long id);

    OrderResponseDto update(Long id, OrderRequestDto dto);
}