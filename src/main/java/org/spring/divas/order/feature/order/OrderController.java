package org.spring.divas.order.feature.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
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

    @PatchMapping("/{id:\\d+}/cancel")
    public OrderResponseDto cancel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "User cancelled order") String reason
    ) {
        return orderService.cancel(id, reason);
    }

    @PostMapping("/{id:\\d+}/pay")
    public OrderResponseDto pay(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean simulateFailure
    ) {
        return orderService.pay(id, simulateFailure);
    }
}