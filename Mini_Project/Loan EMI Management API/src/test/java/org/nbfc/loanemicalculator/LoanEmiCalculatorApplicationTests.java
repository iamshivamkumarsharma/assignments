package org.nbfc.loanemicalculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nbfc.loanemicalculator.dto.LoginRequest;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.nbfc.loanemicalculator.entity.Loan;
import org.nbfc.loanemicalculator.entity.PenaltyTransaction;
import org.nbfc.loanemicalculator.repository.CustomerRepository;
import org.nbfc.loanemicalculator.repository.EmiScheduleRepository;
import org.nbfc.loanemicalculator.repository.LoanRepository;
import org.nbfc.loanemicalculator.repository.PenaltyTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoanEmiCalculatorApplicationTests {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired CustomerRepository customerRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired EmiScheduleRepository emiScheduleRepository;
    @Autowired PenaltyTransactionRepository penaltyTransactionRepository;
    @Autowired PasswordEncoder encoder;

    private Long userId;
    private Long eduLoanId;

    @BeforeEach
    void seed() {
        penaltyTransactionRepository.deleteAll();
        emiScheduleRepository.deleteAll();
        loanRepository.deleteAll();
        customerRepository.deleteAll();

        Customer user = customer("Rahul Sharma", "user@bank.com", "USER", "Bangalore");
        Customer manager = customer("Neha Roy", "manager@bank.com", "MANAGER", "Mumbai");
        Customer admin = customer("Raj Admin", "admin@bank.com", "ADMIN", "Delhi");
        customerRepository.saveAll(java.util.List.of(user, manager, admin));
        userId = user.getCustomerId();

        Loan home = loan("HOME", 1000000.0, 8.0, 240, 10000.0);
        Loan vehicle = loan("VEHICLE", 500000.0, 12.0, 60, 11000.0);
        Loan edu = loan("EDUCATION", 300000.0, 5.0, 48, 7000.0);
        loanRepository.saveAll(java.util.List.of(home, vehicle, edu));
        eduLoanId = edu.getLoanId();

        emiScheduleRepository.save(emi(user, home, "OVERDUE"));
        emiScheduleRepository.save(emi(user, home, "PENDING"));
        emiScheduleRepository.save(emi(user, vehicle, "PAID"));
        emiScheduleRepository.save(emi(manager, edu, "PENDING"));

        penaltyTransactionRepository.save(penalty(home, 500.0, "CASH", LocalDate.of(2026, 1, 1)));
        penaltyTransactionRepository.save(penalty(vehicle, 750.0, "CARD", LocalDate.of(2026, 2, 1)));
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

    private PenaltyTransaction penalty(Loan l, double amt, String mode, LocalDate d) {
        PenaltyTransaction p = new PenaltyTransaction();
        p.setLoan(l);
        p.setPenaltyAmount(amt);
        p.setPaymentMode(mode);
        p.setPaymentDate(d);
        return p;
    }

    private String token(String email) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword("password1");
        MvcResult r = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))).andExpect(status().isOk()).andReturn();
        return mapper.readValue(r.getResponse().getContentAsString(), Map.class).get("token").toString();
    }

    // --- Auth ---
    @Test void loginSuccess() throws Exception { assertNotNull(token("user@bank.com")); }

    @Test void loginInvalid() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@bank.com"); req.setPassword("wrong");
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req))).andExpect(status().isUnauthorized());
    }

    @Test void protectedRequiresToken() throws Exception {
        mockMvc.perform(get("/loans")).andExpect(status().isForbidden());
    }

    // --- Pagination & sorting ---
    @Test void loansSortedByInterestDesc() throws Exception {
        mockMvc.perform(get("/loans").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].interestRate").value(12.0));
    }

    // --- Derived queries ---
    @Test void byLoanType() throws Exception {
        mockMvc.perform(get("/loans/type/HOME").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].loanType").value("HOME"));
    }

    @Test void byInterestGreaterThan() throws Exception {
        mockMvc.perform(get("/loans/interest?rate=10").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].loanType").value("VEHICLE"));
    }

    @Test void byBranch() throws Exception {
        mockMvc.perform(get("/customers/branch/Bangalore").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].customerName").value("Rahul Sharma"));
    }

    @Test void penaltiesByMode() throws Exception {
        mockMvc.perform(get("/penalties/mode/CASH").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].penaltyAmount").value(500.0));
    }

    @Test void emisByStatus() throws Exception {
        mockMvc.perform(get("/emis/status/OVERDUE").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("OVERDUE"));
    }

    // --- JPQL ---
    @Test void highValueBorrowers() throws Exception {
        mockMvc.perform(get("/customers/high-value?count=1").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].customerName").value("Rahul Sharma"));
    }

    @Test void multiLoanCustomers() throws Exception {
        mockMvc.perform(get("/customers/multi-loan").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].customerName").value("Rahul Sharma"));
    }

    @Test void latestPenalty() throws Exception {
        mockMvc.perform(get("/penalties/latest").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.penaltyAmount").value(750.0));
    }

    @Test void loansNoOverdue() throws Exception {
        mockMvc.perform(get("/loans/no-overdue").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test void penaltiesPerBranch() throws Exception {
        mockMvc.perform(get("/penalties/per-branch").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0][0]").value("Bangalore"));
    }

    // --- DTO ---
    @Test void customerSummary() throws Exception {
        mockMvc.perform(get("/customers/" + userId + "/summary").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPenaltyPaid").value(1250.0))
                .andExpect(jsonPath("$.numberOfLoans").value(2));
    }

    // --- Role-based authorization ---
    @Test void managerUpdatesRates() throws Exception {
        mockMvc.perform(put("/loans/interest-rate?loanType=HOME&amount=2").header("Authorization", "Bearer " + token("manager@bank.com")))
                .andExpect(status().isOk());
        assertEquals(10.0, loanRepository.findByLoanType("HOME").get(0).getInterestRate());
    }

    @Test void userCannotUpdateRates() throws Exception {
        mockMvc.perform(put("/loans/interest-rate?loanType=HOME&amount=2").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isForbidden());
    }

    @Test void adminDeletesLoan() throws Exception {
        mockMvc.perform(delete("/loans/" + eduLoanId).header("Authorization", "Bearer " + token("admin@bank.com")))
                .andExpect(status().isOk());
    }

    @Test void userCannotDelete() throws Exception {
        mockMvc.perform(delete("/loans/" + eduLoanId).header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isForbidden());
    }

    // --- Exception handling ---
    @Test void loanNotFound() throws Exception {
        mockMvc.perform(delete("/loans/999999").header("Authorization", "Bearer " + token("admin@bank.com")))
                .andExpect(status().isNotFound());
    }

    @Test void customerSummaryNotFound() throws Exception {
        mockMvc.perform(get("/customers/9999/summary").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isNotFound());
    }

    @Test void validationFails() throws Exception {
        Customer bad = new Customer();
        bad.setEmail("not-an-email");
        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bad))).andExpect(status().isBadRequest());
    }

    // --- Dashboard ---
    @Test void dashboard() throws Exception {
        mockMvc.perform(get("/dashboard").header("Authorization", "Bearer " + token("user@bank.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(3))
                .andExpect(jsonPath("$.totalLoans").value(3))
                .andExpect(jsonPath("$.totalPenaltyCollected").value(1250.0))
                .andExpect(jsonPath("$.topPerformingBranch").value("Bangalore"))
                .andExpect(jsonPath("$.highestPenaltyPayingCustomer").value("Rahul Sharma"))
                .andExpect(jsonPath("$.totalOverdueEmis").value(1));
    }
}
