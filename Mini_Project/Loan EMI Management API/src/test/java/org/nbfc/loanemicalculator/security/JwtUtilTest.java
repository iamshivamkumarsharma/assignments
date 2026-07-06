package org.nbfc.loanemicalculator.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    void generatesAndParsesToken() {
        String token = jwtUtil.generateToken("user@bank.com", "ADMIN");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@bank.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalse_forGarbage() {
        assertThat(jwtUtil.validateToken("not-a-real-token")).isFalse();
    }

    @Test
    void validateToken_returnsFalse_whenSignatureTampered() {
        String token = jwtUtil.generateToken("user@bank.com", "USER");
        char last = token.charAt(token.length() - 1);
        char replacement = last == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }
}
