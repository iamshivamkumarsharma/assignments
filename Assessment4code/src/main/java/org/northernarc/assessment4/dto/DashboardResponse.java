package org.northernarc.assessment4.dto;

import lombok.Data;
public record DashboardResponse(
        Long totalCustomers,
        Long totalAccounts,
        Double totalBalance,
        String topBranch,
        String highestBalanceCustomer
) {}
