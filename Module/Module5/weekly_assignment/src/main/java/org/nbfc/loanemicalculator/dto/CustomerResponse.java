package org.nbfc.loanemicalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Customer view that never exposes the password hash")
public class CustomerResponse {
    private Long customerId;
    private String customerName;
    private String email;
    private String branchName;
    private String role;
    private LocalDateTime createdAt;
}
