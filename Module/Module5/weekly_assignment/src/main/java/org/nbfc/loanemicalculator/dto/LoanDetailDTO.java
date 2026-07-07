package org.nbfc.loanemicalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "A loan together with a summary of its EMI schedule, fetched without N+1 queries")
public class LoanDetailDTO {
    private Long loanId;
    private String loanType;
    private Double interestRate;
    private String loanStatus;
    private int totalEmis;
    private long overdueEmis;
    private List<EmiSummaryDTO> emis;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Compact EMI schedule row")
    public static class EmiSummaryDTO {
        private Long emiId;
        private Double emiAmount;
        private String status;
    }
}
