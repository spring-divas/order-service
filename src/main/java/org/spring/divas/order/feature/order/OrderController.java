package org.spring.divas.order.feature.order;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto create(@Valid @RequestBody OrderRequestDto dto) {
        return orderService.create(dto);
    }

    @GetMapping
    public List<OrderResponseDto> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/{id:\\d+}")
    public OrderResponseDto getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @DeleteMapping("/{id:\\d+}")
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }

    @PutMapping("/{id:\\d+}")
    public OrderResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequestDto dto
    ) {
        return orderService.update(id, dto);
    }

    @GetMapping("/exists/dish")
    public boolean hasUserOrderedDish(
            @RequestParam Long userId, @RequestParam Long dishId
    ) {
        return orderService.hasUserOrderedDish(userId, dishId);
    }

    @GetMapping("/exists/venue")
    public boolean hasUserBeenToVenue(
            @RequestParam Long userId, @RequestParam Long venueId
    ) {
        return orderService.hasUserBeenToVenue(userId, venueId);
    }
}