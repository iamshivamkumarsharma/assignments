package org.nbfc.productwa.service;

import org.nbfc.productwa.dto.PageResponse;
import org.nbfc.productwa.dto.UserResponse;
import org.nbfc.productwa.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getProfile(String email);

    UserResponse updateProfile(String email, UserUpdateRequest request);

    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    void deleteUser(Long id);
}
