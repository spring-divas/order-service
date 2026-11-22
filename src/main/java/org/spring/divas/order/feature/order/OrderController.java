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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto create(@Valid @RequestBody OrderRequestDto dto, @AuthenticationPrincipal Jwt jwt) {
        return orderService.create(dto, Long.valueOf(jwt.getSubject()));
    }

    @GetMapping
    public List<OrderResponseDto> getAll(@AuthenticationPrincipal Jwt jwt) {
        return isAdmin(jwt) ? orderService.getAll() : orderService.getAllByUserId(userId(jwt));
    }

    @GetMapping("/{id:\\d+}")
    public OrderResponseDto getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwnerOrAdmin(orderService.getById(id), jwt);
        return orderService.getById(id);
    }

    @DeleteMapping("/{id:\\d+}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwnerOrAdmin(orderService.getById(id), jwt);
        orderService.delete(id);
    }

    @PutMapping("/{id:\\d+}")
    public OrderResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequestDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        OrderResponseDto order = orderService.getById(id);
        if (!isAdmin(jwt) && !order.getUserId().equals(userId(jwt))
                && !canManageVenue(jwt, order.getVenueId())) {
            throw new AccessDeniedException("Order is outside the current user's permissions");
        }
        if (dto.getStatus() != null && !canManageVenue(jwt, order.getVenueId())) {
            throw new AccessDeniedException("Only an administrator or venue manager may change order status");
        }
        return orderService.update(id, dto);
    }

    @GetMapping("/exists/dish")
    public boolean hasUserOrderedDish(
            @RequestParam Long dishId, @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.hasUserOrderedDish(Long.valueOf(jwt.getSubject()), dishId);
    }

    @GetMapping("/exists/venue")
    public boolean hasUserBeenToVenue(
            @RequestParam Long venueId, @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.hasUserBeenToVenue(Long.valueOf(jwt.getSubject()), venueId);
    }

    @PatchMapping("/{id:\\d+}/cancel")
    public OrderResponseDto cancel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "User cancelled order") String reason,
            @AuthenticationPrincipal Jwt jwt
    ) {
        requireOwnerOrAdmin(orderService.getById(id), jwt);
        return orderService.cancel(id, reason);
    }

    @PostMapping("/{id:\\d+}/pay")
    public OrderResponseDto pay(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean simulateFailure,
            @AuthenticationPrincipal Jwt jwt
    ) {
        requireOwnerOrAdmin(orderService.getById(id), jwt);
        return orderService.pay(id, simulateFailure);
    }

    private static void requireOwnerOrAdmin(OrderResponseDto order, Jwt jwt) {
        if (!isAdmin(jwt) && !order.getUserId().equals(userId(jwt))) {
            throw new AccessDeniedException("Order does not belong to the current user");
        }
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    private static boolean isAdmin(Jwt jwt) {
        return jwt.getClaimAsStringList("roles").contains("ADMIN");
    }

    private static boolean canManageVenue(Jwt jwt, Long venueId) {
        return isAdmin(jwt) || jwt.getClaimAsStringList("roles").stream()
                .anyMatch(role -> role.equals("SENIOR " + venueId) || role.equals("JUNIOR " + venueId));
    }
}
