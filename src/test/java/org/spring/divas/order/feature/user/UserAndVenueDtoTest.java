package org.spring.divas.order.feature.user;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserAndVenueDtoTest {

    @Test
    void shouldTestUserResponseDtoMethods() {
        UserResponseDto defaultDto = new UserResponseDto();
        assertNull(defaultDto.id());
        assertNull(defaultDto.getId());
        assertNull(defaultDto.firstName());
        assertNull(defaultDto.getFirstName());
        assertNull(defaultDto.lastName());
        assertNull(defaultDto.getLastName());
        assertNull(defaultDto.email());
        assertNull(defaultDto.getEmail());
        assertNull(defaultDto.role());
        assertNull(defaultDto.getRole());

        UserResponseDto customDto = new UserResponseDto(
                1L, "Alice", "Smith", "alice@test.com", UserRole.CUSTOMER
        );
        assertEquals(1L, customDto.id());
        assertEquals(1L, customDto.getId());
        assertEquals("Alice", customDto.firstName());
        assertEquals("Alice", customDto.getFirstName());
        assertEquals("Smith", customDto.lastName());
        assertEquals("Smith", customDto.getLastName());
        assertEquals("alice@test.com", customDto.email());
        assertEquals("alice@test.com", customDto.getEmail());
        assertEquals(UserRole.CUSTOMER, customDto.role());
        assertEquals(UserRole.CUSTOMER, customDto.getRole());

        UserResponseDto builtDto = UserResponseDto.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@test.com")
                .role(UserRole.CUSTOMER)
                .build();
        assertEquals(customDto, builtDto);
        assertEquals(customDto.hashCode(), builtDto.hashCode());
        assertNotNull(customDto.toString());
        assertNotEquals(defaultDto, customDto);
    }

    @Test
    void shouldTestVenueDishResponseDtoMethods() {
        org.spring.divas.order.feature.venue.DishResponseDto dto =
                new org.spring.divas.order.feature.venue.DishResponseDto(
                        10L, "Steak", new BigDecimal("25.00")
                );

        assertEquals(10L, dto.id());
        assertEquals("Steak", dto.name());
        assertEquals(new BigDecimal("25.00"), dto.price());
        assertNotNull(dto.toString());

        org.spring.divas.order.feature.venue.DishResponseDto sameDto =
                new org.spring.divas.order.feature.venue.DishResponseDto(
                        10L, "Steak", new BigDecimal("25.00")
                );
        assertEquals(dto, sameDto);
        assertEquals(dto.hashCode(), sameDto.hashCode());
    }
}
