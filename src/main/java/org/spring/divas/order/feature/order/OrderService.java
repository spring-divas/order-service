package org.spring.divas.order.feature.order;

import java.util.List;

public interface OrderService {

    OrderResponseDto create(OrderRequestDto dto, Long userId);
    default OrderResponseDto create(OrderRequestDto dto) {
        return create(dto, dto.getUserId());
    }

    List<OrderResponseDto> getAll();
    List<OrderResponseDto> getAllByUserId(Long userId);

    OrderResponseDto getById(Long id);

    void delete(Long id);

    OrderResponseDto update(Long id, OrderRequestDto dto);

    boolean hasUserOrderedDish(Long userId, Long dishId);

    boolean hasUserBeenToVenue(Long userId, Long venueId);

    OrderResponseDto cancel(Long id);

    OrderResponseDto cancel(Long id, String reason);

    OrderResponseDto pay(Long id, boolean simulateFailure);

    boolean hasGuestAccess(Long id, String accessToken);
}
