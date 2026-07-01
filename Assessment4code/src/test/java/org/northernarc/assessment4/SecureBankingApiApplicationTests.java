package org.northernarc.assessment4;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.northernarc.assessment4.model.*;
import org.northernarc.assessment4.repository.AccountRepository;
import org.northernarc.assessment4.repository.CustomerRepository;
import org.northernarc.assessment4.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional // Ensures clean rollback after each integration test case
public class SecureBankingApiApplicationTests {

//    @Autowired private MockMvc mockMvc;
//    @Autowired private ObjectMapper objectMapper;
//    @Autowired private CustomerRepository customerRepository;
//    @Autowired private AccountRepository accountRepository;
//    @Autowired private TransactionRepository transactionRepository;
//
//    private Customer testCustomer;
//    private Account testAccount1;
//    private Account testAccount2;
//
//    @BeforeEach
//    void setUpData() {
//        transactionRepository.deleteAll();
//        accountRepository.deleteAll();
//        customerRepository.deleteAll();
//
//        // Seed basic reference data for integration tests
//        testCustomer = new Customer();
//        testCustomer.setCustomerName("Rahul Sharma");
//        testCustomer.setEmail("rahul@bank.com");
//        testCustomer.setPassword("$2a$10$7R9X..."); // Mock hashed password
//        testCustomer.setBranch("Chennai");
//        testCustomer = customerRepository.save(testCustomer);
//
//        testAccount1 = new Account();
//        testAccount1.setAccountNumber("ACC1001");
//        testAccount1.setAccountType("SAVINGS");
//        testAccount1.setBalance(50000.0);
//        testAccount1.setCustomer(testCustomer);
//        testAccount1 = accountRepository.save(testAccount1);
//
//        testAccount2 = new Account();
//        testAccount2.setAccountNumber("ACC1002");
//        testAccount2.setAccountType("CURRENT");
//        testAccount2.setBalance(150000.0);
//        testAccount2.setCustomer(testCustomer);
//        testAccount2 = accountRepository.save(testAccount2);
//    }
//
//    // --- TASK 1: ENTITY MAPPING TESTS ---
//    @Nested
//    @DisplayName("Task 1: Entity Relationship & Cascade Validation")
//    class EntityMappingTests {
//
//        @Test
//        @DisplayName("Should cascadingly delete Accounts and Transactions when Customer is deleted")
//        void testCascadeDeleteCustomer() {
//            Transaction tx = new Transaction();
//            tx.setAmount(500.0);
//            tx.setTransactionType("CREDIT");
//            tx.setTransactionDate(LocalDate.now());
//            tx.setAccount(testAccount1);
//            transactionRepository.save(tx);
//
//            customerRepository.delete(testCustomer);
//            customerRepository.flush();
//
//            assertThat(accountRepository.findById(testAccount1.getAccountNumber())).isEmpty();
//            assertThat(transactionRepository.findById(tx.getTransactionId())).isEmpty();
//        }
//    }
//
//    // --- TASK 2: BEAN VALIDATION TESTS ---
//    @Nested
//    @DisplayName("Task 2: Bean Validation Unit & API Constraints")
//    class ValidationTests {
//
//        @Test
//        @DisplayName("Should return 400 Bad Request when customer registrations break constraints")
//        void testCustomerValidationConstraints() throws Exception {
//            Customer invalidCustomer = new Customer();
//            invalidCustomer.setCustomerName(""); // @NotBlank validation failure
//            invalidCustomer.setEmail("not-an-email"); // @Email validation failure
//            invalidCustomer.setPassword("123"); // @Size validation failure
//
//            mockMvc.perform(post("/api/customers")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidCustomer)))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
//                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
//        }
//
//        @Test
//        @DisplayName("Should return 400 Bad Request if account balance is negative")
//        void testNegativeBalanceValidation() throws Exception {
//            Account invalidAccount = new Account();
//            invalidAccount.setAccountNumber("ACC9999");
//            invalidAccount.setAccountType("SAVINGS");
//            invalidAccount.setBalance(-100.0); // Broken @Positive/@PositiveOrZero validation
//
//            mockMvc.perform(post("/api/accounts")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalidAccount)))
//                    .andExpect(status().isBadRequest());
//        }
//    }
//
//    // --- TASK 3: SPRING DATA JPA DERIVED QUERIES TESTS ---
//    @Nested
//    @DisplayName("Task 3: Spring Data JPA Derived Query Asserts")
//    class DerivedQueryTests {
//
//        @Test
//        @DisplayName("Verify findByAccountType derived mechanism works flawlessly")
//        void testDerivedQueries() {
//            List<Account> savingsList = accountRepository.findByAccountType("SAVINGS");
//            assertThat(savingsList).hasSize(1);
//            assertThat(savingsList.get(0).getAccountNumber()).isEqualTo("ACC1001");
//
//            List<Customer> chennaiCustomers = customerRepository.findByBranch("Chennai");
//            assertThat(chennaiCustomers).hasSize(1);
//
//            List<Account> highValueBalances = accountRepository.findByBalanceGreaterThan(100000.0);
//            assertThat(highValueBalances).hasSize(1);
//            assertThat(highValueBalances.get(0).getAccountNumber()).isEqualTo("ACC1002");
//        }
//    }
//
//    // --- TASK 4: JPQL CUSTOM COMPLEX QUERIES TESTS ---
//    @Nested
//    @DisplayName("Task 4: Complex JPQL Query Operations")
//    class JpqlQueryTests {
//
//        @Test
//        @DisplayName("JPQL: Find Rich Customers exceeding a set target threshold balance")
//        void testFindRichCustomers() {
//            List<Customer> richCustomers = customerRepository.findRichCustomers(100000.0);
//            assertThat(richCustomers).hasSize(1);
//            assertThat(richCustomers.get(0).getCustomerName()).isEqualTo("Rahul Sharma");
//        }
//
//        @Test
//        @DisplayName("JPQL: Find aggregated total balances grouped per branch location")
//        void testFindTotalBalancePerBranch() {
//            List<Object[]> balancePerBranch = customerRepository.findTotalBalancePerBranch();
//            assertThat(balancePerBranch).isNotEmpty();
//            Object[] branchRow = balancePerBranch.get(0);
//            assertThat(branchRow[0]).isEqualTo("Chennai"); // Branch Name
//            assertThat((Double) branchRow[1]).isEqualTo(200000.0); // Accumulated total
//        }
//
//        @Test
//        @DisplayName("JPQL: Find customers holding multiple active bank accounts")
//        void testFindCustomersWithMultipleAccounts() {
//            List<Customer> multiAccountHolders = customerRepository.findCustomersWithMultipleAccounts();
//            assertThat(multiAccountHolders).hasSize(1); // Rahul Sharma has 2 accounts configured
//        }
//
//        @Test
//        @DisplayName("JPQL: Fetch the latest single transaction execution entity item")
//        void testFindLatestTransaction() {
//            Transaction tx1 = new Transaction();
//            tx1.setAmount(200.0);
//            tx1.setTransactionType("DEBIT");
//            tx1.setTransactionDate(LocalDate.now().minusDays(2));
//            tx1.setAccount(testAccount1);
//            transactionRepository.save(tx1);
//
//            Transaction tx2 = new Transaction();
//            tx2.setAmount(1200.0);
//            tx2.setTransactionType("CREDIT");
//            tx2.setTransactionDate(LocalDate.now()); // Modern/latest transaction
//            tx2.setAccount(testAccount1);
//            transactionRepository.save(tx2);
//
//            List<Transaction> latest = transactionRepository.findLatestTransaction(PageRequest.of(0, 1));
//            assertThat(latest).hasSize(1);
//            assertThat(latest.get(0).getAmount()).isEqualTo(1200.0);
//        }
//
//        @Test
//        @DisplayName("JPQL: Retrieve all operational accounts having zero transaction items logged")
//        void testFindAccountsWithNoTransactions() {
//            List<Account> idleAccounts = accountRepository.findAccountsWithNoTransactions();
//            // Neither account has had any transaction attached yet during this block setup execution
//            assertThat(idleAccounts).hasSize(2);
//        }
//    }
//
//    // --- TASK 5: JPQL MODIFIER UPDATE TESTS ---
//    @Nested
//    @DisplayName("Task 5: Modifying In-place JPQL Target Update Invocations")
//    class JpqlUpdateTests {
//
//        @Test
//        @DisplayName("Verify balance batch increment calculations work via custom JPQL Modifiers")
//        void testIncreaseBalance() {
//            int itemsUpdated = accountRepository.increaseBalance("ACC1001", 5000.0);
//            assertThat(itemsUpdated).isEqualTo(1);
//
//            // Fetch to ensure actual DB state update took execution place
//            accountRepository.flush();
//            Account updatedAccount = accountRepository.findById("ACC1001").orElseThrow();
//            assertThat(updatedAccount.getBalance()).isEqualTo(55000.0);
//        }
//    }
//
//    // --- TASK 6: PAGINATION & SORTING TESTS ---
//    @Nested
//    @DisplayName("Task 6: API Driven Pagination & Dynamic Sort Evaluations")
//    class PaginationTests {
//
//        @Test
//        @WithMockUser(roles = "USER")
//        @DisplayName("GET /accounts should yield sorted pages descending by raw remaining balance")
//        void testGetAccountsPaginatedAndSorted() throws Exception {
//            mockMvc.perform(get("/api/accounts")
//                            .param("page", "0")
//                            .param("size", "10")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.content", hasSize(2)))
//                    // Balance DESC: Account 2 (150000.0) must come before Account 1 (50000.0)
//                    .andExpect(jsonPath("$.content[0].accountNumber").value("ACC1002"))
//                    .andExpect(jsonPath("$.content[1].accountNumber").value("ACC1001"));
//        }
//    }
//
//    // --- TASK 7: DTO MAPPING CHECKS ---
//    @Nested
//    @DisplayName("Task 7: DTO Projection Mapping Layer Safeguards")
//    class DtoMappingTests {
//
//        @Test
//        @WithMockUser(roles = "USER")
//        @DisplayName("Verify structural serialization matches CustomerSummaryDTO expectations precisely")
//        void testCustomerSummaryDtoMapping() throws Exception {
//            mockMvc.perform(get("/api/customers/" + testCustomer.getCustomerId() + "/summary")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.customerName").value("Rahul Sharma"))
//                    .andExpect(jsonPath("$.branch").value("Chennai"))
//                    .andExpect(jsonPath("$.numberOfAccounts").value(2))
//                    .andExpect(jsonPath("$.totalBalance").value(200000.0));
//        }
//    }
//
//    // --- TASK 8: SECURITY & JWT PIECES ---
//    @Nested
//    @DisplayName("Task 8: End-to-End JWT Auth Engine Handshakes")
//    class JwtAuthenticationTests {
//
//        @Test
//        @DisplayName("POST /login with valid details must serve back a valid JWT Bearer String")
//        void testSuccessfulLogin() throws Exception {
//            // Note: This assumes service maps a testing endpoint or uses the loaded test context credentials.
//            Map<String, String> loginCredentials = Map.of(
//                    "email", "rahul@bank.com",
//                    "password", "password123"
//            );
//
//            mockMvc.perform(post("/api/auth/login")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(loginCredentials)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.token").exists())
//                    .andExpect(jsonPath("$.token").value(startsWith("eyJ"))); // Base64 JWT Header format standard match
//        }
//
//        @Test
//        @DisplayName("Accessing locked banking actions with zero authentication headers must prompt a 401 Unauthorized status")
//        void testUnauthenticatedAccessFails() throws Exception {
//            mockMvc.perform(get("/api/accounts")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isUnauthorized());
//        }
//    }
//
//    // --- TASK 9: RBAC METHOD LEVEL AUTHORIZATION ---
//    @Nested
//    @DisplayName("Task 9: Method Level Role Based Access Validation Control")
//    class AuthorizationTests {
//
//        @Test
//        @WithMockUser(roles = "USER")
//        @DisplayName("USER Role must encounter a 403 Forbidden status when attempting to clear an account")
//        void testUserCannotDeleteAccount() throws Exception {
//            mockMvc.perform(delete("/api/accounts/ACC1001")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isForbidden());
//        }
//
//        @Test
//        @WithMockUser(roles = "ADMIN")
//        @DisplayName("ADMIN Role should be permitted to clear an account successfully")
//        void testAdminCanDeleteAccount() throws Exception {
//            mockMvc.perform(delete("/api/accounts/ACC1001")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isNoContent());
//        }
//
//        @Test
//        @WithMockUser(roles = "MANAGER")
//        @DisplayName("MANAGER Role should be permitted to perform structural configuration balance operations")
//        void testManagerCanUpdateAccount() throws Exception {
//            mockMvc.perform(put("/api/accounts/ACC1002/balance")
//                            .param("amount", "1000.0")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isOk());
//        }
//    }
//
//    // --- TASK 10: ADVICE EXCEPTION CORES ---
//    @Nested
//    @DisplayName("Task 10: Global Exception Interception Handlers")
//    class ExceptionHandlingTests {
//
//        @Test
//        @WithMockUser(roles = "USER")
//        @DisplayName("Should translate a missing customer query into a normalized 404 Entity Not Found layout")
//        void testCustomerNotFoundExceptionHandling() throws Exception {
//            mockMvc.perform(get("/api/customers/999999/summary")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isNotFound())
//                    .andExpect(jsonPath("$.status").value("NOT_FOUND"))
//                    .andExpect(jsonPath("$.message").exists());
//        }
//    }
//
//    // --- FINAL CHALLENGE: PERFORMANCE-OPTIMIZED METRICS DASHBOARD ---
//    @Nested
//    @DisplayName("Final Challenge: Highly Optimized Single Execution Operations Dashboard Metrics")
//    class FinalChallengeTests {
//
//        @Test
//        @WithMockUser(roles = "USER")
//        @DisplayName("GET /dashboard must correctly resolve complex aggregate metric fields without spawning sequential N+1 data queries")
//        void testGetDashboardPerformanceMetrics() throws Exception {
//            // Setup specialized matching data array metrics to thoroughly evaluate calculation assertions
//            mockMvc.perform(get("/api/dashboard")
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.totalCustomers").value(greaterThanOrEqualTo(1)))
//                    .andExpect(jsonPath("$.totalAccounts").value(greaterThanOrEqualTo(2)))
//                    .andExpect(jsonPath("$.totalBalance").value(200000.0))
//                    .andExpect(jsonPath("$.topBranch").value("Chennai"))
//                    .andExpect(jsonPath("$.highestBalanceCustomer").value("Rahul Sharma"));
//        }
//    }
}