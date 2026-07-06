package org.nbfc.loanemicalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Aggregated customer borrowing summary")
public class CustomerSummaryDTO {
    @Schema(example = "Rahul Sharma")
    private String customerName;
    @Schema(example = "Bangalore")
    private String branchName;
    @Schema(description = "Number of distinct loans", example = "2")
    private Long numberOfLoans;
    @Schema(description = "Total penalties paid", example = "1250.0")
    private Double totalPenaltyPaid;
}
