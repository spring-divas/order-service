package org.spring.divas.order.feature.user;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResilientUserClient {
    private final UserClient userClient;

    @Bulkhead(name = "userClient")
    @CircuitBreaker(name = "userClient")
    @Retry(name = "userClient")
    public UserResponseDto findById(Long id) {
        return userClient.findById(id);
    }
}
