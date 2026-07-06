package org.nbfc.loanemicalculator.exception;

/**
 * Thrown when a registration is attempted with an email that already exists.
 * Mapped to HTTP 409 CONFLICT by {@link GlobalExceptionHandler}.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
