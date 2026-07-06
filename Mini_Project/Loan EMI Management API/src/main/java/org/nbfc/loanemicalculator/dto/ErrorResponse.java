package org.nbfc.loanemicalculator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error payload returned by the global exception handler")
public class ErrorResponse {

    @Schema(example = "2026-07-01T22:55:59.401")
    private LocalDateTime timestamp;

    @Schema(example = "404")
    private int status;

    @Schema(example = "Not Found")
    private String error;

    @Schema(example = "Loan with ID 42 does not exist.")
    private String message;

    @Schema(example = "/loans/42")
    private String path;

    @Schema(description = "Per-field validation errors, present only for 400 validation failures")
    private List<FieldValidationError> fieldErrors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "A single field-level validation error")
    public static class FieldValidationError {
        @Schema(example = "email")
        private String field;
        @Schema(example = "must be a well-formed email address")
        private String message;
        @Schema(example = "not-an-email")
        private Object rejectedValue;
    }
}
