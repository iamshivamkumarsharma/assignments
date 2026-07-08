package org.nbfc.asessment5.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.dto.DepositRequest;
import org.nbfc.asessment5.dto.TransactionResponse;
import org.nbfc.asessment5.dto.TransferRequest;
import org.nbfc.asessment5.dto.WithdrawRequest;
import org.nbfc.asessment5.exception.AccountNotFoundException;
import org.nbfc.asessment5.exception.InsufficientBalanceException;
import org.nbfc.asessment5.exception.InvalidTransactionException;
import org.nbfc.asessment5.security.JwtAuthenticationFilter;
import org.nbfc.asessment5.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TransactionController — @WebMvcTest")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionResponse depositResponse;

    @BeforeEach
    void setUp() {
        depositResponse = TransactionResponse.builder()
                .id(10L)
                .type("DEPOSIT")
                .amount(new BigDecimal("500.00"))
                .timestamp(LocalDateTime.now())
                .accountId(100L)
                .build();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private DepositRequest depositRequest(Long accountId, String amount) {
        return DepositRequest.builder().accountId(accountId).amount(amount == null ? null : new BigDecimal(amount)).build();
    }

    private WithdrawRequest withdrawRequest(Long accountId, String amount) {
        return WithdrawRequest.builder().accountId(accountId).amount(new BigDecimal(amount)).build();
    }

    private TransferRequest transferRequest(Long source, Long dest, String amount) {
        return TransferRequest.builder()
                .sourceAccountId(source)
                .destinationAccountId(dest)
                .amount(new BigDecimal(amount))
                .build();
    }

    @Nested
    @DisplayName("POST /api/transactions/deposit")
    class Deposit {

        @Test
        @DisplayName("should deposit and return 201")
        void shouldDepositAndReturn201() throws Exception {
            when(transactionService.deposit(any(DepositRequest.class))).thenReturn(depositResponse);

            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(depositRequest(100L, "500.00"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("DEPOSIT"))
                    .andExpect(jsonPath("$.accountId").value(100));

            verify(transactionService).deposit(any(DepositRequest.class));
        }

        @Test
        @DisplayName("should return 400 when the account id is missing")
        void shouldReturn400WhenAccountIdMissing() throws Exception {
            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(depositRequest(null, "500.00"))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("should return 400 when the amount is not positive")
        void shouldReturn400WhenAmountNotPositive() throws Exception {
            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(depositRequest(100L, "0"))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("should return 404 when the account is missing")
        void shouldReturn404WhenAccountMissing() throws Exception {
            when(transactionService.deposit(any(DepositRequest.class)))
                    .thenThrow(new AccountNotFoundException(100L));

            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(depositRequest(100L, "500.00"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/withdraw")
    class Withdraw {

        @Test
        @DisplayName("should withdraw and return 201")
        void shouldWithdrawAndReturn201() throws Exception {
            TransactionResponse withdrawal = TransactionResponse.builder()
                    .id(11L).type("WITHDRAWAL").amount(new BigDecimal("200.00"))
                    .timestamp(LocalDateTime.now()).accountId(100L).build();
            when(transactionService.withdraw(any(WithdrawRequest.class))).thenReturn(withdrawal);

            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(withdrawRequest(100L, "200.00"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("WITHDRAWAL"));
        }

        @Test
        @DisplayName("should return 422 when there is insufficient balance")
        void shouldReturn422WhenInsufficientBalance() throws Exception {
            when(transactionService.withdraw(any(WithdrawRequest.class)))
                    .thenThrow(new InsufficientBalanceException("Insufficient balance"));

            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(withdrawRequest(100L, "999999.00"))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("should return 404 when the account is missing")
        void shouldReturn404WhenAccountMissing() throws Exception {
            when(transactionService.withdraw(any(WithdrawRequest.class)))
                    .thenThrow(new AccountNotFoundException(100L));

            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(withdrawRequest(100L, "50.00"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/transfer")
    class Transfer {

        @Test
        @DisplayName("should transfer and return 200")
        void shouldTransferAndReturn200() throws Exception {
            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(transferRequest(100L, 200L, "300.00"))))
                    .andExpect(status().isOk());

            verify(transactionService).transfer(any(TransferRequest.class));
        }

        @Test
        @DisplayName("should return 400 when source equals destination")
        void shouldReturn400WhenSourceEqualsDestination() throws Exception {
            doThrow(new InvalidTransactionException("Source and destination must differ"))
                    .when(transactionService).transfer(any(TransferRequest.class));

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(transferRequest(100L, 100L, "300.00"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 422 when the source has insufficient funds")
        void shouldReturn422WhenInsufficientFunds() throws Exception {
            doThrow(new InsufficientBalanceException("Insufficient balance"))
                    .when(transactionService).transfer(any(TransferRequest.class));

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(transferRequest(100L, 200L, "999999.00"))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("should return 400 when the source account id is missing")
        void shouldReturn400WhenSourceMissing() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .destinationAccountId(200L).amount(new BigDecimal("100.00")).build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/account/{accountId}")
    class GetTransactions {

        @Test
        @DisplayName("should return the account transactions with 200")
        void shouldReturnTransactionsWith200() throws Exception {
            when(transactionService.getTransactionsByAccount(100L)).thenReturn(List.of(depositResponse));

            mockMvc.perform(get("/api/transactions/account/{accountId}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].type").value("DEPOSIT"));

            verify(transactionService).getTransactionsByAccount(100L);
        }

        @Test
        @DisplayName("should return 404 when the account is missing")
        void shouldReturn404WhenAccountMissing() throws Exception {
            when(transactionService.getTransactionsByAccount(999L))
                    .thenThrow(new AccountNotFoundException(999L));

            mockMvc.perform(get("/api/transactions/account/{accountId}", 999L))
                    .andExpect(status().isNotFound());
        }
    }
}
