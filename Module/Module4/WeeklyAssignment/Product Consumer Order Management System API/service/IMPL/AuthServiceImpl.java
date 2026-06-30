package org.nbfc.productwa.service.impl;

import lombok.RequiredArgsConstructor;
import org.nbfc.productwa.dto.LoginRequest;
import org.nbfc.productwa.dto.LoginResponse;
import org.nbfc.productwa.dto.UserRegisterRequest;
import org.nbfc.productwa.dto.UserResponse;
import org.nbfc.productwa.exception.DuplicateResourceException;
import org.nbfc.productwa.exception.ResourceNotFoundException;
import org.nbfc.productwa.mapper.UserMapper;
import org.nbfc.productwa.model.Role;
import org.nbfc.productwa.model.User;
import org.nbfc.productwa.repository.UserRepository;
import org.nbfc.productwa.security.JwtTokenProvider;
import org.nbfc.productwa.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        String encoded = passwordEncoder.encode(request.getPassword());
        User user = userMapper.toEntity(request, encoded, Role.ROLE_USER);
        User saved = userRepository.save(user);
        log.info("Registered new user with email {}", saved.getEmail());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getEmail()));

        String token = tokenProvider.generateToken(user);
        log.info("User {} logged in successfully", user.getEmail());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
