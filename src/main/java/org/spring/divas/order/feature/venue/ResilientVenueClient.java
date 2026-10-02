package org.spring.divas.order.feature.venue;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ResilientVenueClient {

    private final VenueClient venueClient;

    @Bulkhead(name = "venueClient")
    @CircuitBreaker(name = "venueClient")
    @Retry(name = "venueClient")
    public List<DishResponseDto> getDishesByIds(List<Long> ids) {
        return venueClient.getDishesByIds(ids);
    }

    @Bulkhead(name = "venueClient")
    @CircuitBreaker(name = "venueClient")
    @Retry(name = "venueClient")
    public DishResponseDto getDishById(Long id) {
        return venueClient.getDishById(id);
    }
}
