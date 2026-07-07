package org.nbfc.loanemicalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.nbfc.loanemicalculator.enums.PaymentMode;
import org.nbfc.loanemicalculator.validation.ValueOfEnum;

@Data
@Schema(description = "Payload used when settling an EMI installment")
public class PayEmiRequest {

    @NotBlank
    @ValueOfEnum(enumClass = PaymentMode.class)
    @Schema(example = "ONLINE", description = "CASH | CARD | ONLINE")
    private String paymentMode;

    @Schema(description = "Penalty charged when the EMI was overdue. Defaults to 0 when omitted.",
            example = "250.0")
    private Double penaltyAmount;
}
