package org.nbfc.productwa.service.impl;

import lombok.RequiredArgsConstructor;
import org.nbfc.productwa.dto.PageResponse;
import org.nbfc.productwa.dto.UserResponse;
import org.nbfc.productwa.dto.UserUpdateRequest;
import org.nbfc.productwa.exception.ResourceNotFoundException;
import org.nbfc.productwa.mapper.UserMapper;
import org.nbfc.productwa.model.User;
import org.nbfc.productwa.repository.UserRepository;
import org.nbfc.productwa.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return userMapper.toResponse(getByEmail(email));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UserUpdateRequest request) {
        User user = getByEmail(email);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobile(request.getMobile());
        user.setAddress(request.getAddress());
        User saved = userRepository.save(user);
        log.info("Updated profile for user {}", email);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
        log.info("Deleted user id={}", id);
    }

    private User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
