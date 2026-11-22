package org.spring.divas.order.feature.user;

import lombok.Builder;

@Builder
public record UserResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role
) {
    public UserResponseDto() {
        this(null, null, null, null, null);
    }

    public Long getId() {
        return id();
    }

    public String getFirstName() {
        return firstName();
    }

    public String getLastName() {
        return lastName();
    }

    public String getEmail() {
        return email();
    }

    public UserRole getRole() {
        return role();
    }
}
