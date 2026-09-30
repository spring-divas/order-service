package org.spring.divas.order.feature.venue;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class ResilientVenueClient {

    private final VenueClient venueClient;

    @Bulkhead(name = "venueClient")
    @CircuitBreaker(name = "venueClient", fallbackMethod = "getDishesByIdsFallback")
    @Retry(name = "venueClient")
    public List<DishResponseDto> getDishesByIds(List<Long> ids) {
        return venueClient.getDishesByIds(ids);
    }

    @Bulkhead(name = "venueClient")
    @CircuitBreaker(name = "venueClient", fallbackMethod = "getDishByIdFallback")
    @Retry(name = "venueClient")
    public DishResponseDto getDishById(Long id) {
        return venueClient.getDishById(id);
    }

    public List<DishResponseDto> getDishesByIdsFallback(List<Long> ids, Throwable ex) {
        ex.printStackTrace();
        return ids.stream()
                .map(id -> new DishResponseDto(id, "Temporally inaccessible", BigDecimal.ZERO))
                .toList();
    }

    public DishResponseDto getDishByIdFallback(Long id, Throwable ex) {
        return new DishResponseDto(id, "Temporally inaccessible", BigDecimal.ZERO);
    }
}
