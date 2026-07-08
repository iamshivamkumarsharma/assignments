package org.nbfc.asessment5.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nbfc.asessment5.dto.CustomerRequest;
import org.nbfc.asessment5.dto.CustomerResponse;
import org.nbfc.asessment5.entity.Customer;
import org.nbfc.asessment5.exception.CustomerNotFoundException;
import org.nbfc.asessment5.exception.DuplicateEmailException;
import org.nbfc.asessment5.repository.CustomerRepository;
import org.nbfc.asessment5.service.impl.CustomerServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService — Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Captor
    private ArgumentCaptor<Customer> customerCaptor;

    private static final Long CUSTOMER_ID = 1L;
    private static final String NAME = "John Doe";
    private static final String EMAIL = "john.doe@example.com";
    private static final String PHONE = "9876543210";
    private static final String RAW_PASSWORD = "P@ssw0rd123";
    private static final String ENCODED_PASSWORD = "$2a$10$abcdefghijklmnopqrstuvEncodedHash";

    private CustomerRequest customerRequest;
    private Customer persistedCustomer;

    @BeforeEach
    void setUp() {
        customerRequest = aCustomerRequest().build();
        persistedCustomer = aCustomer(CUSTOMER_ID, ENCODED_PASSWORD).build();
    }

    // ---------------------------------------------------------------------
    // Helper builders (avoid duplicate setup code)
    // ---------------------------------------------------------------------

    private CustomerRequest.CustomerRequestBuilder aCustomerRequest() {
        return CustomerRequest.builder()
                .name(NAME)
                .email(EMAIL)
                .phone(PHONE)
                .password(RAW_PASSWORD);
    }

    private Customer.CustomerBuilder aCustomer(Long id, String password) {
        return Customer.builder()
                .id(id)
                .name(NAME)
                .email(EMAIL)
                .phone(PHONE)
                .password(password);
    }

    @Nested
    @DisplayName("createCustomer(CustomerRequest)")
    class CreateCustomer {

        @Test
        @DisplayName("should create customer successfully when email is unique")
        void shouldCreateCustomerSuccessfully() {
            // Arrange
            when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenReturn(persistedCustomer);

            // Act
            CustomerResponse response = customerService.createCustomer(customerRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(CUSTOMER_ID);
            assertThat(response.getName()).isEqualTo(NAME);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getPhone()).isEqualTo(PHONE);
            verify(customerRepository).existsByEmail(EMAIL);
            verify(passwordEncoder).encode(RAW_PASSWORD);
            verify(customerRepository).save(any(Customer.class));
            verifyNoMoreInteractions(customerRepository, passwordEncoder);
        }

        @Test
        @DisplayName("should encode the raw password before persisting the customer")
        void shouldEncodePasswordBeforePersisting() {
            // Arrange
            when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenReturn(persistedCustomer);

            // Act
            customerService.createCustomer(customerRequest);

            // Assert
            verify(customerRepository).save(customerCaptor.capture());
            Customer captured = customerCaptor.getValue();
            assertThat(captured.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(captured.getPassword()).isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        @DisplayName("should persist the customer with the exact fields from the request")
        void shouldPersistCustomerWithRequestFields() {
            // Arrange
            when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenReturn(persistedCustomer);

            // Act
            customerService.createCustomer(customerRequest);

            // Assert
            verify(customerRepository).save(customerCaptor.capture());
            Customer captured = customerCaptor.getValue();
            assertThat(captured.getName()).isEqualTo(NAME);
            assertThat(captured.getEmail()).isEqualTo(EMAIL);
            assertThat(captured.getPhone()).isEqualTo(PHONE);
        }

        @Test
        @DisplayName("should throw DuplicateEmailException when the email already exists")
        void shouldThrowDuplicateEmailExceptionWhenEmailExists() {
            // Arrange
            when(customerRepository.existsByEmail(EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> customerService.createCustomer(customerRequest))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining(EMAIL);
        }

        @Test
        @DisplayName("should not encode password or save when the email is duplicate")
        void shouldNotSaveWhenEmailIsDuplicate() {
            // Arrange
            when(customerRepository.existsByEmail(EMAIL)).thenReturn(true);

            // Act
            assertThatThrownBy(() -> customerService.createCustomer(customerRequest))
                    .isInstanceOf(DuplicateEmailException.class);

            // Assert
            verify(customerRepository).existsByEmail(EMAIL);
            verify(customerRepository, never()).save(any(Customer.class));
            verify(passwordEncoder, never()).encode(anyString());
            verifyNoMoreInteractions(customerRepository);
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("should check email uniqueness, then encode, then save (correct order)")
        void shouldCheckEmailUniquenessBeforeEncodingAndSaving() {
            // Arrange
            when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenReturn(persistedCustomer);

            // Act
            customerService.createCustomer(customerRequest);

            // Assert
            InOrder inOrder = inOrder(customerRepository, passwordEncoder);
            inOrder.verify(customerRepository).existsByEmail(EMAIL);
            inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);
            inOrder.verify(customerRepository).save(any(Customer.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("should not expose the encoded password on the response")
        void shouldNotExposePasswordOnResponse() {
            // Arrange
            when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenReturn(persistedCustomer);

            // Act
            CustomerResponse response = customerService.createCustomer(customerRequest);

            // Assert
            assertThat(response).extracting(Object::toString)
                    .satisfies(s -> assertThat(s).doesNotContain(ENCODED_PASSWORD, RAW_PASSWORD));
        }
    }

    @Nested
    @DisplayName("getCustomerById(Long)")
    class GetCustomerById {

        @Test
        @DisplayName("should return the customer when it exists")
        void shouldReturnCustomerWhenFound() {
            // Arrange
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(persistedCustomer));

            // Act
            CustomerResponse response = customerService.getCustomerById(CUSTOMER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(CUSTOMER_ID);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            verify(customerRepository).findById(CUSTOMER_ID);
            verifyNoMoreInteractions(customerRepository);
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when the customer does not exist")
        void shouldThrowCustomerNotFoundExceptionWhenNotFound() {
            // Arrange
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> customerService.getCustomerById(CUSTOMER_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(String.valueOf(CUSTOMER_ID));
            verify(customerRepository).findById(CUSTOMER_ID);
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("getAllCustomers()")
    class GetAllCustomers {

        @Test
        @DisplayName("should return every customer mapped to a response")
        void shouldReturnAllCustomers() {
            // Arrange
            Customer second = aCustomer(2L, ENCODED_PASSWORD).email("jane.doe@example.com").build();
            when(customerRepository.findAll()).thenReturn(List.of(persistedCustomer, second));

            // Act
            List<CustomerResponse> result = customerService.getAllCustomers();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CustomerResponse::getEmail)
                    .containsExactly(EMAIL, "jane.doe@example.com");
            verify(customerRepository).findAll();
            verifyNoMoreInteractions(customerRepository);
        }

        @Test
        @DisplayName("should return an empty list when no customers exist")
        void shouldReturnEmptyListWhenNoCustomersExist() {
            // Arrange
            when(customerRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<CustomerResponse> result = customerService.getAllCustomers();

            // Assert
            assertThat(result).isNotNull().isEmpty();
            verify(customerRepository).findAll();
        }
    }

    @Nested
    @DisplayName("updateCustomer(Long, CustomerRequest)")
    class UpdateCustomer {

        private static final String NEW_NAME = "Johnathan Doe";
        private static final String NEW_PHONE = "9123456780";
        private static final String NEW_RAW_PASSWORD = "N3wP@ssw0rd";
        private static final String NEW_ENCODED_PASSWORD = "$2a$10$newlyEncodedHashValue000000";

        @Test
        @DisplayName("should update an existing customer successfully")
        void shouldUpdateCustomerSuccessfully() {
            // Arrange
            Customer existing = aCustomer(CUSTOMER_ID, ENCODED_PASSWORD).build();
            CustomerRequest updateRequest = aCustomerRequest()
                    .name(NEW_NAME).phone(NEW_PHONE).password(NEW_RAW_PASSWORD).build();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            CustomerResponse response = customerService.updateCustomer(CUSTOMER_ID, updateRequest);

            // Assert
            assertThat(response.getId()).isEqualTo(CUSTOMER_ID);
            assertThat(response.getName()).isEqualTo(NEW_NAME);
            assertThat(response.getPhone()).isEqualTo(NEW_PHONE);
            verify(customerRepository).findById(CUSTOMER_ID);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should re-encode the password when updating")
        void shouldReEncodePasswordWhenUpdating() {
            // Arrange
            Customer existing = aCustomer(CUSTOMER_ID, ENCODED_PASSWORD).build();
            CustomerRequest updateRequest = aCustomerRequest().password(NEW_RAW_PASSWORD).build();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            customerService.updateCustomer(CUSTOMER_ID, updateRequest);

            // Assert
            verify(passwordEncoder).encode(NEW_RAW_PASSWORD);
            verify(customerRepository).save(customerCaptor.capture());
            assertThat(customerCaptor.getValue().getPassword()).isEqualTo(NEW_ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("should persist the updated fields on the managed entity")
        void shouldPersistUpdatedFields() {
            // Arrange
            Customer existing = aCustomer(CUSTOMER_ID, ENCODED_PASSWORD).build();
            CustomerRequest updateRequest = aCustomerRequest()
                    .name(NEW_NAME).phone(NEW_PHONE).password(NEW_RAW_PASSWORD).build();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            customerService.updateCustomer(CUSTOMER_ID, updateRequest);

            // Assert
            verify(customerRepository).save(customerCaptor.capture());
            Customer saved = customerCaptor.getValue();
            assertThat(saved.getId()).isEqualTo(CUSTOMER_ID);
            assertThat(saved.getName()).isEqualTo(NEW_NAME);
            assertThat(saved.getPhone()).isEqualTo(NEW_PHONE);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when updating a non-existent customer")
        void shouldThrowCustomerNotFoundExceptionWhenUpdatingNonExistentCustomer() {
            // Arrange
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());
            CustomerRequest updateRequest = aCustomerRequest().name(NEW_NAME).build();

            // Act & Assert
            assertThatThrownBy(() -> customerService.updateCustomer(CUSTOMER_ID, updateRequest))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(String.valueOf(CUSTOMER_ID));
            verify(customerRepository).findById(CUSTOMER_ID);
            verify(customerRepository, never()).save(any(Customer.class));
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("deleteCustomer(Long)")
    class DeleteCustomer {

        @Test
        @DisplayName("should delete the customer when it exists")
        void shouldDeleteCustomerSuccessfully() {
            // Arrange
            when(customerRepository.existsById(CUSTOMER_ID)).thenReturn(true);

            // Act
            customerService.deleteCustomer(CUSTOMER_ID);

            // Assert
            InOrder inOrder = inOrder(customerRepository);
            inOrder.verify(customerRepository).existsById(CUSTOMER_ID);
            inOrder.verify(customerRepository).deleteById(CUSTOMER_ID);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when deleting a non-existent customer")
        void shouldThrowCustomerNotFoundExceptionWhenDeletingNonExistentCustomer() {
            // Arrange
            when(customerRepository.existsById(CUSTOMER_ID)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> customerService.deleteCustomer(CUSTOMER_ID))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(String.valueOf(CUSTOMER_ID));
            verify(customerRepository).existsById(CUSTOMER_ID);
            verify(customerRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("should delete exactly once for a valid id")
        void shouldDeleteExactlyOnce() {
            // Arrange
            when(customerRepository.existsById(CUSTOMER_ID)).thenReturn(true);

            // Act
            customerService.deleteCustomer(CUSTOMER_ID);

            // Assert
            verify(customerRepository, times(1)).deleteById(CUSTOMER_ID);
        }
    }
}
