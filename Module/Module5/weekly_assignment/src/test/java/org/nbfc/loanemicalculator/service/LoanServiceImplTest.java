package org.nbfc.loanemicalculator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nbfc.loanemicalculator.dto.CreateLoanRequest;
import org.nbfc.loanemicalculator.dto.CustomerSummaryDTO;
import org.nbfc.loanemicalculator.dto.DashboardDTO;
import org.nbfc.loanemicalculator.dto.LoanDetailDTO;
import org.nbfc.loanemicalculator.dto.PayEmiRequest;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.nbfc.loanemicalculator.entity.Loan;
import org.nbfc.loanemicalculator.entity.PenaltyTransaction;
import org.nbfc.loanemicalculator.exception.CustomerNotFoundException;
import org.nbfc.loanemicalculator.exception.EmiNotFoundException;
import org.nbfc.loanemicalculator.exception.LoanNotFoundException;
import org.nbfc.loanemicalculator.exception.PenaltyNotFoundException;
import org.nbfc.loanemicalculator.repository.CustomerRepository;
import org.nbfc.loanemicalculator.repository.EmiScheduleRepository;
import org.nbfc.loanemicalculator.repository.LoanRepository;
import org.nbfc.loanemicalculator.repository.PenaltyTransactionRepository;
import org.nbfc.loanemicalculator.serviceimpl.LoanServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit5 + Mockito unit tests for the service layer business logic.
 * No Spring context is started, so transaction/cache annotations are inert and
 * only the method logic is exercised.
 */
