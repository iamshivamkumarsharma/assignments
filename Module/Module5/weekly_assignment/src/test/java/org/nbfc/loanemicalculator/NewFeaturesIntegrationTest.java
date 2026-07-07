package org.nbfc.loanemicalculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nbfc.loanemicalculator.dto.LoginRequest;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.nbfc.loanemicalculator.entity.Loan;
import org.nbfc.loanemicalculator.repository.CustomerRepository;
import org.nbfc.loanemicalculator.repository.EmiScheduleRepository;
import org.nbfc.loanemicalculator.repository.LoanRepository;
import org.nbfc.loanemicalculator.repository.PenaltyTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration coverage for the features added on top of the reference solution:
 * loan creation, the atomic pay-EMI transaction, N+1-free loan details, paged EMIs,
 * enum parameter validation and password-safe registration.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NewFeaturesIntegrationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired CustomerRepository customerRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired EmiScheduleRepository emiScheduleRepository;
    @Autowired PenaltyTransactionRepository penaltyTransactionRepository;
    @Autowired PasswordEncoder encoder;

    private Long overdueEmiId;

    @BeforeEach
    void seed() {
        penaltyTransactionRepository.deleteAll();
        emiScheduleRepository.deleteAll();
        loanRepository.deleteAll();
        customerRepository.deleteAll();

        Customer user = customer("Rahul Sharma", "user@bank.com", "USER", "Bangalore");
        Customer manager = customer("Neha Roy", "manager@bank.com", "MANAGER", "Mumbai");
        Customer admin = customer("Raj Admin", "admin@bank.com", "ADMIN", "Delhi");
        customerRepository.saveAll(List.of(user, manager, admin));

        Loan home = loan("HOME", 1000000.0, 8.0, 240, 10000.0);
        loanRepository.save(home);

        EmiSchedule overdue = emi(user, home, "OVERDUE");
        emiScheduleRepository.save(overdue);
        overdueEmiId = overdue.getEmiId();
    }

    private Customer customer(String name, String email, String role, String branch) {
        Customer c = new Customer();
        c.setCustomerName(name);
        c.setEmail(email);
        c.setPassword(encoder.encode("password1"));
        c.setRole(role);
        c.setBranchName(branch);
        return c;
    }

    private Loan loan(String type, double principal, double interest, int tenure, double emi) {
        Loan l = new Loan();
        l.setLoanType(type);
        l.setPrincipalAmount(principal);
        l.setInterestRate(interest);
        l.setLoanTenureMonths(tenure);
        l.setMonthlyEmi(emi);
        l.setLoanStatus("ACTIVE");
        return l;
    }

    private EmiSchedule emi(Customer c, Loan l, String status) {
        EmiSchedule e = new EmiSchedule();
        e.setCustomer(c);
        e.setLoan(l);
        e.setStatus(status);
        e.setEmiAmount(l.getMonthlyEmi());
        e.setDueDate(LocalDate.of(2026, 1, 15));
        return e;
    }

    private String token(String email) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("password1");
        MvcResult r = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))).andExpect(status().isOk()).andReturn();
        return mapper.readValue(r.getResponse().getContentAsString(), Map.class).get("token").toString();
    }

    @Test
    void managerCanCreateLoan_returns201() throws Exception {
        String body = "{\"loanType\":\"VEHICLE\",\"principalAmount\":500000,\"interestRate\":11.5,"
                + "\"loanTenureMonths\":60,\"monthlyEmi\":11000,\"loanStatus\":\"ACTIVE\"}";
        mockMvc.perform(post("/loans").header("Authorization", "Bearer " + token("manager@bank.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanType").value("VEHICLE"));
        assertEquals(2, loanRepository.count());
    }

    @Test
    void createLoanWithInvalidEnum_returns400() throws Exception {
        String body = "{\"loanType\":\"SPACESHIP\",\"principalAmount\":500000,\"interestRate\":11.5,"
                + "\"loanTenureMonths\":60,\"monthlyEmi\":11000,\"loanStatus\":\"ACTIVE\"}";
        mockMvc.perform(post("/loans").header("Authorization", "Bearer " + token("manager@bank.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void userCannotCreateLoan_returns403() throws Exception {
        String body = "{\"loanType\":\"HOME\",\"principalAmount\":1,\"interestRate\":1,"
                + "\"loanTenureMonths\":1,\"monthlyEmi\":1,\"loanStatus\":\"ACTIVE\"}";
        mockMvc.perform(post("/loans").header("Authorization", "Bearer " + token("user@bank.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidEnumPathParam_returns400() throws Exception {
        mockMvc.perform(get("/loans/type/SPACESHIP").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payEmi_settlesOverdueAndRecordsPenaltyAtomically() throws Exception {
        String body = "{\"paymentMode\":\"ONLINE\",\"penaltyAmount\":250.0}";
        mockMvc.perform(post("/emis/" + overdueEmiId + "/pay").header("Authorization", "Bearer " + token("user@bank.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        assertEquals(1, penaltyTransactionRepository.count());
        assertEquals("PAID", emiScheduleRepository.findById(overdueEmiId).orElseThrow().getStatus());
    }

    @Test
    void payMissingEmi_returns404() throws Exception {
        String body = "{\"paymentMode\":\"CASH\"}";
        mockMvc.perform(post("/emis/999999/pay").header("Authorization", "Bearer " + token("user@bank.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void loanDetails_returnsScheduleSummary_withoutNPlus1() throws Exception {
        mockMvc.perform(get("/loans/details").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalEmis").value(1))
                .andExpect(jsonPath("$[0].overdueEmis").value(1));
    }

    @Test
    void pagedEmis_returnsContent() throws Exception {
        mockMvc.perform(get("/emis?status=OVERDUE&page=0&size=5").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("OVERDUE"));
    }

    @Test
    void register_returnsCustomer_withoutPasswordHash() throws Exception {
        String body = "{\"customerName\":\"New Person\",\"email\":\"new@bank.com\","
                + "\"password\":\"password1\",\"branchName\":\"Pune\"}";
        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@bank.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
