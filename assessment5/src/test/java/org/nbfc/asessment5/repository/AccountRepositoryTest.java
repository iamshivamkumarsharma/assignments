package org.nbfc.asessment5.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.entity.Account;
import org.nbfc.asessment5.entity.AccountType;
import org.nbfc.asessment5.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("AccountRepository — @DataJpaTest")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = entityManager.persistAndFlush(Customer.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .password("$2a$10$encodedHash")
                .build());
    }

    private Customer persistCustomer(String email) {
        return entityManager.persistAndFlush(Customer.builder()
                .name("Jane Doe")
                .email(email)
                .phone("9123456780")
                .password("$2a$10$encodedHash")
                .build());
    }

    private Account account(String number, AccountType type, String balance, Customer owner) {
        return Account.builder()
                .accountNumber(number)
                .accountType(type)
                .balance(new BigDecimal(balance))
                .customer(owner)
                .build();
    }

    @Nested
    @DisplayName("save & findById")
    class SaveAndFind {

        @Test
        @DisplayName("should persist an account and generate an id")
        void shouldSaveAndGenerateId() {
            Account saved = accountRepository.save(account("AC0000001", AccountType.SAVINGS, "1000.00", customer));

            assertThat(saved.getId()).isNotNull();
            assertThat(accountRepository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("should keep the customer relationship on the persisted account")
        void shouldMaintainCustomerRelationship() {
            Account saved = entityManager.persistFlushFind(
                    account("AC0000001", AccountType.CURRENT, "500.00", customer));

            assertThat(saved.getCustomer()).isNotNull();
            assertThat(saved.getCustomer().getId()).isEqualTo(customer.getId());
            assertThat(saved.getAccountType()).isEqualTo(AccountType.CURRENT);
        }
    }

    @Nested
    @DisplayName("account-number queries")
    class AccountNumberQueries {

        @Test
        @DisplayName("should find an account by its number")
        void shouldFindByAccountNumber() {
            entityManager.persistAndFlush(account("AC0000009", AccountType.SAVINGS, "10.00", customer));

            Optional<Account> found = accountRepository.findByAccountNumber("AC0000009");

            assertThat(found).isPresent();
            assertThat(found.get().getBalance()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("should return empty optional for an unknown account number")
        void shouldReturnEmptyForUnknownNumber() {
            assertThat(accountRepository.findByAccountNumber("AC9999999")).isEmpty();
        }

        @Test
        @DisplayName("should report existsByAccountNumber correctly")
        void shouldReportExistsByAccountNumber() {
            entityManager.persistAndFlush(account("AC0000010", AccountType.SAVINGS, "10.00", customer));

            assertThat(accountRepository.existsByAccountNumber("AC0000010")).isTrue();
            assertThat(accountRepository.existsByAccountNumber("AC0000011")).isFalse();
        }

        @Test
        @DisplayName("should enforce the unique account-number constraint")
        void shouldEnforceUniqueAccountNumber() {
            entityManager.persistAndFlush(account("AC0000012", AccountType.SAVINGS, "10.00", customer));
            Account duplicate = account("AC0000012", AccountType.CURRENT, "20.00", customer);

            assertThatThrownBy(() -> accountRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("findByCustomerId")
    class FindByCustomer {

        @Test
        @DisplayName("should return every account belonging to a customer")
        void shouldFindAccountsByCustomerId() {
            entityManager.persistAndFlush(account("AC0000021", AccountType.SAVINGS, "100.00", customer));
            entityManager.persistAndFlush(account("AC0000022", AccountType.CURRENT, "200.00", customer));

            List<Account> accounts = accountRepository.findByCustomerId(customer.getId());

            assertThat(accounts).hasSize(2)
                    .extracting(Account::getAccountNumber)
                    .containsExactlyInAnyOrder("AC0000021", "AC0000022");
        }

        @Test
        @DisplayName("should not return accounts belonging to another customer")
        void shouldIsolateAccountsPerCustomer() {
            Customer other = persistCustomer("other@example.com");
            entityManager.persistAndFlush(account("AC0000031", AccountType.SAVINGS, "100.00", customer));
            entityManager.persistAndFlush(account("AC0000032", AccountType.SAVINGS, "300.00", other));

            assertThat(accountRepository.findByCustomerId(other.getId()))
                    .hasSize(1)
                    .extracting(Account::getAccountNumber)
                    .containsExactly("AC0000032");
        }

        @Test
        @DisplayName("should return an empty list for a customer with no accounts")
        void shouldReturnEmptyForCustomerWithoutAccounts() {
            Customer other = persistCustomer("empty@example.com");

            assertThat(accountRepository.findByCustomerId(other.getId())).isEmpty();
        }
    }
}
