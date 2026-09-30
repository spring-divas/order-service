package org.spring.divas.order.feature.venue;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface VenueClient {
    @PostExchange("/api/dish/batch")
    List<DishResponseDto> getDishesByIds(@RequestParam List<Long> ids);

    @GetExchange("/api/dish/{id}")
    DishResponseDto getDishById(@PathVariable Long id);
}
