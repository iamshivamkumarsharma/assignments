package org.nbfc.asessment5.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("CustomerRepository — @DataJpaTest")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final String EMAIL = "john.doe@example.com";

    private Customer transientCustomer;

    @BeforeEach
    void setUp() {
        transientCustomer = customer(EMAIL);
    }

    private Customer customer(String email) {
        return Customer.builder()
                .name("John Doe")
                .email(email)
                .phone("9876543210")
                .password("$2a$10$encodedHash")
                .build();
    }

    @Nested
    @DisplayName("save & findById")
    class SaveAndFind {

        @Test
        @DisplayName("should persist a customer and generate an id")
        void shouldSaveAndGenerateId() {
            Customer saved = customerRepository.save(transientCustomer);

            assertThat(saved.getId()).isNotNull();
            Optional<Customer> found = customerRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("should return empty optional for an unknown id")
        void shouldReturnEmptyForUnknownId() {
            assertThat(customerRepository.findById(9999L)).isEmpty();
        }

        @Test
        @DisplayName("should return all persisted customers")
        void shouldFindAll() {
            customerRepository.save(customer("a@example.com"));
            customerRepository.save(customer("b@example.com"));

            List<Customer> all = customerRepository.findAll();

            assertThat(all).extracting(Customer::getEmail)
                    .contains("a@example.com", "b@example.com");
        }
    }

    @Nested
    @DisplayName("findByEmail / existsByEmail")
    class EmailQueries {

        @Test
        @DisplayName("should find a customer by email when present")
        void shouldFindByEmailWhenPresent() {
            entityManager.persistAndFlush(transientCustomer);

            Optional<Customer> found = customerRepository.findByEmail(EMAIL);

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should return empty optional when the email is absent")
        void shouldReturnEmptyWhenEmailAbsent() {
            assertThat(customerRepository.findByEmail("missing@example.com")).isEmpty();
        }

        @Test
        @DisplayName("should report existsByEmail = true for a registered email")
        void shouldReturnTrueWhenEmailExists() {
            entityManager.persistAndFlush(transientCustomer);

            assertThat(customerRepository.existsByEmail(EMAIL)).isTrue();
        }

        @Test
        @DisplayName("should report existsByEmail = false for an unknown email")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            assertThat(customerRepository.existsByEmail("nobody@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("constraints & deletion")
    class ConstraintsAndDeletion {

        @Test
        @DisplayName("should enforce the unique email constraint")
        void shouldEnforceUniqueEmailConstraint() {
            entityManager.persistAndFlush(customer(EMAIL));
            Customer duplicate = customer(EMAIL);

            assertThatThrownBy(() -> customerRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should delete a customer by id")
        void shouldDeleteCustomer() {
            Customer saved = entityManager.persistFlushFind(transientCustomer);

            customerRepository.deleteById(saved.getId());
            entityManager.flush();

            assertThat(customerRepository.findById(saved.getId())).isEmpty();
        }
    }
}
