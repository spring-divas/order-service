package org.spring.divas.order.feature.user;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
}
