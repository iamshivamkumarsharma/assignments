package org.northernarc.assessment4;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.northernarc.assessment4.model.Account;
import org.northernarc.assessment4.model.Customer;
import org.northernarc.assessment4.model.Transaction;
import org.northernarc.assessment4.repository.AccountRepository;
import org.northernarc.assessment4.repository.CustomerRepository;
import org.northernarc.assessment4.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Exhaustive / Hidden-Case Style Banking Tests")
class ExhaustiveBankingTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String SECRET = "YourSuperSecretSecureKeyForBankingAPI2026!!";

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    // ---------- helpers ----------
    private Key key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String signedToken(String subject, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(now - 2000))
                .setExpiration(new Date(now + ttlMillis))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Customer newCustomer(String name, String email, String branch) {
        Customer c = new Customer();
        c.setCustomerName(name);
        c.setEmail(email);
        c.setPassword(passwordEncoder.encode("password123"));
        c.setBranch(branch);
        return customerRepository.save(c);
    }

    private Account addAccount(Customer c, String number, String type, double balance) {
        Account a = new Account();
        a.setAccountNumber(number);
        a.setAccountType(type);
        a.setBalance(balance);
        a.setCustomer(c);
        a = accountRepository.save(a);
        c.getAccounts().add(a);
        return a;
    }

    private Transaction addTransaction(Account a, double amount, String type, LocalDate date) {
        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setTransactionType(type);
        t.setTransactionDate(date);
        t.setAccount(a);
        return transactionRepository.save(t);
    }

    // =====================================================================
    // 1. JWT AUTHENTICATION EDGE CASES
    // =====================================================================
    @Nested
    @DisplayName("JWT hardening")
    class JwtTests {

        @Test
        @DisplayName("Login token can be used as a Bearer to reach a protected endpoint")
        void loginTokenReachesProtectedEndpoint() throws Exception {
            newCustomer("Tok User", "tok@bank.com", "Chennai");
            String body = objectMapper.writeValueAsString(Map.of("email", "tok@bank.com", "password", "password123"));

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andReturn();

            String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
            assertThat(token).startsWith("eyJ");

            mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Expired JWT is rejected with 401")
        void expiredTokenRejected() throws Exception {
            newCustomer("Exp", "exp@bank.com", "Chennai");
            String expired = signedToken("exp@bank.com", -60_000); // already expired

            mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + expired))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Malformed JWT is rejected with 401 (not 500)")
        void malformedTokenRejected() throws Exception {
            mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer not.a.jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Tampered signature is rejected with 401")
        void tamperedTokenRejected() throws Exception {
            newCustomer("Tam", "tam@bank.com", "Chennai");
            String valid = signedToken("tam@bank.com", 600_000);
            String tampered = valid.substring(0, valid.length() - 3) + "AAA";

            mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + tampered))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Validly-signed token for a non-existent user must NOT 500 (expect 401)")
        void tokenForUnknownUserRejected() throws Exception {
            String ghost = signedToken("ghost@nowhere.com", 600_000);

            mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + ghost))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Login with wrong password returns 401")
        void wrongPasswordRejected() throws Exception {
            newCustomer("Wrong", "wrong@bank.com", "Chennai");
            String body = objectMapper.writeValueAsString(Map.of("email", "wrong@bank.com", "password", "totallyWrong"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Login with unknown email returns 401")
        void unknownEmailRejected() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("email", "nobody@bank.com", "password", "password123"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =====================================================================
    // 2. VALIDATION EDGE CASES
    // =====================================================================
    @Nested
    @DisplayName("Bean validation edges")
    class ValidationTests {

        @Test
        @DisplayName("Null branch customer -> 400 with field error")
        void nullBranchRejected() throws Exception {
            Customer c = new Customer();
            c.setCustomerName("NoBranch");
            c.setEmail("nb@bank.com");
            c.setPassword("longenough1");
            // branch left null

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(c)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("Blank account type -> 400")
        void blankAccountTypeRejected() throws Exception {
            Account a = new Account();
            a.setAccountNumber("ACCBLANK");
            a.setAccountType("");
            a.setBalance(1000.0);

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(a)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Zero balance violates @Positive -> 400")
        void zeroBalanceRejected() throws Exception {
            Account a = new Account();
            a.setAccountNumber("ACCZERO");
            a.setAccountType("SAVINGS");
            a.setBalance(0.0);

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(a)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Valid customer via API is created and password is BCrypt-encrypted")
        void validCustomerEncryptsPassword() throws Exception {
            Customer c = new Customer();
            c.setCustomerName("Secure User");
            c.setEmail("secure@bank.com");
            c.setPassword("PlainSecret123");
            c.setBranch("Pune");

            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(c)))
                    .andExpect(status().isCreated());

            Customer stored = customerRepository.findByEmail("secure@bank.com").orElseThrow();
            assertThat(stored.getPassword()).isNotEqualTo("PlainSecret123");
            assertThat(stored.getPassword()).startsWith("$2");
            assertThat(passwordEncoder.matches("PlainSecret123", stored.getPassword())).isTrue();
        }
    }

    // =====================================================================
    // 3. AUTHORIZATION (RBAC) EDGE CASES
    // =====================================================================
    @Nested
    @DisplayName("RBAC edges")
    class AuthorizationTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER may view accounts by type")
        void userCanViewByType() throws Exception {
            Customer c = newCustomer("V", "v@bank.com", "Chennai");
            addAccount(c, "ACCV1", "SAVINGS", 1000.0);

            mockMvc.perform(get("/api/accounts/view/ACCV1").param("type", "SAVINGS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER cannot delete (ADMIN-only) -> 403")
        void managerCannotDelete() throws Exception {
            mockMvc.perform(delete("/api/accounts/ACCX"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN cannot update balance (MANAGER-only) -> 403")
        void adminCannotUpdateBalance() throws Exception {
            mockMvc.perform(put("/api/accounts/ACCX/balance").param("amount", "10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN deleting a non-existent account -> 404 (not 500)")
        void adminDeleteNonExistent() throws Exception {
            mockMvc.perform(delete("/api/accounts/DOES-NOT-EXIST"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("NOT_FOUND"));
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER updating a non-existent account -> 404")
        void managerUpdateNonExistent() throws Exception {
            mockMvc.perform(put("/api/accounts/DOES-NOT-EXIST/balance").param("amount", "100"))
                    .andExpect(status().isNotFound());
        }
    }

    // =====================================================================
    // 4. REPOSITORY / JPQL EDGE CASES
    // =====================================================================
    @Nested
    @DisplayName("Repository & JPQL edges")
    class RepositoryTests {

        @Test
        @DisplayName("Rich-customers threshold above everyone's total -> empty")
        void richCustomersThresholdTooHigh() {
            Customer c = newCustomer("Poor", "poor@bank.com", "Chennai");
            addAccount(c, "ACCP", "SAVINGS", 500.0);

            assertThat(customerRepository.findRichCustomers(1_000_000.0)).isEmpty();
        }

        @Test
        @DisplayName("Idle-account query excludes accounts that already have a transaction")
        void idleExcludesAccountsWithTransactions() {
            Customer c = newCustomer("Idle", "idle@bank.com", "Chennai");
            Account withTx = addAccount(c, "ACC-TX", "SAVINGS", 1000.0);
            addAccount(c, "ACC-IDLE", "CURRENT", 2000.0);
            addTransaction(withTx, 50.0, "DEBIT", LocalDate.now());

            List<Account> idle = accountRepository.findAccountsWithNoTransactions();
            assertThat(idle).extracting(Account::getAccountNumber).containsExactly("ACC-IDLE");
        }

        @Test
        @DisplayName("Latest-transaction ordering breaks ties by id DESC")
        void latestTransactionTieBreak() {
            Customer c = newCustomer("L", "l@bank.com", "Chennai");
            Account a = addAccount(c, "ACC-L", "SAVINGS", 1000.0);
            LocalDate today = LocalDate.now();
            Transaction first = addTransaction(a, 100.0, "DEBIT", today);
            Transaction second = addTransaction(a, 200.0, "CREDIT", today);

            List<Transaction> latest = transactionRepository.findLatestTransaction(PageRequest.of(0, 1));
            assertThat(latest).hasSize(1);
            assertThat(latest.get(0).getTransactionId()).isEqualTo(second.getTransactionId());
            assertThat(first.getTransactionId()).isLessThan(second.getTransactionId());
        }

        @Test
        @DisplayName("Cascade delete removes accounts and their transactions")
        void cascadeDeleteRemovesChildren() {
            Customer c = newCustomer("Cascade", "cas@bank.com", "Chennai");
            Account a = addAccount(c, "ACC-CAS", "SAVINGS", 1000.0);
            Transaction t = addTransaction(a, 25.0, "DEBIT", LocalDate.now());

            customerRepository.deleteById(c.getCustomerId());

            assertThat(accountRepository.findById("ACC-CAS")).isEmpty();
            assertThat(transactionRepository.findById(t.getTransactionId())).isEmpty();
        }

        @Test
        @DisplayName("Duplicate email violates the unique constraint")
        void duplicateEmailRejected() {
            newCustomer("First", "dup@bank.com", "Chennai");

            Customer dup = new Customer();
            dup.setCustomerName("Second");
            dup.setEmail("dup@bank.com");
            dup.setPassword(passwordEncoder.encode("password123"));
            dup.setBranch("Delhi");

            assertThatThrownBy(() -> customerRepository.saveAndFlush(dup))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("increaseBalance on a missing account affects zero rows")
        void increaseBalanceMissingAccount() {
            assertThat(accountRepository.increaseBalance("NOPE", 100.0)).isZero();
        }

        @Test
        @DisplayName("existsByEmail / findByEmail behave correctly")
        void emailLookups() {
            newCustomer("E", "e@bank.com", "Chennai");
            assertThat(customerRepository.existsByEmail("e@bank.com")).isTrue();
            assertThat(customerRepository.existsByEmail("missing@bank.com")).isFalse();
            assertThat(customerRepository.findByEmail("e@bank.com")).isPresent();
        }

        @Test
        @DisplayName("Branch balance aggregation sums accounts across a branch")
        void branchBalanceAggregation() {
            Customer a = newCustomer("A", "a@bank.com", "Mumbai");
            addAccount(a, "M1", "SAVINGS", 100000.0);
            Customer b = newCustomer("B", "b@bank.com", "Mumbai");
            addAccount(b, "M2", "CURRENT", 50000.0);

            List<Object[]> rows = customerRepository.findTotalBalancePerBranch();
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[0]).isEqualTo("Mumbai");
            assertThat(((Number) rows.get(0)[1]).doubleValue()).isEqualTo(150000.0);
        }
    }

    // =====================================================================
    // 5. DTO + DASHBOARD EDGE CASES
    // =====================================================================
    @Nested
    @DisplayName("DTO & dashboard edges")
    class DashboardTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Summary for a customer with no accounts -> 0 accounts, 0.0 balance")
        void summaryWithNoAccounts() throws Exception {
            Customer c = newCustomer("Empty", "empty@bank.com", "Chennai");

            mockMvc.perform(get("/api/customers/" + c.getCustomerId() + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerName").value("Empty"))
                    .andExpect(jsonPath("$.numberOfAccounts").value(0))
                    .andExpect(jsonPath("$.totalBalance").value(0.0));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Dashboard on an empty database returns zeros and null leaders")
        void dashboardEmptyDatabase() throws Exception {
            mockMvc.perform(get("/api/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCustomers").value(0))
                    .andExpect(jsonPath("$.totalAccounts").value(0))
                    .andExpect(jsonPath("$.totalBalance").value(0.0))
                    .andExpect(jsonPath("$.topBranch").value(nullValue()))
                    .andExpect(jsonPath("$.highestBalanceCustomer").value(nullValue()));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Dashboard picks the richest branch and richest customer across data")
        void dashboardPicksLeaders() throws Exception {
            Customer alice = newCustomer("Alice", "alice@bank.com", "Mumbai");
            addAccount(alice, "AL1", "SAVINGS", 100000.0);
            Customer bob = newCustomer("Bob", "bob@bank.com", "Delhi");
            addAccount(bob, "BO1", "SAVINGS", 200000.0);
            addAccount(bob, "BO2", "CURRENT", 100000.0);

            mockMvc.perform(get("/api/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCustomers").value(2))
                    .andExpect(jsonPath("$.totalAccounts").value(3))
                    .andExpect(jsonPath("$.totalBalance").value(400000.0))
                    .andExpect(jsonPath("$.topBranch").value("Delhi"))
                    .andExpect(jsonPath("$.highestBalanceCustomer").value("Bob"));
        }
    }

    // =====================================================================
    // 6. PAGINATION EDGE CASES
    // =====================================================================
    @Nested
    @DisplayName("Pagination edges")
    class PaginationTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Page size 1 yields two pages, highest balance first")
        void pageSizeOne() throws Exception {
            Customer c = newCustomer("P", "p@bank.com", "Chennai");
            addAccount(c, "P-LOW", "SAVINGS", 50000.0);
            addAccount(c, "P-HIGH", "CURRENT", 150000.0);

            mockMvc.perform(get("/api/accounts").param("page", "0").param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].accountNumber").value("P-HIGH"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Requesting a page beyond the range returns empty content, correct totals")
        void pageBeyondRange() throws Exception {
            Customer c = newCustomer("PB", "pb@bank.com", "Chennai");
            addAccount(c, "PB1", "SAVINGS", 1000.0);
            addAccount(c, "PB2", "CURRENT", 2000.0);

            mockMvc.perform(get("/api/accounts").param("page", "5").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }
}
