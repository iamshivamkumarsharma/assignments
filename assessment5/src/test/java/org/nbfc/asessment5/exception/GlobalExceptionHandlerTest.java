package org.nbfc.asessment5.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nbfc.asessment5.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/test");
        request = mockRequest;
    }

    private void assertError(ResponseEntity<ErrorResponse> response, HttpStatus expectedStatus) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("should map CustomerNotFoundException to 404")
    void shouldMapCustomerNotFoundTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleCustomerNotFound(new CustomerNotFoundException(1L), request);

        assertError(response, HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("1");
    }

    @Test
    @DisplayName("should map AccountNotFoundException to 404")
    void shouldMapAccountNotFoundTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccountNotFound(new AccountNotFoundException(5L), request);

        assertError(response, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("should map DuplicateEmailException to 409")
    void shouldMapDuplicateEmailTo409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleDuplicateEmail(new DuplicateEmailException("john@example.com"), request);

        assertError(response, HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("john@example.com");
    }

    @Test
    @DisplayName("should map InsufficientBalanceException to 422")
    void shouldMapInsufficientBalanceTo422() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInsufficientBalance(new InsufficientBalanceException("Insufficient funds"), request);

        assertError(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("should map InvalidTransactionException to 400")
    void shouldMapInvalidTransactionTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleInvalidTransaction(new InvalidTransactionException("Invalid transfer"), request);

        assertError(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should map IllegalArgumentException to 400")
    void shouldMapIllegalArgumentTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Invalid account type"), request);

        assertError(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should map MethodArgumentNotValidException to 400 with field errors")
    void shouldMapValidationErrorsTo400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("customerRequest", "email", "must be a valid email");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex, request);

        assertError(response, HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getValidationErrors())
                .containsEntry("email", "must be a valid email");
    }

    @Test
    @DisplayName("should map ConstraintViolationException to 400")
    void shouldMapConstraintViolationTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException("constraint violated", Collections.emptySet()), request);

        assertError(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should map HttpMessageNotReadableException to 400")
    void shouldMapNotReadableTo400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("Malformed JSON", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleNotReadable(ex, request);

        assertError(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should map AuthenticationException to 401")
    void shouldMapAuthenticationTo401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAuthentication(new BadCredentialsException("Bad credentials"), request);

        assertError(response, HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("should map AccessDeniedException to 403")
    void shouldMapAccessDeniedTo403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("Access denied"), request);

        assertError(response, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("should map an unexpected Exception to 500")
    void shouldMapGenericExceptionTo500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGeneric(new RuntimeException("boom"), request);

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
