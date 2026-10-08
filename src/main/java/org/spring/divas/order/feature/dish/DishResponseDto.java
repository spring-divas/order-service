package org.spring.divas.order.feature.dish;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DishResponseDto {
    private Long id;
    private Long venueId;
    private String name;
    private BigDecimal price;
}
