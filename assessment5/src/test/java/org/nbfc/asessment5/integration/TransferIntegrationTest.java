package org.nbfc.asessment5.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.dto.AccountRequest;
import org.nbfc.asessment5.dto.AccountResponse;
import org.nbfc.asessment5.dto.AuthResponse;
import org.nbfc.asessment5.dto.CustomerRequest;
import org.nbfc.asessment5.dto.CustomerResponse;
import org.nbfc.asessment5.dto.LoginRequest;
import org.nbfc.asessment5.dto.TransferRequest;
import org.nbfc.asessment5.entity.Account;
import org.nbfc.asessment5.repository.AccountRepository;
import org.nbfc.asessment5.repository.CustomerRepository;
import org.nbfc.asessment5.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Transfer flow — Integration (@SpringBootTest)")
class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "Secret123";

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    private String bootstrapAuthenticatedCustomerToken() throws Exception {
        CustomerRequest register = CustomerRequest.builder()
                .name("Owner").email(EMAIL).phone("9876543210").password(PASSWORD).build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder().email(EMAIL).password(PASSWORD).build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return "Bearer " + auth.getToken();
    }

    private Long customerId() {
        return customerRepository.findByEmail(EMAIL).orElseThrow().getId();
    }

    private AccountResponse createAccount(String token, Long customerId, String balance) throws Exception {
        AccountRequest request = AccountRequest.builder()
                .customerId(customerId).accountType("SAVINGS").openingBalance(new BigDecimal(balance)).build();
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AccountResponse.class);
    }

    private void transfer(String token, Long source, Long dest, String amount, int expectedStatus) throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(source).destinationAccountId(dest).amount(new BigDecimal(amount)).build();
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("should move funds atomically and create debit + credit transactions")
    void shouldTransferFundsSuccessfully() throws Exception {
        String token = bootstrapAuthenticatedCustomerToken();
        Long customerId = customerId();
        AccountResponse source = createAccount(token, customerId, "1000.00");
        AccountResponse dest = createAccount(token, customerId, "200.00");

        transfer(token, source.getId(), dest.getId(), "300.00", 200);

        Account reloadedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account reloadedDest = accountRepository.findById(dest.getId()).orElseThrow();
        assertThat(reloadedSource.getBalance()).isEqualByComparingTo("700.00");
        assertThat(reloadedDest.getBalance()).isEqualByComparingTo("500.00");

        assertThat(transactionRepository.findByAccountId(source.getId())).isNotEmpty();
        assertThat(transactionRepository.findByAccountId(dest.getId())).isNotEmpty();

        mockMvc.perform(get("/api/transactions/account/{id}", source.getId()).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should roll back and leave balances unchanged when funds are insufficient")
    void shouldRollBackOnInsufficientFunds() throws Exception {
        String token = bootstrapAuthenticatedCustomerToken();
        Long customerId = customerId();
        AccountResponse source = createAccount(token, customerId, "100.00");
        AccountResponse dest = createAccount(token, customerId, "50.00");

        transfer(token, source.getId(), dest.getId(), "999999.00", 422);

        Account reloadedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account reloadedDest = accountRepository.findById(dest.getId()).orElseThrow();
        assertThat(reloadedSource.getBalance()).isEqualByComparingTo("100.00");
        assertThat(reloadedDest.getBalance()).isEqualByComparingTo("50.00");
        assertThat(transactionRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("should reject a transfer to the same account with 400")
    void shouldRejectTransferToSameAccount() throws Exception {
        String token = bootstrapAuthenticatedCustomerToken();
        Long customerId = customerId();
        AccountResponse source = createAccount(token, customerId, "500.00");

        transfer(token, source.getId(), source.getId(), "100.00", 400);

        Account reloaded = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("should reject transfers from unauthenticated callers with 401")
    void shouldRejectUnauthenticatedTransfer() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(1L).destinationAccountId(2L).amount(new BigDecimal("100.00")).build();

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
