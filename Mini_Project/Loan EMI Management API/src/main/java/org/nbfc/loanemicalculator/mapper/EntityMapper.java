package org.nbfc.loanemicalculator.mapper;

import org.nbfc.loanemicalculator.dto.CustomerResponse;
import org.nbfc.loanemicalculator.dto.LoanDetailDTO;
import org.nbfc.loanemicalculator.entity.Customer;
import org.nbfc.loanemicalculator.entity.EmiSchedule;
import org.nbfc.loanemicalculator.entity.Loan;
import org.nbfc.loanemicalculator.enums.EmiStatus;

import java.util.List;

/**
 * Central place for entity to DTO conversions so controllers never leak entities
 * (or the password hash) to clients.
 */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static CustomerResponse toCustomerResponse(Customer c) {
        return CustomerResponse.builder()
                .customerId(c.getCustomerId())
                .customerName(c.getCustomerName())
                .email(c.getEmail())
                .branchName(c.getBranchName())
                .role(c.getRole())
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static LoanDetailDTO toLoanDetail(Loan loan) {
        List<EmiSchedule> schedules = loan.getEmiSchedules();
        List<LoanDetailDTO.EmiSummaryDTO> emis = schedules.stream()
                .map(e -> LoanDetailDTO.EmiSummaryDTO.builder()
                        .emiId(e.getEmiId())
                        .emiAmount(e.getEmiAmount())
                        .status(e.getStatus())
                        .build())
                .toList();
        long overdue = schedules.stream()
                .filter(e -> EmiStatus.OVERDUE.name().equalsIgnoreCase(e.getStatus()))
                .count();
        return LoanDetailDTO.builder()
                .loanId(loan.getLoanId())
                .loanType(loan.getLoanType())
                .interestRate(loan.getInterestRate())
                .loanStatus(loan.getLoanStatus())
                .totalEmis(schedules.size())
                .overdueEmis(overdue)
                .emis(emis)
                .build();
    }
}
