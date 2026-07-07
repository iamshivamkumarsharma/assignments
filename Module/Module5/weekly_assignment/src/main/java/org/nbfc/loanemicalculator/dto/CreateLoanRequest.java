package org.nbfc.loanemicalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.nbfc.loanemicalculator.enums.LoanStatus;
import org.nbfc.loanemicalculator.enums.LoanType;
import org.nbfc.loanemicalculator.validation.ValueOfEnum;

@Data
@Schema(description = "Payload to create a new loan")
public class CreateLoanRequest {

    @NotBlank
    @ValueOfEnum(enumClass = LoanType.class)
    @Schema(example = "HOME", description = "HOME | PERSONAL | VEHICLE | EDUCATION")
    private String loanType;

    @NotNull
    @Positive
    @Schema(example = "1000000.0")
    private Double principalAmount;

    @NotNull
    @Positive
    @Schema(example = "8.5")
    private Double interestRate;

    @NotNull
    @Positive
    @Schema(example = "240")
    private Integer loanTenureMonths;

    @NotNull
    @Positive
    @Schema(example = "10000.0")
    private Double monthlyEmi;

    @NotBlank
    @ValueOfEnum(enumClass = LoanStatus.class)
    @Schema(example = "ACTIVE", description = "ACTIVE | CLOSED | DEFAULTED")
    private String loanStatus;
}
