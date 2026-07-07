package org.nbfc.loanemicalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nbfc.loanemicalculator.repository.CustomerRepository;
import org.nbfc.loanemicalculator.repository.EmiScheduleRepository;
import org.nbfc.loanemicalculator.repository.LoanRepository;
import org.nbfc.loanemicalculator.repository.PenaltyTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context MockMvc coverage for the newly added features:
 * the {@code DuplicateEmailException} (HTTP 409) on repeated registration,
 * and the publicly reachable OpenAPI / Swagger documentation endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndDocsMockMvcTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    LoanRepository loanRepository;
    @Autowired
    EmiScheduleRepository emiScheduleRepository;
    @Autowired
    PenaltyTransactionRepository penaltyTransactionRepository;

    private static final String BODY =
            "{\"customerName\":\"Dup Person\",\"email\":\"dup@bank.com\","
                    + "\"password\":\"password1\",\"branchName\":\"Pune\"}";

    @BeforeEach
    void clean() {
        penaltyTransactionRepository.deleteAll();
        emiScheduleRepository.deleteAll();
        loanRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void register_thenDuplicate_returns409() throws Exception {
        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email dup@bank.com is already registered."));
    }

    @Test
    void openApiDocs_areAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Loan EMI Management API"));
    }
}
