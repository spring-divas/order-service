package org.spring.divas.order.feature.payment;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PaymentClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    public PaymentResponseDto createPayment(Long orderId) {

        log.info("Creating payment for order {} via {}", orderId, paymentServiceUrl);
        RestClient restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();

        log.info("Payment created for order {}", orderId);

        return restClient
                .post()
                .uri("/payment")
                .body(new PaymentRequestDto(orderId))
                .retrieve()
                .body(PaymentResponseDto.class);


    }
}