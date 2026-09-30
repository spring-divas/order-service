package org.spring.divas.order.feature.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.spring.divas.order.feature.orderitem.OrderItemRequestDto;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequestDto {

    private Long userId;

    @NotNull
    private Long tableId;

    @Builder.Default
    private OrderStatus status = OrderStatus.NEW;

    @NotEmpty
    private List<@Valid OrderItemRequestDto> items;
}