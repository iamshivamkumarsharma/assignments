package org.northernarc.assessment4.serviceimpl;

import lombok.RequiredArgsConstructor;
import org.northernarc.assessment4.dto.CustomerSummaryDTO;
import org.northernarc.assessment4.dto.DashboardResponse;
import org.northernarc.assessment4.exception.AccountNotFoundException;
import org.northernarc.assessment4.exception.CustomerNotFoundException;
import org.northernarc.assessment4.model.Account;
import org.northernarc.assessment4.model.Customer;
import org.northernarc.assessment4.model.Transaction;
import org.northernarc.assessment4.repository.AccountRepository;
import org.northernarc.assessment4.repository.CustomerRepository;
import org.northernarc.assessment4.repository.TransactionRepository;
import org.northernarc.assessment4.service.BankService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankServiceImpl implements BankService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    // --- Core Entity Writing Persistence Methods ---
    @Override
    @Transactional
    public Customer saveCustomer(Customer customer) {
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteAccount(String accountNumber) {
        Account account = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
        accountRepository.delete(account);
    }

    // --- Task 3: Spring Data JPA Derived Queries ---
    @Override
    public List<Account> getAccountsByType(String accountType) {
        return accountRepository.findByAccountType(accountType);
    }

    @Override
    public List<Customer> getCustomersByBranch(String branch) {
        return customerRepository.findByBranch(branch);
    }

    @Override
    public List<Transaction> getTransactionsByType(String transactionType) {
        return transactionRepository.findByTransactionType(transactionType);
    }

    @Override
    public List<Account> getAccountsWithBalanceGreaterThan(double amount) {
        return accountRepository.findByBalanceGreaterThan(amount);
    }

    // --- Task 4: JPQL Custom Queries ---
    @Override
    public List<Customer> getRichCustomers(double threshold) {
        return customerRepository.findRichCustomers(threshold);
    }

    @Override
    public Map<String, Double> getTotalBalancePerBranch() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : customerRepository.findTotalBalancePerBranch()) {
            String branch = (String) row[0];
            Double totalBalance = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            result.put(branch, totalBalance);
        }
        return result;
    }

    @Override
    public List<Customer> getCustomersWithMultipleAccounts() {
        return customerRepository.findCustomersWithMultipleAccounts();
    }

    @Override
    public Transaction getLatestTransaction() {
        return transactionRepository.findLatestTransaction(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Account> getAccountsWithNoTransactions() {
        return accountRepository.findAccountsWithNoTransactions();
    }

    // --- Task 5: JPQL Update Query ---
    @Override
    @Transactional
    public void increaseAccountBalance(String accountNumber, double amount) {
        int updated = accountRepository.increaseBalance(accountNumber, amount);
        if (updated == 0) {
            throw new AccountNotFoundException("Account not found with number: " + accountNumber);
        }
    }

    // --- Task 6: Pagination & Sorting ---
    @Override
    public Page<Account> getAllAccountsPaginated(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    // --- Task 7: DTO Projection Mapping ---
    @Override
    public CustomerSummaryDTO getCustomerSummary(Long customerId) {
        return customerRepository.findCustomerSummary(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
    }

    // --- Final Challenge: Optimized Dashboard Metrics ---
    @Override
    public DashboardResponse getDashboardMetrics() {
        long totalCustomers = customerRepository.count();
        long totalAccounts = accountRepository.count();

        Double totalBalance = accountRepository.sumAllBalances();
        if (totalBalance == null) {
            totalBalance = 0.0;
        }

        Pageable topOne = PageRequest.of(0, 1);
        String topBranch = customerRepository.findTopBranches(topOne)
                .stream().findFirst().orElse(null);
        String highestBalanceCustomer = customerRepository.findTopCustomers(topOne)
                .stream().findFirst().orElse(null);

        return new DashboardResponse(totalCustomers, totalAccounts, totalBalance, topBranch, highestBalanceCustomer);
    }
}
