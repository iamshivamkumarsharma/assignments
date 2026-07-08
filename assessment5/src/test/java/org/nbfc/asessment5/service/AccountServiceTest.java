package org.nbfc.asessment5.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nbfc.asessment5.dto.AccountRequest;
import org.nbfc.asessment5.dto.AccountResponse;
import org.nbfc.asessment5.entity.Account;
import org.nbfc.asessment5.entity.AccountType;
import org.nbfc.asessment5.entity.Customer;
import org.nbfc.asessment5.exception.AccountNotFoundException;
import org.nbfc.asessment5.exception.CustomerNotFoundException;
import org.nbfc.asessment5.repository.AccountRepository;
import org.nbfc.asessment5.repository.CustomerRepository;
import org.nbfc.asessment5.service.impl.AccountServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService — Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long UNKNOWN_CUSTOMER_ID = 999L;
    private static final Long ACCOUNT_ID = 100L;
    private static final String ACCOUNT_NUMBER = "ACC0000001";
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = aCustomer(CUSTOMER_ID).build();
    }

    // ---------------------------------------------------------------------
    // Helper builders / stubs (avoid duplicate setup code)
    // ---------------------------------------------------------------------

    private Customer.CustomerBuilder aCustomer(Long id) {
        return Customer.builder()
                .id(id)
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .password("$2a$10$encodedHash");
    }

    private Account.AccountBuilder anAccount(Long id, String number, AccountType type,
                                             BigDecimal balance, Customer owner) {
        return Account.builder()
                .id(id)
                .accountNumber(number)
                .accountType(type)
                .balance(balance)
                .customer(owner);
    }

    private AccountRequest.AccountRequestBuilder anAccountRequest() {
        return AccountRequest.builder()
                .customerId(CUSTOMER_ID)
                .accountType("SAVINGS")
                .openingBalance(OPENING_BALANCE);
    }

    private void givenCustomerExists() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
    }

    private void givenSaveAssignsId() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account toSave = invocation.getArgument(0);
            toSave.setId(ACCOUNT_ID);
            return toSave;
        });
    }

    @Nested
    @DisplayName("createAccount(AccountRequest)")
    class CreateAccount {

        @Test
        @DisplayName("should create a SAVINGS account successfully")
        void shouldCreateSavingsAccountSuccessfully() {
            // Arrange
            givenCustomerExists();
            givenSaveAssignsId();
            AccountRequest request = anAccountRequest().accountType("SAVINGS").build();

            // Act
            AccountResponse response = accountService.createAccount(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.getAccountType()).isEqualTo("SAVINGS");
            assertThat(response.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(response.getBalance()).isEqualByComparingTo(OPENING_BALANCE);
            verify(customerRepository).findById(CUSTOMER_ID);
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should create a CURRENT account successfully")
        void shouldCreateCurrentAccountSuccessfully() {
            // Arrange
            givenCustomerExists();
            givenSaveAssignsId();
            AccountRequest request = anAccountRequest().accountType("CURRENT").build();

            // Act
            AccountResponse response = accountService.createAccount(request);

            // Assert
            assertThat(response.getAccountType()).isEqualTo("CURRENT");
            verify(accountRepository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getAccountType()).isEqualTo(AccountType.CURRENT);
        }

        @Test
        @DisplayName("should allow a zero opening balance (boundary)")
        void shouldAllowZeroOpeningBalance() {
            // Arrange
            givenCustomerExists();
            givenSaveAssignsId();
            AccountRequest request = anAccountRequest().openingBalance(BigDecimal.ZERO).build();

            // Act
            AccountResponse response = accountService.createAccount(request);

            // Assert
            assertThat(response.getBalance()).isEqualByComparingTo("0");
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should allow a very large opening balance (edge)")
        void shouldAllowVeryLargeOpeningBalance() {
            // Arrange
            BigDecimal huge = new BigDecimal("99999999999999.99");
            givenCustomerExists();
            givenSaveAssignsId();
            AccountRequest request = anAccountRequest().openingBalance(huge).build();

            // Act
            AccountResponse response = accountService.createAccount(request);

            // Assert
            assertThat(response.getBalance()).isEqualByComparingTo(huge);
        }

        @Test
        @DisplayName("should generate a non-blank account number when creating")
        void shouldGenerateNonBlankAccountNumber() {
            // Arrange
            givenCustomerExists();
            givenSaveAssignsId();

            // Act
            AccountResponse response = accountService.createAccount(anAccountRequest().build());

            // Assert
            verify(accountRepository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getAccountNumber()).isNotBlank();
            assertThat(response.getAccountNumber()).isNotBlank();
        }

        @Test
        @DisplayName("should persist an account linked to the resolved customer with request values")
        void shouldPersistAccountLinkedToCustomerWithRequestValues() {
            // Arrange
            givenCustomerExists();
            givenSaveAssignsId();
            AccountRequest request = anAccountRequest().accountType("SAVINGS").build();

            // Act
            accountService.createAccount(request);

            // Assert
            verify(accountRepository).save(accountCaptor.capture());
            Account persisted = accountCaptor.getValue();
            assertThat(persisted.getCustomer()).isNotNull();
            assertThat(persisted.getCustomer().getId()).isEqualTo(CUSTOMER_ID);
            assertThat(persisted.getAccountType()).isEqualTo(AccountType.SAVINGS);
            assertThat(persisted.getBalance()).isEqualByComparingTo(OPENING_BALANCE);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when the customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
            // Arrange
            when(customerRepository.findById(UNKNOWN_CUSTOMER_ID)).thenReturn(Optional.empty());
            AccountRequest request = anAccountRequest().customerId(UNKNOWN_CUSTOMER_ID).build();

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(request))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(String.valueOf(UNKNOWN_CUSTOMER_ID));
            verify(accountRepository, never()).save(any(Account.class));
            verifyNoMoreInteractions(accountRepository);
        }

        @ParameterizedTest(name = "accountType=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "GOLD", "PREMIUM", "FIXED", "123", "current-account"})
        @DisplayName("should reject null, blank, or invalid account types with IllegalArgumentException")
        void shouldRejectInvalidNullOrBlankAccountType(String invalidType) {
            // Arrange
            givenCustomerExists();
            AccountRequest request = anAccountRequest().accountType(invalidType).build();

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(request))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(accountRepository, never()).save(any(Account.class));
        }

        @ParameterizedTest(name = "openingBalance={0}")
        @ValueSource(strings = {"-0.01", "-1", "-1000.50", "-99999999"})
        @DisplayName("should reject negative opening balances with IllegalArgumentException")
        void shouldRejectNegativeOpeningBalance(String negativeBalance) {
            // Arrange
            givenCustomerExists();
            AccountRequest request = anAccountRequest()
                    .openingBalance(new BigDecimal(negativeBalance))
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(request))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("should validate customer existence before saving the account")
        void shouldValidateCustomerExistenceBeforeSaving() {
            // Arrange
            givenCustomerExists();
            givenSaveAssignsId();

            // Act
            accountService.createAccount(anAccountRequest().build());

            // Assert
            InOrder inOrder = inOrder(customerRepository, accountRepository);
            inOrder.verify(customerRepository).findById(CUSTOMER_ID);
            inOrder.verify(accountRepository).save(any(Account.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("getAccountById(Long)")
    class GetAccountById {

        @Test
        @DisplayName("should return the account when it exists")
        void shouldReturnAccountWhenFound() {
            // Arrange
            Account account = anAccount(ACCOUNT_ID, ACCOUNT_NUMBER, AccountType.SAVINGS,
                    OPENING_BALANCE, customer).build();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            // Act
            AccountResponse response = accountService.getAccountById(ACCOUNT_ID);

            // Assert
            assertThat(response.getId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(response.getAccountType()).isEqualTo("SAVINGS");
            assertThat(response.getBalance()).isEqualByComparingTo(OPENING_BALANCE);
            assertThat(response.getCustomerId()).isEqualTo(CUSTOMER_ID);
            verify(accountRepository).findById(ACCOUNT_ID);
            verifyNoInteractions(customerRepository);
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when the account does not exist")
        void shouldThrowAccountNotFoundExceptionWhenNotFound() {
            // Arrange
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getAccountById(ACCOUNT_ID))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ACCOUNT_ID));
            verify(accountRepository).findById(ACCOUNT_ID);
            verifyNoInteractions(customerRepository);
        }
    }

    @Nested
    @DisplayName("getAllAccounts()")
    class GetAllAccounts {

        @Test
        @DisplayName("should return every account mapped to a response")
        void shouldReturnAllAccounts() {
            // Arrange
            Account first = anAccount(ACCOUNT_ID, ACCOUNT_NUMBER, AccountType.SAVINGS,
                    OPENING_BALANCE, customer).build();
            Account second = anAccount(101L, "ACC0000002", AccountType.CURRENT,
                    new BigDecimal("500.00"), customer).build();
            when(accountRepository.findAll()).thenReturn(List.of(first, second));

            // Act
            List<AccountResponse> result = accountService.getAllAccounts();

            // Assert
            assertThat(result).hasSize(2)
                    .extracting(AccountResponse::getAccountNumber)
                    .containsExactly(ACCOUNT_NUMBER, "ACC0000002");
            verify(accountRepository).findAll();
            verifyNoMoreInteractions(accountRepository);
        }

        @Test
        @DisplayName("should return an empty list when no accounts exist")
        void shouldReturnEmptyListWhenNoAccountsExist() {
            // Arrange
            when(accountRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<AccountResponse> result = accountService.getAllAccounts();

            // Assert
            assertThat(result).isNotNull().isEmpty();
            verify(accountRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("getAccountByNumber(String)")
    class GetAccountByNumber {

        @Test
        @DisplayName("should return the account when found by its number")
        void shouldReturnAccountWhenFoundByNumber() {
            // Arrange
            Account account = anAccount(ACCOUNT_ID, ACCOUNT_NUMBER, AccountType.SAVINGS,
                    OPENING_BALANCE, customer).build();
            when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(account));

            // Act
            AccountResponse response = accountService.getAccountByNumber(ACCOUNT_NUMBER);

            // Assert
            assertThat(response.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(response.getId()).isEqualTo(ACCOUNT_ID);
            verify(accountRepository).findByAccountNumber(ACCOUNT_NUMBER);
            verifyNoInteractions(customerRepository);
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when no account has the given number")
        void shouldThrowAccountNotFoundExceptionWhenNumberNotFound() {
            // Arrange
            String missingNumber = "ACC9999999";
            when(accountRepository.findByAccountNumber(missingNumber)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.getAccountByNumber(missingNumber))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(missingNumber);
            verify(accountRepository).findByAccountNumber(missingNumber);
        }
    }
}
