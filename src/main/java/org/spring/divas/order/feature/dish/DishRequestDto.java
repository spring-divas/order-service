package org.spring.divas.order.feature.dish;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DishRequestDto {
    @NotNull
    private Long dishId;

    @NotNull
    @NotBlank
    private String name;

    @NotNull
    private BigDecimal price;
}
