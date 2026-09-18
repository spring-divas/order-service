package org.spring.divas.order.feature.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.spring.divas.order.feature.orderItem.OrderItemRequestDto;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDto {

        private Long userId;

        @NotNull
        private Long tableId;

        @NotEmpty
        private List<@Valid OrderItemRequestDto> items;
}