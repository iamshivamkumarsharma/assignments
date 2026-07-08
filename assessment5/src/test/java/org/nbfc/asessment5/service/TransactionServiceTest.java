package org.nbfc.asessment5.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nbfc.asessment5.dto.DepositRequest;
import org.nbfc.asessment5.dto.TransactionResponse;
import org.nbfc.asessment5.dto.TransferRequest;
import org.nbfc.asessment5.dto.WithdrawRequest;
import org.nbfc.asessment5.entity.Account;
import org.nbfc.asessment5.entity.AccountType;
import org.nbfc.asessment5.entity.Customer;
import org.nbfc.asessment5.entity.Transaction;
import org.nbfc.asessment5.entity.TransactionType;
import org.nbfc.asessment5.exception.AccountNotFoundException;
import org.nbfc.asessment5.exception.InsufficientBalanceException;
import org.nbfc.asessment5.exception.InvalidTransactionException;
import org.nbfc.asessment5.repository.AccountRepository;
import org.nbfc.asessment5.repository.TransactionRepository;
import org.nbfc.asessment5.service.impl.TransactionServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService — Unit Tests")
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long SOURCE_ID = 100L;
    private static final Long DEST_ID = 200L;
    private static final Long TXN_ID = 5000L;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(CUSTOMER_ID)
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .password("$2a$10$encoded")
                .build();
    }

    // ---------------------------------------------------------------------
    // Helper builders
    // ---------------------------------------------------------------------

    private Account account(Long id, String number, BigDecimal balance) {
        return Account.builder()
                .id(id)
                .accountNumber(number)
                .accountType(AccountType.SAVINGS)
                .balance(balance)
                .customer(customer)
                .build();
    }

    private DepositRequest depositRequest(Long accountId, String amount) {
        return DepositRequest.builder().accountId(accountId).amount(new BigDecimal(amount)).build();
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

    private void stubTransactionSaveAssignsId() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction toSave = invocation.getArgument(0);
            toSave.setId(TXN_ID);
            return toSave;
        });
    }

    @Nested
    @DisplayName("deposit(DepositRequest)")
    class Deposit {

        @Test
        @DisplayName("should deposit successfully and increase the balance")
        void shouldDepositSuccessfullyAndIncreaseBalance() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(acc));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            stubTransactionSaveAssignsId();

            TransactionResponse response = transactionService.deposit(depositRequest(SOURCE_ID, "500.00"));

            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(TransactionType.DEPOSIT.name());
            assertThat(response.getAmount()).isEqualByComparingTo("500.00");
            assertThat(acc.getBalance()).isEqualByComparingTo("1500.00");
            verify(accountRepository).save(acc);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should record a DEPOSIT transaction linked to the account")
        void shouldRecordDepositTransaction() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(acc));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            stubTransactionSaveAssignsId();

            transactionService.deposit(depositRequest(SOURCE_ID, "250.00"));

            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction txn = transactionCaptor.getValue();
            assertThat(txn.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(txn.getAmount()).isEqualByComparingTo("250.00");
            assertThat(txn.getAccount()).isEqualTo(acc);
        }

        @Test
        @DisplayName("should allow depositing one rupee (boundary)")
        void shouldAllowDepositingOneRupee() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("0.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(acc));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            stubTransactionSaveAssignsId();

            transactionService.deposit(depositRequest(SOURCE_ID, "1.00"));

            assertThat(acc.getBalance()).isEqualByComparingTo("1.00");
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when the account is missing")
        void shouldThrowAccountNotFoundWhenAccountMissing() {
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.deposit(depositRequest(SOURCE_ID, "100.00")))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SOURCE_ID));
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @ParameterizedTest(name = "amount={0}")
        @ValueSource(strings = {"0", "0.00", "-0.01", "-100"})
        @DisplayName("should reject a non-positive deposit amount")
        void shouldRejectNonPositiveDepositAmount(String amount) {
            assertThatThrownBy(() -> transactionService.deposit(depositRequest(SOURCE_ID, amount)))
                    .isInstanceOf(InvalidTransactionException.class);
            verify(transactionRepository, never()).save(any(Transaction.class));
            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("withdraw(WithdrawRequest)")
    class Withdraw {

        @Test
        @DisplayName("should withdraw successfully and decrease the balance")
        void shouldWithdrawSuccessfullyAndDecreaseBalance() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(acc));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            stubTransactionSaveAssignsId();

            TransactionResponse response = transactionService.withdraw(withdrawRequest(SOURCE_ID, "400.00"));

            assertThat(response.getType()).isEqualTo(TransactionType.WITHDRAWAL.name());
            assertThat(acc.getBalance()).isEqualByComparingTo("600.00");
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should record a WITHDRAWAL transaction")
        void shouldRecordWithdrawalTransaction() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(acc));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            stubTransactionSaveAssignsId();

            transactionService.withdraw(withdrawRequest(SOURCE_ID, "400.00"));

            verify(transactionRepository).save(transactionCaptor.capture());
            assertThat(transactionCaptor.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL);
        }

        @Test
        @DisplayName("should allow withdrawing the entire balance (boundary)")
        void shouldAllowWithdrawingEntireBalance() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(acc));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            stubTransactionSaveAssignsId();

            transactionService.withdraw(withdrawRequest(SOURCE_ID, "1000.00"));

            assertThat(acc.getBalance()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("should throw InsufficientBalanceException when amount exceeds balance")
        void shouldThrowInsufficientBalanceWhenAmountExceedsBalance() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("100.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(acc));

            assertThatThrownBy(() -> transactionService.withdraw(withdrawRequest(SOURCE_ID, "100.01")))
                    .isInstanceOf(InsufficientBalanceException.class);
            assertThat(acc.getBalance()).isEqualByComparingTo("100.00");
            verify(accountRepository, never()).save(any(Account.class));
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when the account is missing")
        void shouldThrowAccountNotFoundWhenAccountMissing() {
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.withdraw(withdrawRequest(SOURCE_ID, "50.00")))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @ParameterizedTest(name = "amount={0}")
        @ValueSource(strings = {"0", "-0.01", "-500"})
        @DisplayName("should reject a non-positive withdrawal amount")
        void shouldRejectNonPositiveWithdrawAmount(String amount) {
            assertThatThrownBy(() -> transactionService.withdraw(withdrawRequest(SOURCE_ID, amount)))
                    .isInstanceOf(InvalidTransactionException.class);
            verify(transactionRepository, never()).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("transfer(TransferRequest)")
    class Transfer {

        @Test
        @DisplayName("should transfer funds and update both balances")
        void shouldTransferSuccessfullyBetweenAccounts() {
            Account source = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            Account dest = account(DEST_ID, "AC0002", new BigDecimal("200.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
            when(accountRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.transfer(transferRequest(SOURCE_ID, DEST_ID, "300.00"));

            assertThat(source.getBalance()).isEqualByComparingTo("700.00");
            assertThat(dest.getBalance()).isEqualByComparingTo("500.00");
            verify(accountRepository, times(2)).save(any(Account.class));
        }

        @Test
        @DisplayName("should record both a TRANSFER_DEBIT and a TRANSFER_CREDIT transaction")
        void shouldRecordDebitAndCreditTransactions() {
            Account source = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            Account dest = account(DEST_ID, "AC0002", new BigDecimal("200.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
            when(accountRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.transfer(transferRequest(SOURCE_ID, DEST_ID, "300.00"));

            verify(transactionRepository, times(2)).save(transactionCaptor.capture());
            List<Transaction> txns = transactionCaptor.getAllValues();
            assertThat(txns).extracting(Transaction::getType)
                    .containsExactlyInAnyOrder(TransactionType.TRANSFER_DEBIT, TransactionType.TRANSFER_CREDIT);
        }

        @Test
        @DisplayName("should throw InvalidTransactionException when source equals destination")
        void shouldThrowWhenSourceEqualsDestination() {
            assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE_ID, SOURCE_ID, "100.00")))
                    .isInstanceOf(InvalidTransactionException.class);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when the source account is missing")
        void shouldThrowWhenSourceMissing() {
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE_ID, DEST_ID, "100.00")))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when the destination account is missing")
        void shouldThrowWhenDestinationMissing() {
            Account source = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
            when(accountRepository.findById(DEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE_ID, DEST_ID, "100.00")))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("should throw InsufficientBalanceException and not move funds when source lacks funds")
        void shouldThrowInsufficientBalanceAndNotMoveFunds() {
            Account source = account(SOURCE_ID, "AC0001", new BigDecimal("50.00"));
            Account dest = account(DEST_ID, "AC0002", new BigDecimal("200.00"));
            when(accountRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
            when(accountRepository.findById(DEST_ID)).thenReturn(Optional.of(dest));

            assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE_ID, DEST_ID, "100.00")))
                    .isInstanceOf(InsufficientBalanceException.class);
            assertThat(source.getBalance()).isEqualByComparingTo("50.00");
            assertThat(dest.getBalance()).isEqualByComparingTo("200.00");
            verify(accountRepository, never()).save(any(Account.class));
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @ParameterizedTest(name = "amount={0}")
        @ValueSource(strings = {"0", "-0.01", "-1000"})
        @DisplayName("should reject a non-positive transfer amount")
        void shouldRejectNonPositiveTransferAmount(String amount) {
            assertThatThrownBy(() -> transactionService.transfer(transferRequest(SOURCE_ID, DEST_ID, amount)))
                    .isInstanceOf(InvalidTransactionException.class);
            verify(transactionRepository, never()).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("getTransactionsByAccount(Long)")
    class GetTransactionsByAccount {

        @Test
        @DisplayName("should return the transactions for an existing account")
        void shouldReturnTransactionsForAccount() {
            Account acc = account(SOURCE_ID, "AC0001", new BigDecimal("1000.00"));
            Transaction t1 = Transaction.builder().id(1L).type(TransactionType.DEPOSIT)
                    .amount(new BigDecimal("100.00")).account(acc).build();
            Transaction t2 = Transaction.builder().id(2L).type(TransactionType.WITHDRAWAL)
                    .amount(new BigDecimal("40.00")).account(acc).build();
            when(accountRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(transactionRepository.findByAccountId(SOURCE_ID)).thenReturn(List.of(t1, t2));

            List<TransactionResponse> result = transactionService.getTransactionsByAccount(SOURCE_ID);

            assertThat(result).hasSize(2)
                    .extracting(TransactionResponse::getType)
                    .containsExactly(TransactionType.DEPOSIT.name(), TransactionType.WITHDRAWAL.name());
        }

        @Test
        @DisplayName("should return an empty list when the account has no transactions")
        void shouldReturnEmptyListWhenNoTransactions() {
            when(accountRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(transactionRepository.findByAccountId(SOURCE_ID)).thenReturn(Collections.emptyList());

            assertThat(transactionService.getTransactionsByAccount(SOURCE_ID)).isEmpty();
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when the account does not exist")
        void shouldThrowAccountNotFoundWhenAccountMissing() {
            when(accountRepository.existsById(SOURCE_ID)).thenReturn(false);

            assertThatThrownBy(() -> transactionService.getTransactionsByAccount(SOURCE_ID))
                    .isInstanceOf(AccountNotFoundException.class);
            verify(transactionRepository, never()).findByAccountId(any());
        }
    }
}
