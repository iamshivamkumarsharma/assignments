package org.nbfc.loanemicalculator.serviceimpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nbfc.loanemicalculator.config.CachingConfig;
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
import org.nbfc.loanemicalculator.exception.CustomerNotFoundException;
import org.nbfc.loanemicalculator.exception.EmiNotFoundException;
import org.nbfc.loanemicalculator.exception.LoanNotFoundException;
import org.nbfc.loanemicalculator.exception.PenaltyNotFoundException;
import org.nbfc.loanemicalculator.mapper.EntityMapper;
import org.nbfc.loanemicalculator.repository.CustomerRepository;
import org.nbfc.loanemicalculator.repository.EmiScheduleRepository;
import org.nbfc.loanemicalculator.repository.LoanRepository;
import org.nbfc.loanemicalculator.repository.PenaltyTransactionRepository;
import org.nbfc.loanemicalculator.service.LoanService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final PenaltyTransactionRepository penaltyTransactionRepository;

    @Override
    public List<Loan> findByLoanType(String loanType) {
        return loanRepository.findByLoanType(loanType);
    }

    @Override
    public List<Customer> findByBranchName(String branchName) {
        return customerRepository.findByBranchName(branchName);
    }

    @Override
    public List<PenaltyTransaction> findByPaymentMode(String paymentMode) {
        return penaltyTransactionRepository.findByPaymentMode(paymentMode);
    }

    @Override
    public List<Loan> findByInterestRateGreaterThan(Double rate) {
        return loanRepository.findByInterestRateGreaterThan(rate);
    }

    @Override
    public List<EmiSchedule> findByStatus(String status) {
        return emiScheduleRepository.findByStatus(status);
    }

    @Override
    public List<Customer> findHighValueBorrowers(long count) {
        return customerRepository.findHighValueBorrowers(count);
    }

    @Override
    public List<Object[]> findBranchWisePenaltyCollection() {
        return customerRepository.findBranchWisePenaltyCollection();
    }

    @Override
    public List<Customer> findCustomersWithMultipleLoanTypes() {
        return customerRepository.findCustomersWithMultipleLoanTypes();
    }

    @Override
    public PenaltyTransaction findLatestPenalty() {
        List<PenaltyTransaction> list = penaltyTransactionRepository.findLatest(PageRequest.of(0, 1));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Loan> findLoansWithoutOverdue() {
        return loanRepository.findLoansWithoutOverdue();
    }

    @Override
    @Transactional
    @CacheEvict(value = CachingConfig.DASHBOARD_CACHE, allEntries = true)
    public int increaseInterestRate(String loanType, double amount) {
        int updated = loanRepository.increaseInterestRate(loanType, amount);
        log.info("Increased interest rate by {} for {} {} loan(s)", amount, updated, loanType);
        return updated;
    }

    @Override
    public Page<Loan> getLoans(Pageable pageable) {
        return loanRepository.findAll(pageable);
    }

    @Override
    @Cacheable(value = CachingConfig.CUSTOMER_SUMMARY_CACHE, key = "#customerId")
    public CustomerSummaryDTO getCustomerSummary(Long customerId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " does not exist."));
        Long loans = customerRepository.countLoans(customerId);
        Double penalty = customerRepository.totalPenaltyPaid(customerId);
        log.debug("Computed summary for customer {}: loans={}, penalty={}", customerId, loans, penalty);
        return new CustomerSummaryDTO(c.getCustomerName(), c.getBranchName(), loans, penalty);
    }

    @Override
    @Transactional
    @CacheEvict(value = CachingConfig.DASHBOARD_CACHE, allEntries = true)
    public void deleteLoan(Long loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new LoanNotFoundException("Loan with ID " + loanId + " does not exist.");
        }
        loanRepository.deleteById(loanId);
        log.info("Deleted loan {}", loanId);
    }

    @Override
    @Transactional
    @CacheEvict(value = CachingConfig.DASHBOARD_CACHE, allEntries = true)
    public void deletePenalty(Long transactionId) {
        if (!penaltyTransactionRepository.existsById(transactionId)) {
            throw new PenaltyNotFoundException("Penalty with ID " + transactionId + " does not exist.");
        }
        penaltyTransactionRepository.deleteById(transactionId);
        log.info("Deleted penalty {}", transactionId);
    }

    @Override
    @Cacheable(CachingConfig.DASHBOARD_CACHE)
    public DashboardDTO getDashboard() {
        long totalCustomers = customerRepository.count();
        long totalLoans = loanRepository.count();
        Double totalPenalty = penaltyTransactionRepository.getTotalPenaltyCollected();
        List<String> branches = customerRepository.findTopBranches(PageRequest.of(0, 1));
        List<String> customers = customerRepository.findTopPenaltyPayingCustomers(PageRequest.of(0, 1));
        Long overdue = emiScheduleRepository.countOverdue();
        return new DashboardDTO(
                totalCustomers,
                totalLoans,
                totalPenalty,
                branches.isEmpty() ? null : branches.get(0),
                customers.isEmpty() ? null : customers.get(0),
                overdue);
    }

    // --- Advanced fetching / pagination ---

    @Override
    public List<LoanDetailDTO> getLoanDetails() {
        // Single JOIN FETCH query -> no N+1 when reading each loan's EMI schedule.
        List<Loan> loans = loanRepository.findAllWithSchedules();
        log.debug("Loaded {} loans with schedules in one query", loans.size());
        return loans.stream().map(EntityMapper::toLoanDetail).toList();
    }

    @Override
    public Page<EmiSchedule> getEmisByStatus(String status, Pageable pageable) {
        return emiScheduleRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<PenaltyTransaction> getPenaltiesByMode(String paymentMode, Pageable pageable) {
        return penaltyTransactionRepository.findByPaymentMode(paymentMode, pageable);
    }

    // --- Write operations (transactional) ---

    @Override
    @Transactional
    @CacheEvict(value = CachingConfig.DASHBOARD_CACHE, allEntries = true)
    public Loan createLoan(CreateLoanRequest request) {
        Loan loan = new Loan();
        loan.setLoanType(request.getLoanType());
        loan.setPrincipalAmount(request.getPrincipalAmount());
        loan.setInterestRate(request.getInterestRate());
        loan.setLoanTenureMonths(request.getLoanTenureMonths());
        loan.setMonthlyEmi(request.getMonthlyEmi());
        loan.setLoanStatus(request.getLoanStatus());
        Loan saved = loanRepository.save(loan);
        log.info("Created {} loan {} (principal={})", saved.getLoanType(), saved.getLoanId(), saved.getPrincipalAmount());
        return saved;
    }

    /**
     * Settles an EMI and, when it was overdue, records a penalty in the SAME transaction.
     * Either both changes commit or, on any failure, both roll back.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CachingConfig.DASHBOARD_CACHE, allEntries = true),
            @CacheEvict(value = CachingConfig.CUSTOMER_SUMMARY_CACHE, allEntries = true)
    })
    public EmiSchedule payEmi(Long emiId, PayEmiRequest request) {
        EmiSchedule emi = emiScheduleRepository.findById(emiId)
                .orElseThrow(() -> new EmiNotFoundException("EMI with ID " + emiId + " does not exist."));

        boolean wasOverdue = EmiStatus.OVERDUE.name().equalsIgnoreCase(emi.getStatus());
        emi.setStatus(EmiStatus.PAID.name());
        emi.setPaymentDate(LocalDate.now());
        EmiSchedule saved = emiScheduleRepository.save(emi);

        double penaltyAmount = request.getPenaltyAmount() == null ? 0.0 : request.getPenaltyAmount();
        if (wasOverdue && penaltyAmount > 0) {
            PenaltyTransaction penalty = new PenaltyTransaction();
            penalty.setLoan(emi.getLoan());
            penalty.setPenaltyAmount(penaltyAmount);
            penalty.setPaymentMode(request.getPaymentMode());
            penalty.setPaymentDate(LocalDate.now());
            penaltyTransactionRepository.save(penalty);
            log.info("EMI {} was overdue; recorded penalty of {} via {}", emiId, penaltyAmount, request.getPaymentMode());
        }
        log.info("Settled EMI {} (mode={})", emiId, request.getPaymentMode());
        return saved;
    }
}
