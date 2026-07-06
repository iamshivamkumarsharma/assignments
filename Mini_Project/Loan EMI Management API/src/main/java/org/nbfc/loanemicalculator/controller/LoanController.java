package org.nbfc.loanemicalculator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nbfc.loanemicalculator.dto.CreateLoanRequest;
import org.nbfc.loanemicalculator.dto.CustomerSummaryDTO;
import org.nbfc.loanemicalculator.dto.DashboardDTO;
import org.nbfc.loanemicalculator.dto.LoanDetailDTO;
import org.nbfc.loanemicalculator.dto.PayEmiRequest;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.nbfc.loanemicalculator.entity.Loan;
import org.nbfc.loanemicalculator.entity.PenaltyTransaction;
import org.nbfc.loanemicalculator.enums.EmiStatus;
import org.nbfc.loanemicalculator.enums.LoanType;
import org.nbfc.loanemicalculator.enums.PaymentMode;
import org.nbfc.loanemicalculator.service.LoanService;
import org.nbfc.loanemicalculator.validation.ValueOfEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Validated
@RequiredArgsConstructor
@Tag(name = "Loans & Analytics", description = "Loan catalog, customers, penalties and dashboard. Requires a JWT.")
public class LoanController {

    private final LoanService loanService;

    @GetMapping("/loans")
    @Operation(summary = "List loans (paged)", description = "Returns loans sorted by interest rate DESC by default.")
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    public Page<Loan> getLoans(@PageableDefault(sort = "interestRate", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Listing loans page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return loanService.getLoans(pageable);
    }

    @GetMapping("/loans/details")
    @Operation(summary = "List loans with their EMI schedule",
            description = "Fetched with a single JOIN FETCH query, demonstrating an N+1-free read.")
    public List<LoanDetailDTO> loanDetails() {
        return loanService.getLoanDetails();
    }

    @PostMapping("/loans")
    @Operation(summary = "Create a loan", description = "MANAGER or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Requires MANAGER or ADMIN role")
    })
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Loan> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        Loan created = loanService.createLoan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/loans/type/{loanType}")
    public List<Loan> byType(@PathVariable @ValueOfEnum(enumClass = LoanType.class) String loanType) {
        return loanService.findByLoanType(loanType);
    }

    @GetMapping("/loans/interest")
    public List<Loan> byInterest(@RequestParam @Positive Double rate) {
        return loanService.findByInterestRateGreaterThan(rate);
    }

    @GetMapping("/loans/no-overdue")
    public List<Loan> noOverdue() {
        return loanService.findLoansWithoutOverdue();
    }

    @GetMapping("/emis/status/{status}")
    public List<EmiSchedule> emisByStatus(@PathVariable @ValueOfEnum(enumClass = EmiStatus.class) String status) {
        return loanService.findByStatus(status);
    }

    @GetMapping("/emis")
    @Operation(summary = "List EMIs by status (paged & sorted)")
    public Page<EmiSchedule> emisPaged(
            @RequestParam @ValueOfEnum(enumClass = EmiStatus.class) String status,
            @PageableDefault(sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return loanService.getEmisByStatus(status, pageable);
    }

    @PostMapping("/emis/{emiId}/pay")
    @Operation(summary = "Settle an EMI installment",
            description = "Marks the EMI PAID and, if it was overdue, records a penalty atomically in one transaction.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "EMI settled"),
            @ApiResponse(responseCode = "404", description = "EMI not found")
    })
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    public EmiSchedule payEmi(@PathVariable Long emiId, @Valid @RequestBody PayEmiRequest request) {
        return loanService.payEmi(emiId, request);
    }

    @GetMapping("/customers/branch/{branchName}")
    public List<Customer> byBranch(@PathVariable String branchName) {
        return loanService.findByBranchName(branchName);
    }

    @GetMapping("/customers/high-value")
    public List<Customer> highValue(@RequestParam @PositiveOrZero long count) {
        return loanService.findHighValueBorrowers(count);
    }

    @GetMapping("/customers/multi-loan")
    public List<Customer> multiLoan() {
        return loanService.findCustomersWithMultipleLoanTypes();
    }

    @GetMapping("/customers/{id}/summary")
    public CustomerSummaryDTO summary(@PathVariable Long id) {
        return loanService.getCustomerSummary(id);
    }

    @GetMapping("/penalties/mode/{paymentMode}")
    public List<PenaltyTransaction> byMode(@PathVariable @ValueOfEnum(enumClass = PaymentMode.class) String paymentMode) {
        return loanService.findByPaymentMode(paymentMode);
    }

    @GetMapping("/penalties")
    @Operation(summary = "List penalties by payment mode (paged & sorted)")
    public Page<PenaltyTransaction> penaltiesPaged(
            @RequestParam @ValueOfEnum(enumClass = PaymentMode.class) String paymentMode,
            @PageableDefault(sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return loanService.getPenaltiesByMode(paymentMode, pageable);
    }

    @GetMapping("/penalties/latest")
    public PenaltyTransaction latest() {
        return loanService.findLatestPenalty();
    }

    @GetMapping("/penalties/per-branch")
    public List<Object[]> perBranch() {
        return loanService.findBranchWisePenaltyCollection();
    }

    @PutMapping("/loans/interest-rate")
    @Operation(summary = "Increase interest rate for a loan type", description = "MANAGER only bulk update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loans updated"),
            @ApiResponse(responseCode = "403", description = "Requires MANAGER role")
    })
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> increaseRates(
            @RequestParam @ValueOfEnum(enumClass = LoanType.class) String loanType,
            @RequestParam @Positive double amount) {
        int updated = loanService.increaseInterestRate(loanType, amount);
        return ResponseEntity.ok("Updated " + updated + " loans");
    }

    @DeleteMapping("/loans/{loanId}")
    @Operation(summary = "Delete a loan", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan deleted"),
            @ApiResponse(responseCode = "404", description = "Loan not found"),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteLoan(@Parameter(description = "Loan id") @PathVariable Long loanId) {
        loanService.deleteLoan(loanId);
        return ResponseEntity.ok("Deleted");
    }

    @DeleteMapping("/penalties/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deletePenalty(@PathVariable Long transactionId) {
        loanService.deletePenalty(transactionId);
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Platform analytics dashboard")
    public DashboardDTO dashboard() {
        return loanService.getDashboard();
    }
}
