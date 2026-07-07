package org.nbfc.loanemicalculator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login credentials")
public class LoginRequest {
    @NotBlank
    @Schema(description = "Registered email", example = "user@bank.com")
    private String email;
    @NotBlank
    @Schema(description = "Account password", example = "password1")
    private String password;
}
