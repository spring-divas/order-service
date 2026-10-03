package org.spring.divas.order.feature.order;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.spring.divas.order.feature.orderitem.OrderItemResponseDto;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {

    private Long id;

    private Long userId;

    private Long venueId;

    private Long tableId;

    private OrderStatus status;

    private LocalDateTime createdAt;

    private List<OrderItemResponseDto> items;
}