# Loan EMI Calculator — Secure Loan EMI Management API

A production‑style Spring Boot REST API for a Non‑Banking Financial Company (NBFC). It manages customers,
loans, EMI schedules, overdue penalty payments, and branch‑wise lending analytics behind JWT‑based
authentication and role‑based authorization.

This repository is the **fully implemented reference solution** for the assessment described in
[`PROBLEM.md`](./PROBLEM.md). All ten tasks plus the analytics Final Challenge are completed, and a
64‑case unit + integration test suite verifies every feature against an in‑memory database.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [How to Build & Run](#how-to-build--run)
3. [Running the Tests](#running-the-tests)
4. [Domain Model](#domain-model)
5. [Project Structure & Package Walkthrough](#project-structure--package-walkthrough)
6. [Security Architecture](#security-architecture)
7. [REST API Reference](#rest-api-reference)
8. [Test Scenarios (23 cases)](#test-scenarios-23-cases)
9. [Design Notes & Gotchas](#design-notes--gotchas)

---

## Tech Stack

| Concern         | Technology                          |
|-----------------|-------------------------------------|
| Language        | Java 17                             |
| Framework       | Spring Boot 4.x                     |
| Persistence     | Spring Data JPA + Hibernate         |
| DB (runtime)    | PostgreSQL                          |
| DB (tests)      | H2 in‑memory (PostgreSQL mode)      |
| Security        | Spring Security + JWT (jjwt 0.12.x) |
| Validation      | Jakarta Bean Validation             |
| Caching         | Spring Cache (simple, prod) / no‑op (tests) |
| Monitoring      | Spring Boot Actuator                |
| API Docs        | springdoc‑openapi (Swagger UI)      |
| Testing         | JUnit 5 + Mockito + AssertJ + MockMvc |
| Boilerplate     | Lombok                              |
| Build           | Maven (wrapper included)            |

---

## Industry-Standard Features Added

On top of the reference solution, the following production-grade capabilities were layered in:

### Advanced data fetching
- **Pagination & sorting** — `GET /loans`, `GET /emis`, `GET /penalties` return `Page<T>` with
  sensible default sorts. Page JSON uses Spring Data's stable `VIA_DTO` structure
  (`content` + `page`).
- **N+1 problem solved** — `GET /loans/details` loads every loan *with* its EMI schedule in a
  single `LEFT JOIN FETCH` query (`LoanRepository.findAllWithSchedules`), plus `@EntityGraph`
  finders (`findWithSchedulesByLoanId`, `findWithCustomerAndLoanByStatus`,
  `findWithEmiSchedulesByCustomerId`) for targeted eager loading.
- **Lazy loading, controlled** — all `@ManyToOne`/`@OneToMany` stay `LAZY`;
  `spring.jpa.open-in-view=false` forces lazy access to happen inside transactional service
  methods, never during view rendering.
- **Custom query methods** — derived queries (`findByLoanStatus`,
  `findByLoanTypeAndLoanStatus`, `findByCustomer_CustomerId`,
  `findByPenaltyAmountGreaterThan`) alongside the existing JPQL.

### Transaction management
- `LoanServiceImpl` is `@Transactional(readOnly = true)` by default; every write method
  overrides with a read‑write `@Transactional`.
- `POST /emis/{emiId}/pay` is a genuine multi‑step atomic unit: it marks the EMI `PAID` **and**,
  when the installment was overdue, records a penalty in the *same* transaction — both commit or
  both roll back.

### Robust API layering
- **Jakarta Validation** — request DTOs (`CreateLoanRequest`, `PayEmiRequest`) plus method‑level
  `@Validated` parameter validation, and a reusable custom constraint `@ValueOfEnum` that checks
  string inputs against enum names.
- **Global exception handling** — `@RestControllerAdvice` returns a consistent `ErrorResponse`
  (timestamp, status, error, message, path, and per‑field errors) for not‑found, bean‑validation,
  constraint‑violation, unreadable‑body, data‑integrity, access‑denied and uncaught errors.
- **Logging** — SLF4J (`@Slf4j`) across controllers, service, JWT filter and the exception
  handler, with levels/pattern configured in `application.properties`.

### Other production features
- **JPA auditing** — `createdAt` / `updatedAt` populated automatically via
  `@EnableJpaAuditing`.
- **Caching** — `@Cacheable` on the dashboard and customer summary, `@CacheEvict` on writes
  (no‑op during tests so results are always fresh).
- **Actuator** — `/actuator/health`, `/info`, `/metrics`, `/loggers` exposed (health/info public).
- **Safer auth** — registration returns a password‑free `CustomerResponse` and always assigns
  the `USER` role server‑side (no client role self‑assignment).

### Latest additions

- **Swagger / OpenAPI UI** — interactive docs at `http://localhost:8080/swagger-ui/index.html`
  (raw spec at `/v3/api-docs` and `/v3/api-docs.yaml`); the JWT **Authorize** button is wired via a
  `bearerAuth` security scheme. These paths are public and skipped by the JWT filter.
- **Per‑request logging** — `RequestLoggingFilter` (a highest‑precedence `OncePerRequestFilter`)
  logs `METHOD URI -> status (ms)` for every request, wrapping Spring Security.
- **`DuplicateEmailException`** — `POST /register` with an already‑registered email now returns a
  clean `409 Conflict` via the global handler instead of a generic data‑integrity error.
- **More tests (now 64)** — added service‑layer unit tests, a standalone‑MockMvc controller‑slice
  test (`LoanControllerUnitTest`), and a full‑context MockMvc test (`AuthAndDocsMockMvcTest`)
  covering the 409 path and the public docs endpoint.

---

## How to Build & Run

The runtime profile targets PostgreSQL. Create a database named `loanemi` (or edit
`src/main/resources/application.properties`).

```bash
# compile only
./mvnw -DskipTests compile

# run the app
./mvnw spring-boot:run
```

Default datasource (override via env/props as needed):

```
spring.datasource.url=jdbc:postgresql://localhost:5432/loanemi
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
```

---

## Running the Tests

Tests use an in‑memory H2 database (see `src/test/resources/application.properties`) so **no PostgreSQL is
required**:

```bash
./mvnw test
```

Expected result: `Tests run: 64, Failures: 0, Errors: 0`.

The suite is split into fast **unit tests** (pure JUnit 5 + Mockito, no Spring context) and
**integration tests** (`@SpringBootTest` + MockMvc):

| Test class | Kind | What it covers |
|------------|------|----------------|
| `LoanServiceImplTest` | unit (Mockito) | service business logic, `payEmi` transaction branches, N+1‑free mapping |
| `JwtUtilTest` | unit | token generation, parsing, tamper/expiry validation |
| `ValueOfEnumValidatorTest` | unit | the custom `@ValueOfEnum` constraint |
| `EntityMapperTest` | unit | entity → DTO mapping (password never leaks) |
| `LoanEmiCalculatorApplicationTests` | integration | the original 23 end‑to‑end scenarios |
| `NewFeaturesIntegrationTest` | integration | loan creation, pay‑EMI, paged/detail reads, enum validation |
| `LoanControllerUnitTest` | unit (standalone MockMvc) | controller mapping/JSON/status with a mocked service |
| `AuthAndDocsMockMvcTest` | integration | duplicate‑email 409 + public OpenAPI docs endpoint |

---

## Domain Model

```
Customer 1 ─────< * EmiSchedule * >───── 1 Loan 1 ─────< * PenaltyTransaction
```

* A **Customer** can have many **EmiSchedule** rows (their repayment history).
* A **Loan** can have many **EmiSchedule** rows and many **PenaltyTransaction** rows.
* A customer is linked to loans indirectly through their EMI schedules.

### Entities

| Entity              | Key fields                                                                                  |
|---------------------|---------------------------------------------------------------------------------------------|
| `Customer`          | customerId, customerName, email (unique), password, branchName, role, emiSchedules          |
| `Loan`              | loanId, loanType, principalAmount, interestRate, loanTenureMonths, monthlyEmi, loanStatus   |
| `EmiSchedule`       | emiId, dueDate, paymentDate, emiAmount, status (PENDING/PAID/OVERDUE), customer, loan        |
| `PenaltyTransaction`| transactionId, penaltyAmount, paymentMode (CASH/CARD/ONLINE), paymentDate, loan             |

Loan types: `HOME`, `PERSONAL`, `VEHICLE`, `EDUCATION`. Loan status: `ACTIVE`, `CLOSED`, `DEFAULTED`.

---

## Project Structure & Package Walkthrough

```
src/main/java/org/nbfc/loanemicalculator
├── LoanEmiCalculatorApplication.java   # Spring Boot entry point
├── entity/                             # JPA entities (mapping + validation)
├── repository/                         # Spring Data JPA repos + JPQL queries
├── service/                            # service interface
├── serviceimpl/                        # service implementation (business logic)
├── controller/                         # REST controllers
├── dto/                                # request/response projections
├── exception/                          # custom exceptions + global handler
└── security/                           # JWT + Spring Security config
```

### `entity/`
JPA‑mapped domain classes. Relationships use `@OneToMany(mappedBy=…, cascade=ALL)` and lazy `@ManyToOne`.
Collections are initialized to `new ArrayList<>()` to avoid `NullPointerException`. Bean Validation
annotations (`@NotBlank`, `@Email`, `@Size`, `@Positive`, `@PositiveOrZero`, `@NotNull`) enforce data
rules. `@JsonIgnore` is placed on the inverse relationship sides to stop infinite JSON recursion.

* `Customer.java` — owner of EMI schedules; `email` is unique; `role` defaults to `USER`.
* `Loan.java` — loan account with rate/tenure/EMI; owns EMI schedules and penalties.
* `EmiSchedule.java` — one installment, links a customer and a loan.
* `PenaltyTransaction.java` — a recorded overdue/penalty payment on a loan.

### `repository/`
Spring Data JPA interfaces. Contains derived queries, custom JPQL, an aggregation update, and dashboard
aggregates.

* `CustomerRepository` — `findByBranchName`, `findByEmail`, high‑value borrowers, multi‑loan‑type customers,
  branch‑wise penalty totals, top branch/customer, loan count + penalty totals per customer.
* `LoanRepository` — `findByLoanType`, `findByInterestRateGreaterThan`, loans‑without‑overdue (LEFT‑JOIN
  style), and `increaseInterestRate` (`@Modifying`+`@Transactional`).
* `EmiScheduleRepository` — `findByStatus`, count overdue.
* `PenaltyTransactionRepository` — `findByPaymentMode`, latest penalty, total collected.

### `service/` + `serviceimpl/`
`LoanService` is the contract; `LoanServiceImpl` is the implementation using constructor injection. It maps
entities to DTOs, throws domain exceptions when records are missing, and assembles the dashboard with a
minimal number of aggregate queries (no N+1 loops).

### `controller/`
* `AuthController` — `POST /register` (BCrypt‑hashes password) and `POST /login` (returns a JWT).
* `LoanController` — loan/customer/penalty/EMI read endpoints, paginated `/loans`, summary DTO, rate update,
  delete, and `/dashboard`. Secured with `@PreAuthorize`.

### `dto/`
* `CustomerSummaryDTO` — customerName, branchName, numberOfLoans, totalPenaltyPaid.
* `DashboardDTO` — totals, top branch, top penalty‑paying customer, overdue count.
* `LoginRequest` — email + password payload.

### `exception/`
Four `NotFound` exceptions plus `GlobalExceptionHandler` (`@ControllerAdvice`) returning standardized JSON
(`timestamp`, `status`, `error`, `message`, `path`) for not‑found, validation, illegal‑argument, and
access‑denied cases.

### `security/`
* `JwtUtil` — generates/parses/validates HS256 tokens with a `role` claim.
* `JwtFilter` — `OncePerRequestFilter` that authenticates from `Authorization: Bearer …`.
* `AppConfig` — `UserDetailsService` + `PasswordEncoder` (split out to avoid a circular bean dependency).
* `SecurityConfig` — stateless filter chain, `/login` & `/register` public, everything else authenticated,
  method security enabled.

---

## Security Architecture

1. `POST /login` validates credentials and returns a signed JWT (subject=email, claim=role).
2. Clients send `Authorization: Bearer <token>` on all other requests.
3. `JwtFilter` validates the token and sets a `ROLE_<role>` authority in the security context.
4. `@PreAuthorize` enforces: **ADMIN** deletes loans/penalties, **MANAGER** updates rates, **USER** views.

---

## REST API Reference

| Method | Path                          | Role        | Description                          |
|--------|-------------------------------|-------------|--------------------------------------|
| POST   | /register                     | public      | Create customer (password hashed)    |
| POST   | /login                        | public      | Authenticate → JWT                   |
| GET    | /loans                        | any auth    | Paged loans, default interest DESC   |
| GET    | /loans/details                | any auth    | Loans + EMI schedule (single fetch, N+1‑free) |
| POST   | /loans                        | MANAGER/ADMIN | Create a loan (validated)          |
| GET    | /loans/type/{loanType}        | any auth    | Loans by type                        |
| GET    | /loans/interest?rate=         | any auth    | Loans with interest > rate           |
| GET    | /loans/no-overdue             | any auth    | Loans with no overdue EMIs           |
| GET    | /emis/status/{status}         | any auth    | EMIs by status                       |
| GET    | /emis?status=                 | any auth    | Paged & sorted EMIs by status        |
| POST   | /emis/{emiId}/pay             | any auth    | Settle EMI (+penalty) atomically     |
| GET    | /customers/branch/{branch}    | any auth    | Customers in a branch                |
| GET    | /customers/high-value?count=  | any auth    | Borrowers with > count loans         |
| GET    | /customers/multi-loan         | any auth    | Customers with multiple loan types   |
| GET    | /customers/{id}/summary       | any auth    | CustomerSummaryDTO                    |
| GET    | /penalties/mode/{mode}        | any auth    | Penalties by payment mode            |
| GET    | /penalties?paymentMode=       | any auth    | Paged & sorted penalties by mode     |
| GET    | /penalties/latest             | any auth    | Most recent penalty                  |
| GET    | /penalties/per-branch         | any auth    | Branch‑wise penalty totals           |
| PUT    | /loans/interest-rate          | MANAGER     | Bulk increase rate for a loan type   |
| DELETE | /loans/{loanId}               | ADMIN       | Delete a loan                        |
| DELETE | /penalties/{transactionId}    | ADMIN       | Delete a penalty                     |
| GET    | /dashboard                    | any auth    | Aggregated analytics (cached)        |
| GET    | /actuator/health              | public      | Liveness/health probe                |
| GET    | /actuator/metrics, /info      | any auth/public | Runtime metrics & app info       |

---

## Test Scenarios (23 cases)

All in `src/test/java/org/nbfc/loanemicalculator/LoanEmiCalculatorApplicationTests.java`. Each test seeds 3
customers (USER/MANAGER/ADMIN), 3 loans (HOME/VEHICLE/EDUCATION), 4 EMI schedules, and 2 penalties, then
calls the API through MockMvc.

| # | Test | Verifies |
|---|------|----------|
| 1 | `loginSuccess` | Valid credentials return a JWT |
| 2 | `loginInvalid` | Wrong password → 401 |
| 3 | `protectedRequiresToken` | No token → 403 on `/loans` |
| 4 | `loansSortedByInterestDesc` | Pagination + default sort interest DESC |
| 5 | `byLoanType` | Derived query by loan type |
| 6 | `byInterestGreaterThan` | Derived query interest > rate |
| 7 | `byBranch` | Customers by branch |
| 8 | `penaltiesByMode` | Penalties by payment mode |
| 9 | `emisByStatus` | EMIs filtered by OVERDUE |
| 10 | `highValueBorrowers` | JPQL high‑value borrowers |
| 11 | `multiLoanCustomers` | JPQL distinct loan‑type customers |
| 12 | `latestPenalty` | JPQL latest penalty (ORDER BY + limit) |
| 13 | `loansNoOverdue` | LEFT‑JOIN loans without overdue EMIs |
| 14 | `penaltiesPerBranch` | GROUP BY / SUM branch totals |
| 15 | `customerSummary` | DTO projection + distinct penalty sum |
| 16 | `managerUpdatesRates` | MANAGER bulk rate update applies |
| 17 | `userCannotUpdateRates` | USER blocked from update → 403 |
| 18 | `adminDeletesLoan` | ADMIN delete succeeds |
| 19 | `userCannotDelete` | USER blocked from delete → 403 |
| 20 | `loanNotFound` | Missing loan → 404 |
| 21 | `customerSummaryNotFound` | Missing customer → 404 |
| 22 | `validationFails` | Invalid payload → 400 |
| 23 | `dashboard` | Aggregated dashboard correctness |

Coverage map: Auth (1–3), Pagination/Sorting (4), Derived queries (5–9), JPQL (10–14), DTO (15),
Authorization (16–19), Exceptions (20–22), Analytics (23).

---

## Design Notes & Gotchas

* **JWT bean cycle** — `UserDetailsService`/`PasswordEncoder` live in `AppConfig`, not `SecurityConfig`, to
  break a `JwtFilter ↔ SecurityConfig` circular dependency.
* **JSON recursion** — `@JsonIgnore` on inverse relations prevents infinite serialization loops.
* **Penalty sums** — per‑customer totals use a subquery on distinct loans so duplicate EMI rows don’t
  inflate the amount.
* **Spring Boot 4** — MockMvc auto‑config import is `org.springframework.boot.webmvc.test.autoconfigure`.
* **Tests** — `@SpringBootTest` is non‑transactional, so seed data is cleared in `@BeforeEach`.
