package org.nbfc.assignment3.controller;

import org.nbfc.assignment3.model.LoanApplication;
import org.nbfc.assignment3.service.LendingAnalytics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/analytics")
public class LendingAnalyticsController {

    private final LendingAnalytics lendingAnalytics;

    public LendingAnalyticsController(LendingAnalytics lendingAnalytics) {
        this.lendingAnalytics = lendingAnalytics;
    }

    @GetMapping("/top-credit-profiles")
    public List<LoanApplication> topCreditProfiles(@RequestParam(defaultValue = "5") int n) {
        return lendingAnalytics.topCreditProfiles(n);
    }

    @GetMapping("/average-loan-amount-by-type")
    public Map<String, Double> averageLoanAmountByType() {
        return lendingAnalytics.averageLoanAmountByType();
    }

    @GetMapping("/highest-loan-application")
    public Optional<LoanApplication> highestLoanApplication() {
        return lendingAnalytics.highestLoanApplication();
    }

    @GetMapping("/lenders-with-multiple-loan-types")
    public Set<String> lendersWithMultipleLoanTypes() {
        return lendingAnalytics.lendersWithMultipleLoanTypes();
    }

    @GetMapping("/group-applications-by-lender")
    public Map<String, List<LoanApplication>> groupApplicationsByLender() {
        return lendingAnalytics.groupApplicationsByLender();
    }

    @GetMapping("/suspicious-applications")
    public List<String> suspiciousApplications() {
        return lendingAnalytics.suspiciousApplications();
    }

    @GetMapping("/loan-type-wise-top-applicant-by-lender")
    public Map<String, Map<String, Optional<LoanApplication>>> loanTypeWiseTopApplicantByLender() {
        return lendingAnalytics.loanTypeWiseTopApplicantByLender();
    }
}