@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock LoanRepository loanRepository;
    @Mock CustomerRepository customerRepository;
    @Mock EmiScheduleRepository emiScheduleRepository;
    @Mock PenaltyTransactionRepository penaltyTransactionRepository;

    @InjectMocks LoanServiceImpl service;

    @Captor ArgumentCaptor<PenaltyTransaction> penaltyCaptor;

    private Loan loan(Long id, String type, double rate, String status) {
        Loan l = new Loan();
        l.setLoanId(id);
        l.setLoanType(type);
        l.setInterestRate(rate);
        l.setLoanStatus(status);
        return l;
    }

    private EmiSchedule emi(Long id, String status, Loan loan) {
        EmiSchedule e = new EmiSchedule();
        e.setEmiId(id);
        e.setStatus(status);
        e.setEmiAmount(1000.0);
        e.setLoan(loan);
        return e;
    }

    @Test
    void getCustomerSummary_returnsDto_whenFound() {
        Customer c = new Customer();
        c.setCustomerId(1L);
        c.setCustomerName("Rahul Sharma");
        c.setBranchName("Bangalore");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(c));
        when(customerRepository.countLoans(1L)).thenReturn(2L);
        when(customerRepository.totalPenaltyPaid(1L)).thenReturn(1250.0);

        CustomerSummaryDTO dto = service.getCustomerSummary(1L);

        assertThat(dto.getCustomerName()).isEqualTo("Rahul Sharma");
        assertThat(dto.getBranchName()).isEqualTo("Bangalore");
        assertThat(dto.getNumberOfLoans()).isEqualTo(2L);
        assertThat(dto.getTotalPenaltyPaid()).isEqualTo(1250.0);
    }

    @Test
    void getCustomerSummary_throws_whenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCustomerSummary(99L))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void deleteLoan_throws_whenMissing() {
        when(loanRepository.existsById(5L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteLoan(5L)).isInstanceOf(LoanNotFoundException.class);
        verify(loanRepository, never()).deleteById(any());
    }

    @Test
    void deleteLoan_deletes_whenPresent() {
        when(loanRepository.existsById(5L)).thenReturn(true);
        service.deleteLoan(5L);
        verify(loanRepository).deleteById(5L);
    }

    @Test
    void deletePenalty_throws_whenMissing() {
        when(penaltyTransactionRepository.existsById(7L)).thenReturn(false);
        assertThatThrownBy(() -> service.deletePenalty(7L)).isInstanceOf(PenaltyNotFoundException.class);
    }

    @Test
    void increaseInterestRate_delegatesToRepository() {
        when(loanRepository.increaseInterestRate("HOME", 2.0)).thenReturn(3);
        int updated = service.increaseInterestRate("HOME", 2.0);
        assertThat(updated).isEqualTo(3);
        verify(loanRepository).increaseInterestRate("HOME", 2.0);
    }

    @Test
    void getDashboard_assemblesAggregates() {
        when(customerRepository.count()).thenReturn(3L);
        when(loanRepository.count()).thenReturn(3L);
        when(penaltyTransactionRepository.getTotalPenaltyCollected()).thenReturn(1250.0);
        when(customerRepository.findTopBranches(any())).thenReturn(List.of("Bangalore"));
        when(customerRepository.findTopPenaltyPayingCustomers(any())).thenReturn(List.of("Rahul Sharma"));
        when(emiScheduleRepository.countOverdue()).thenReturn(1L);

        DashboardDTO d = service.getDashboard();

        assertThat(d.getTotalCustomers()).isEqualTo(3L);
        assertThat(d.getTotalLoans()).isEqualTo(3L);
        assertThat(d.getTotalPenaltyCollected()).isEqualTo(1250.0);
        assertThat(d.getTopPerformingBranch()).isEqualTo("Bangalore");
        assertThat(d.getHighestPenaltyPayingCustomer()).isEqualTo("Rahul Sharma");
        assertThat(d.getTotalOverdueEmis()).isEqualTo(1L);
    }

    @Test
    void getLoanDetails_mapsFetchJoinResult_inSingleQuery() {
        Loan l = loan(1L, "HOME", 8.0, "ACTIVE");
        l.setEmiSchedules(List.of(emi(10L, "OVERDUE", l), emi(11L, "PAID", l)));
        when(loanRepository.findAllWithSchedules()).thenReturn(List.of(l));

        List<LoanDetailDTO> details = service.getLoanDetails();

        assertThat(details).hasSize(1);
        assertThat(details.get(0).getTotalEmis()).isEqualTo(2);
        assertThat(details.get(0).getOverdueEmis()).isEqualTo(1L);
        // one query, no per-loan follow-ups
        verify(loanRepository, times(1)).findAllWithSchedules();
    }

    @Test
    void payEmi_marksPaidAndRecordsPenalty_whenOverdue() {
        Loan l = loan(1L, "HOME", 8.0, "ACTIVE");
        EmiSchedule e = emi(10L, "OVERDUE", l);
        when(emiScheduleRepository.findById(10L)).thenReturn(Optional.of(e));
        when(emiScheduleRepository.save(any(EmiSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        PayEmiRequest req = new PayEmiRequest();
        req.setPaymentMode("ONLINE");
        req.setPenaltyAmount(250.0);

        EmiSchedule result = service.payEmi(10L, req);

        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getPaymentDate()).isNotNull();
        verify(penaltyTransactionRepository).save(penaltyCaptor.capture());
        PenaltyTransaction saved = penaltyCaptor.getValue();
        assertThat(saved.getPenaltyAmount()).isEqualTo(250.0);
        assertThat(saved.getPaymentMode()).isEqualTo("ONLINE");
        assertThat(saved.getLoan()).isSameAs(l);
    }

    @Test
    void payEmi_noPenalty_whenNotOverdue() {
        Loan l = loan(1L, "HOME", 8.0, "ACTIVE");
        EmiSchedule e = emi(12L, "PENDING", l);
        when(emiScheduleRepository.findById(12L)).thenReturn(Optional.of(e));
        when(emiScheduleRepository.save(any(EmiSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        PayEmiRequest req = new PayEmiRequest();
        req.setPaymentMode("CASH");
        req.setPenaltyAmount(500.0);

        EmiSchedule result = service.payEmi(12L, req);

        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(penaltyTransactionRepository, never()).save(any());
    }

    @Test
    void payEmi_throws_whenEmiMissing() {
        when(emiScheduleRepository.findById(404L)).thenReturn(Optional.empty());
        PayEmiRequest req = new PayEmiRequest();
        req.setPaymentMode("CASH");
        assertThatThrownBy(() -> service.payEmi(404L, req)).isInstanceOf(EmiNotFoundException.class);
        verify(emiScheduleRepository, never()).save(any());
    }

    @Test
    void createLoan_savesMappedEntity() {
        CreateLoanRequest req = new CreateLoanRequest();
        req.setLoanType("HOME");
        req.setPrincipalAmount(1000000.0);
        req.setInterestRate(8.5);
        req.setLoanTenureMonths(240);
        req.setMonthlyEmi(10000.0);
        req.setLoanStatus("ACTIVE");
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setLoanId(100L);
            return l;
        });

        Loan saved = service.createLoan(req);

        assertThat(saved.getLoanId()).isEqualTo(100L);
        assertThat(saved.getLoanType()).isEqualTo("HOME");
        assertThat(saved.getInterestRate()).isEqualTo(8.5);
        assertThat(saved.getLoanStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void findByLoanType_delegatesToRepository() {
        Loan l = loan(1L, "HOME", 8.0, "ACTIVE");
        when(loanRepository.findByLoanType("HOME")).thenReturn(List.of(l));
        assertThat(service.findByLoanType("HOME")).containsExactly(l);
    }

    @Test
    void findLatestPenalty_returnsNull_whenNone() {
        when(penaltyTransactionRepository.findLatest(any())).thenReturn(List.of());
        assertThat(service.findLatestPenalty()).isNull();
    }

    @Test
    void findLatestPenalty_returnsMostRecent() {
        PenaltyTransaction p = new PenaltyTransaction();
        p.setPenaltyAmount(750.0);
        when(penaltyTransactionRepository.findLatest(any())).thenReturn(List.of(p));
        assertThat(service.findLatestPenalty().getPenaltyAmount()).isEqualTo(750.0);
    }

    @Test
    void deletePenalty_deletes_whenPresent() {
        when(penaltyTransactionRepository.existsById(7L)).thenReturn(true);
        service.deletePenalty(7L);
        verify(penaltyTransactionRepository).deleteById(7L);
    }

    @Test
    void payEmi_overdueWithZeroPenalty_recordsNoPenalty() {
        Loan l = loan(1L, "HOME", 8.0, "ACTIVE");
        EmiSchedule e = emi(20L, "OVERDUE", l);
        when(emiScheduleRepository.findById(20L)).thenReturn(Optional.of(e));
        when(emiScheduleRepository.save(any(EmiSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        PayEmiRequest req = new PayEmiRequest();
        req.setPaymentMode("CASH");
        req.setPenaltyAmount(0.0);

        EmiSchedule result = service.payEmi(20L, req);

        assertThat(result.getStatus()).isEqualTo("PAID");
        verify(penaltyTransactionRepository, never()).save(any());
    }

    @Test
    void getDashboard_nullsTopFields_whenNoData() {
        when(customerRepository.count()).thenReturn(0L);
        when(loanRepository.count()).thenReturn(0L);
        when(penaltyTransactionRepository.getTotalPenaltyCollected()).thenReturn(0.0);
        when(customerRepository.findTopBranches(any())).thenReturn(List.of());
        when(customerRepository.findTopPenaltyPayingCustomers(any())).thenReturn(List.of());
        when(emiScheduleRepository.countOverdue()).thenReturn(0L);

        DashboardDTO d = service.getDashboard();

        assertThat(d.getTopPerformingBranch()).isNull();
        assertThat(d.getHighestPenaltyPayingCustomer()).isNull();
        assertThat(d.getTotalOverdueEmis()).isEqualTo(0L);
    }
}
