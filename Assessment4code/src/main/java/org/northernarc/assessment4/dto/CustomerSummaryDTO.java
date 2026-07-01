package org.northernarc.assessment4.dto;

public record CustomerSummaryDTO(
        String customerName,
        String branch,
        Long numberOfAccounts,
        Double totalBalance
) {
    // Canonical constructor validation (optional safeguard)
    public CustomerSummaryDTO {
        if (totalBalance == null) {
            totalBalance = 0.0;
        }
        if (numberOfAccounts == null) {
            numberOfAccounts = 0L;
        }
    }
}
