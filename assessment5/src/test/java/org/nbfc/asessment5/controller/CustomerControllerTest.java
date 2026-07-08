package org.nbfc.asessment5.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.dto.CustomerRequest;
import org.nbfc.asessment5.dto.CustomerResponse;
import org.nbfc.asessment5.exception.CustomerNotFoundException;
import org.nbfc.asessment5.exception.DuplicateEmailException;
import org.nbfc.asessment5.security.JwtAuthenticationFilter;
import org.nbfc.asessment5.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CustomerController — @WebMvcTest")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    private CustomerRequest validRequest;
    private CustomerResponse response;

    @BeforeEach
    void setUp() {
        validRequest = CustomerRequest.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .password("Secret123")
                .build();
        response = CustomerResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("9876543210")
                .build();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Nested
    @DisplayName("POST /api/customers")
    class CreateCustomer {

        @Test
        @DisplayName("should create a customer and return 201 with body")
        void shouldCreateCustomerAndReturn201() throws Exception {
            when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.password").doesNotExist());

            verify(customerService).createCustomer(any(CustomerRequest.class));
        }

        @Test
        @DisplayName("should return 400 when the name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            validRequest.setName("");

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("should return 400 when the email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            validRequest.setEmail("not-an-email");

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("should return 400 when the phone is not 10 digits")
        void shouldReturn400WhenPhoneInvalid() throws Exception {
            validRequest.setPhone("12345");

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("should return 409 when the email already exists")
        void shouldReturn409WhenDuplicateEmail() throws Exception {
            when(customerService.createCustomer(any(CustomerRequest.class)))
                    .thenThrow(new DuplicateEmailException("john.doe@example.com"));

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 when the request body is malformed JSON")
        void shouldReturn400WhenBodyMalformed() throws Exception {
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ this is not json "))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }
    }

    @Nested
    @DisplayName("GET /api/customers")
    class GetCustomers {

        @Test
        @DisplayName("should return a customer by id with 200")
        void shouldGetCustomerByIdAndReturn200() throws Exception {
            when(customerService.getCustomerById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/customers/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("John Doe"));

            verify(customerService).getCustomerById(1L);
        }

        @Test
        @DisplayName("should return 404 when the customer is not found")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            when(customerService.getCustomerById(99L)).thenThrow(new CustomerNotFoundException(99L));

            mockMvc.perform(get("/api/customers/{id}", 99L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return all customers with 200")
        void shouldReturnAllCustomers() throws Exception {
            when(customerService.getAllCustomers()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/customers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));
        }
    }

    @Nested
    @DisplayName("PUT & DELETE /api/customers/{id}")
    class UpdateAndDelete {

        @Test
        @DisplayName("should update a customer and return 200")
        void shouldUpdateCustomerAndReturn200() throws Exception {
            when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/customers/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(customerService).updateCustomer(eq(1L), any(CustomerRequest.class));
        }

        @Test
        @DisplayName("should return 404 when updating a missing customer")
        void shouldReturn404WhenUpdatingMissingCustomer() throws Exception {
            when(customerService.updateCustomer(eq(99L), any(CustomerRequest.class)))
                    .thenThrow(new CustomerNotFoundException(99L));

            mockMvc.perform(put("/api/customers/{id}", 99L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(validRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should delete a customer and return 204")
        void shouldDeleteCustomerAndReturn204() throws Exception {
            mockMvc.perform(delete("/api/customers/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(customerService).deleteCustomer(1L);
        }

        @Test
        @DisplayName("should return 404 when deleting a missing customer")
        void shouldReturn404WhenDeletingMissingCustomer() throws Exception {
            org.mockito.Mockito.doThrow(new CustomerNotFoundException(99L))
                    .when(customerService).deleteCustomer(99L);

            mockMvc.perform(delete("/api/customers/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
