package org.spring.divas.order.feature.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDto {

    private Long id;

    private Long orderId;

    private String status;

    private LocalDateTime createdAt;
}
