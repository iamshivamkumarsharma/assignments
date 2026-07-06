# Coding Assessment: Secure Loan EMI Management API – Spring Boot Assessment

(Spring Boot + Spring Data JPA + JPQL + JWT)
Expected Duration: 120 minutes
Maximum Marks: 400 (200 Functional + 200 Hidden test cases)

A leading NBFC is building a secure REST API to manage customer loans, EMI schedules, EMI payments,
overdue penalties, and branch-wise lending analytics. The project is partially implemented; participants
complete the missing service methods, repository queries, security config, exception handlers, and REST
endpoints to deliver a secure, production-ready API.

## Entities
- Customer: customerId, customerName, email, password, branchName
- Loan: loanId, loanType (HOME/PERSONAL/VEHICLE/EDUCATION), principalAmount, interestRate, loanTenureMonths, monthlyEmi, loanStatus (ACTIVE/CLOSED/DEFAULTED)
- EmiSchedule: emiId, dueDate, paymentDate, emiAmount, status (PENDING/PAID/OVERDUE), @ManyToOne Customer, @ManyToOne Loan
- PenaltyTransaction: transactionId, penaltyAmount, paymentMode (CASH/CARD/ONLINE), paymentDate, @ManyToOne Loan

Relationships: Customer 1—* EmiSchedule *—1 Loan; Loan 1—* PenaltyTransaction.

## Tasks
1. Entity mapping (10) — @OneToMany/@ManyToOne, mappedBy, Cascade, FetchType, safe collection init.
2. Validation (20) — @NotBlank/@Email/@Size/@Positive/@PositiveOrZero/@NotNull.
3. Derived queries (15) — findByLoanType, findByBranchName, findByPaymentMode, findByInterestRateGreaterThan, findByStatus.
4. JPQL (50) — high value borrowers, branch-wise penalty collection, multi-loan-type customers, latest penalty, loans without overdue EMIs.
5. JPQL update (10) — increaseInterestRate() with @Modifying/@Transactional.
6. Pagination & sorting (10) — GET /loans, default interestRate DESC.
7. DTO (10) — CustomerSummaryDTO(customerName, branchName, numberOfLoans, totalPenaltyPaid).
8. JWT (25) — UserDetailsService, AuthenticationManager, PasswordEncoder, JwtFilter, JwtUtil, SecurityConfig; /login permitAll.
9. Roles (10) — @PreAuthorize ADMIN delete, MANAGER update rates, USER view only.
10. Global exceptions (20) — Customer/Loan/Emi/PenaltyNotFound, MethodArgumentNotValid, Validation, IllegalArgument, AccessDenied.

## Final Challenge (40)
GET /dashboard → totalCustomers, totalLoans, totalPenaltyCollected, topPerformingBranch,
highestPenaltyPayingCustomer, totalOverdueEmis. Minimum optimized JPQL, no N+1.

## Running tests
`./mvnw test` — uses in-memory H2; 23 cases cover auth, queries, pagination, DTO, roles, exceptions, dashboard.
