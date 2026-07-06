package org.nbfc.loanemicalculator.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nbfc.loanemicalculator.dto.CustomerResponse;
import org.nbfc.loanemicalculator.dto.LoginRequest;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.exception.CustomerNotFoundException;
import org.nbfc.loanemicalculator.exception.DuplicateEmailException;
import org.nbfc.loanemicalculator.mapper.EntityMapper;
import org.nbfc.loanemicalculator.repository.CustomerRepository;
import org.nbfc.loanemicalculator.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Customer registration and JWT login")
public class AuthController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer", description = "Creates a customer; the password is BCrypt-hashed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody Customer customer) {
        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email " + customer.getEmail() + " is already registered.");
        }
        // Registration always creates a standard USER; roles are never client-assignable
        // (prevents privilege escalation and guards against the field default being
        // overwritten with null during JSON binding).
        customer.setRole("USER");
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        Customer saved = customerRepository.save(customer);
        log.info("Registered customer {} in branch {}", saved.getEmail(), saved.getBranchName());
        return ResponseEntity.status(HttpStatus.CREATED).body(EntityMapper.toCustomerResponse(saved));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain a JWT", description = "Returns a Bearer token for valid credentials.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomerNotFoundException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            log.warn("Failed login attempt for {}", request.getEmail());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String token = jwtUtil.generateToken(customer.getEmail(), customer.getRole());
        log.info("Issued JWT for {}", customer.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
