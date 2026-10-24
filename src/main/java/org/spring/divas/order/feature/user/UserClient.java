package org.spring.divas.order.feature.user;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface UserClient {
    @GetExchange("/user/{id}")
    UserResponseDto findById(@PathVariable Long id);
}
