package org.nbfc.loanemicalculator.security;

import lombok.RequiredArgsConstructor;
import org.nbfc.loanemicalculator.repository.CustomerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final CustomerRepository customerRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> customerRepository.findByEmail(email)
                .map(c -> User.withUsername(c.getEmail())
                        .password(c.getPassword())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + c.getRole())))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found: " + email));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
