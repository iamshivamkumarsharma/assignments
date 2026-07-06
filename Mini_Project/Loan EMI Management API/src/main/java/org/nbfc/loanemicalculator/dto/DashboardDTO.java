package org.nbfc.loanemicalculator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDTO {
    private Long totalCustomers;
    private Long totalLoans;
    private Double totalPenaltyCollected;
    private String topPerformingBranch;
    private String highestPenaltyPayingCustomer;
    private Long totalOverdueEmis;
}
