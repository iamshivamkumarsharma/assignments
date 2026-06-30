package org.nbfc.productwa.mapper;

import org.nbfc.productwa.dto.UserRegisterRequest;
import org.nbfc.productwa.dto.UserResponse;
import org.nbfc.productwa.model.Role;
import org.nbfc.productwa.model.User;
import org.springframework.stereotype.Component;

/**
 * Manual mapper between {@link User} entity and its DTOs.
 */
@Component
public class UserMapper {

    /**
     * Builds a new {@link User}. Password must already be encoded by the caller.
     */
    public User toEntity(UserRegisterRequest request, String encodedPassword, Role role) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(encodedPassword)
                .mobile(request.getMobile())
                .address(request.getAddress())
                .role(role)
                .enabled(true)
                .build();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .address(user.getAddress())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .build();
    }
}
