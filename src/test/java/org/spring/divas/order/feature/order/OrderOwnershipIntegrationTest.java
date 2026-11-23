package org.spring.divas.order.feature.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderOwnershipIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Test
    void shouldReturnOrdersOfUserButNotGuestOrderWithSameIdAsUser() {
        Order guestOrder = orderRepository.save(order(null, 1L));
        Long customerId = guestOrder.getId();
        Order firstOwnOrder = orderRepository.save(order(customerId, 2L));
        Order secondOwnOrder = orderRepository.save(order(customerId, 3L));
        orderRepository.save(order(customerId + 1000, 4L));

        List<OrderResponseDto> result = orderService.getAllByUserId(customerId);

        assertThat(result)
                .extracting(OrderResponseDto::id)
                .containsExactlyInAnyOrder(firstOwnOrder.getId(), secondOwnOrder.getId())
                .doesNotContain(guestOrder.getId());
    }

    private static Order order(Long userId, Long tableId) {
        return Order.builder()
                .userId(userId)
                .venueId(1L)
                .tableId(tableId)
                .build();
    }
}
