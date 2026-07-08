package org.nbfc.asessment5.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.dto.CustomerRequest;
import org.nbfc.asessment5.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security — configuration & JWT protection")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("should permit unauthenticated access to /api/auth/register")
        void shouldPermitRegister() throws Exception {
            CustomerRequest request = CustomerRequest.builder()
                    .name("John Doe").email("john.doe@example.com")
                    .phone("9876543210").password("Secret123").build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
        }

        @Test
        @DisplayName("should permit unauthenticated access to /api/auth/login")
        void shouldPermitLogin() throws Exception {
            LoginRequest request = LoginRequest.builder()
                    .email("john.doe@example.com").password("Secret123").build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
        }
    }

    @Nested
    @DisplayName("secured endpoints")
    class SecuredEndpoints {

        @Test
        @DisplayName("should reject GET /api/customers without a token (401)")
        void shouldRejectCustomersWithoutToken() throws Exception {
            mockMvc.perform(get("/api/customers"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject GET /api/accounts without a token (401)")
        void shouldRejectAccountsWithoutToken() throws Exception {
            mockMvc.perform(get("/api/accounts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject GET /api/transactions without a token (401)")
        void shouldRejectTransactionsWithoutToken() throws Exception {
            mockMvc.perform(get("/api/transactions/account/{id}", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject a request carrying an invalid token (401)")
        void shouldRejectInvalidToken() throws Exception {
            mockMvc.perform(get("/api/customers")
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
