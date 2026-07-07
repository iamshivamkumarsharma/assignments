package org.nbfc.loanemicalculator.mapper;

import org.junit.jupiter.api.Test;
import org.nbfc.loanemicalculator.dto.CustomerResponse;
import org.nbfc.loanemicalculator.dto.LoanDetailDTO;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.nbfc.loanemicalculator.entity.Loan;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest {

    @Test
    void toCustomerResponse_copiesSafeFieldsOnly() {
        Customer c = new Customer();
        c.setCustomerId(1L);
        c.setCustomerName("Rahul Sharma");
        c.setEmail("rahul@bank.com");
        c.setBranchName("Bangalore");
        c.setRole("USER");
        c.setPassword("bcrypt-hash-should-not-leak");

        CustomerResponse r = EntityMapper.toCustomerResponse(c);

        assertThat(r.getCustomerId()).isEqualTo(1L);
        assertThat(r.getEmail()).isEqualTo("rahul@bank.com");
        assertThat(r.getBranchName()).isEqualTo("Bangalore");
        assertThat(r.getRole()).isEqualTo("USER");
        // CustomerResponse intentionally has no password field, so the hash cannot leak.
    }

    @Test
    void toLoanDetail_summarisesSchedule() {
        Loan l = new Loan();
        l.setLoanId(1L);
        l.setLoanType("HOME");
        l.setInterestRate(8.0);
        l.setLoanStatus("ACTIVE");
        l.setEmiSchedules(List.of(emi(1L, "OVERDUE"), emi(2L, "PAID"), emi(3L, "PENDING")));

        LoanDetailDTO dto = EntityMapper.toLoanDetail(l);

        assertThat(dto.getLoanId()).isEqualTo(1L);
        assertThat(dto.getTotalEmis()).isEqualTo(3);
        assertThat(dto.getOverdueEmis()).isEqualTo(1L);
        assertThat(dto.getEmis()).hasSize(3);
    }

    private EmiSchedule emi(Long id, String status) {
        EmiSchedule e = new EmiSchedule();
        e.setEmiId(id);
        e.setStatus(status);
        e.setEmiAmount(1000.0);
        return e;
    }
}
