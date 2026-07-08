package org.nbfc.asessment5.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.dto.AuthResponse;
import org.nbfc.asessment5.dto.CustomerRequest;
import org.nbfc.asessment5.dto.CustomerResponse;
import org.nbfc.asessment5.dto.LoginRequest;
import org.nbfc.asessment5.repository.AccountRepository;
import org.nbfc.asessment5.repository.CustomerRepository;
import org.nbfc.asessment5.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Customer flow — Integration (@SpringBootTest)")
class CustomerIntegrationTest {

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

    private static final String EMAIL = "john.doe@example.com";
    private static final String PASSWORD = "Secret123";

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    private CustomerRequest customerRequest(String name, String email, String phone) {
        return CustomerRequest.builder()
                .name(name).email(email).phone(phone).password(PASSWORD).build();
    }

    private CustomerResponse register(CustomerRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CustomerResponse.class);
    }

    private String login(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder().email(email).password(password).build();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return "Bearer " + auth.getToken();
    }

    @Test
    @DisplayName("should register, login, and read the customer through the secured API")
    void shouldRegisterLoginAndReadCustomer() throws Exception {
        CustomerResponse registered = register(customerRequest("John Doe", EMAIL, "9876543210"));
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/customers/{id}", registered.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("John Doe"));

        assertThat(customerRepository.existsByEmail(EMAIL)).isTrue();
    }

    @Test
    @DisplayName("should reject duplicate registration with 409")
    void shouldRejectDuplicateRegistration() throws Exception {
        register(customerRequest("John Doe", EMAIL, "9876543210"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest("John Two", EMAIL, "9000000000"))))
                .andExpect(status().isConflict());

        assertThat(customerRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("should update a customer through the secured API")
    void shouldUpdateCustomer() throws Exception {
        CustomerResponse registered = register(customerRequest("John Doe", EMAIL, "9876543210"));
        String token = login(EMAIL, PASSWORD);

        CustomerRequest update = customerRequest("Johnathan Doe", EMAIL, "9123456780");
        mockMvc.perform(put("/api/customers/{id}", registered.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Johnathan Doe"))
                .andExpect(jsonPath("$.phone").value("9123456780"));

        assertThat(customerRepository.findByEmail(EMAIL))
                .isPresent()
                .get()
                .extracting("name").isEqualTo("Johnathan Doe");
    }

    @Test
    @DisplayName("should delete a customer and remove it from the repository")
    void shouldDeleteCustomer() throws Exception {
        CustomerResponse registered = register(customerRequest("John Doe", EMAIL, "9876543210"));
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(delete("/api/customers/{id}", registered.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.findById(registered.getId())).isEmpty();
    }

    @Test
    @DisplayName("should list all customers for an authenticated caller")
    void shouldListAllCustomers() throws Exception {
        register(customerRequest("John Doe", EMAIL, "9876543210"));
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/customers").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EMAIL));
    }

    @Test
    @DisplayName("should reject access to the customer API without authentication (401)")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }
}
