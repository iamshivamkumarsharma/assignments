package org.nbfc.asessment5.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.dto.AccountRequest;
import org.nbfc.asessment5.dto.AccountResponse;
import org.nbfc.asessment5.exception.AccountNotFoundException;
import org.nbfc.asessment5.exception.CustomerNotFoundException;
import org.nbfc.asessment5.security.JwtAuthenticationFilter;
import org.nbfc.asessment5.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AccountController — @WebMvcTest")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private AccountRequest validRequest;
    private AccountResponse response;

    @BeforeEach
    void setUp() {
        validRequest = AccountRequest.builder()
                .customerId(1L)
                .accountType("SAVINGS")
                .openingBalance(new BigDecimal("1000.00"))
                .build();
        response = AccountResponse.builder()
                .id(100L)
                .accountNumber("AC0000001")
                .accountType("SAVINGS")
                .balance(new BigDecimal("1000.00"))
                .customerId(1L)
                .build();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Nested
    @DisplayName("POST /api/accounts")
    class CreateAccount {

        @Test
        @DisplayName("should create an account and return 201")
        void shouldCreateAccountAndReturn201() throws Exception {
            when(accountService.createAccount(any(AccountRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.accountNumber").value("AC0000001"))
                    .andExpect(jsonPath("$.accountType").value("SAVINGS"));

            verify(accountService).createAccount(any(AccountRequest.class));
        }

        @Test
        @DisplayName("should return 400 when the customer id is missing")
        void shouldReturn400WhenCustomerIdMissing() throws Exception {
            validRequest.setCustomerId(null);

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("should return 400 when the account type is blank")
        void shouldReturn400WhenAccountTypeBlank() throws Exception {
            validRequest.setAccountType("");

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("should return 400 when the opening balance is negative")
        void shouldReturn400WhenOpeningBalanceNegative() throws Exception {
            validRequest.setOpeningBalance(new BigDecimal("-1.00"));

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("should return 404 when the owning customer does not exist")
        void shouldReturn404WhenCustomerMissing() throws Exception {
            when(accountService.createAccount(any(AccountRequest.class)))
                    .thenThrow(new CustomerNotFoundException(1L));

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when the account type is invalid")
        void shouldReturn400WhenAccountTypeInvalid() throws Exception {
            when(accountService.createAccount(any(AccountRequest.class)))
                    .thenThrow(new IllegalArgumentException("Invalid account type: GOLD"));

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/accounts")
    class GetAccounts {

        @Test
        @DisplayName("should return an account by id with 200")
        void shouldGetAccountByIdAndReturn200() throws Exception {
            when(accountService.getAccountById(100L)).thenReturn(response);

            mockMvc.perform(get("/api/accounts/{id}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountNumber").value("AC0000001"));

            verify(accountService).getAccountById(100L);
        }

        @Test
        @DisplayName("should return 404 when the account is not found by id")
        void shouldReturn404WhenAccountNotFoundById() throws Exception {
            when(accountService.getAccountById(999L)).thenThrow(new AccountNotFoundException(999L));

            mockMvc.perform(get("/api/accounts/{id}", 999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return all accounts with 200")
        void shouldReturnAllAccounts() throws Exception {
            when(accountService.getAllAccounts()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(100));
        }

        @Test
        @DisplayName("should return an account by number with 200")
        void shouldGetAccountByNumberAndReturn200() throws Exception {
            when(accountService.getAccountByNumber("AC0000001")).thenReturn(response);

            mockMvc.perform(get("/api/accounts/number/{accountNumber}", "AC0000001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100));

            verify(accountService).getAccountByNumber("AC0000001");
        }

        @Test
        @DisplayName("should return 404 when the account number is unknown")
        void shouldReturn404WhenAccountNumberUnknown() throws Exception {
            when(accountService.getAccountByNumber("AC9999999"))
                    .thenThrow(new AccountNotFoundException("AC9999999"));

            mockMvc.perform(get("/api/accounts/number/{accountNumber}", "AC9999999"))
                    .andExpect(status().isNotFound());
        }
    }
}
