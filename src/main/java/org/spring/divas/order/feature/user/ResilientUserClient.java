package org.spring.divas.order.feature.user;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientUserClient {
    private final UserClient userClient;

    @Bulkhead(name = "userClient")
    @CircuitBreaker(name = "userClient", fallbackMethod = "findByIdFallback")
    @Retry(name = "userClient")
    public UserResponseDto findById(Long id) {
        return userClient.findById(id);
    }

    private UserResponseDto findByIdFallback(Long id, Throwable throwable) {
        log.warn("User fallback activated for userId={}: {}", id, throwable.getMessage());
        return new UserResponseDto(id, "", "", "", null);
    }
}
