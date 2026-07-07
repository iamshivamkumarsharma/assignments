package org.nbfc.loanemicalculator.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nbfc.loanemicalculator.dto.DashboardDTO;
import org.nbfc.loanemicalculator.entity.Loan;
import org.nbfc.loanemicalculator.exception.GlobalExceptionHandler;
import org.nbfc.loanemicalculator.service.LoanService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pure controller-slice test using standalone MockMvc with a mocked service.
 * No Spring context, database or security is started, so it exercises only the
 * request mapping, JSON (de)serialization and status handling of the controller.
 */
@ExtendWith(MockitoExtension.class)
class LoanControllerUnitTest {

    @Mock
    LoanService loanService;

    @InjectMocks
    LoanController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Loan loan(String type) {
        Loan l = new Loan();
        l.setLoanId(1L);
        l.setLoanType(type);
        l.setInterestRate(8.0);
        l.setLoanStatus("ACTIVE");
        return l;
    }

    @Test
    void dashboard_returnsAggregates() throws Exception {
        when(loanService.getDashboard())
                .thenReturn(new DashboardDTO(3L, 3L, 1250.0, "Bangalore", "Rahul Sharma", 1L));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(3))
                .andExpect(jsonPath("$.totalPenaltyCollected").value(1250.0))
                .andExpect(jsonPath("$.topPerformingBranch").value("Bangalore"));
    }

    @Test
    void noOverdue_returnsLoanList() throws Exception {
        when(loanService.findLoansWithoutOverdue()).thenReturn(List.of(loan("EDUCATION")));

        mockMvc.perform(get("/loans/no-overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanType").value("EDUCATION"));
    }

    @Test
    void createLoan_returns201() throws Exception {
        when(loanService.createLoan(any())).thenReturn(loan("VEHICLE"));
        String body = "{\"loanType\":\"VEHICLE\",\"principalAmount\":500000,\"interestRate\":11.5,"
                + "\"loanTenureMonths\":60,\"monthlyEmi\":11000,\"loanStatus\":\"ACTIVE\"}";

        mockMvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanType").value("VEHICLE"));
    }
}
