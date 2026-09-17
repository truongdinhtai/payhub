package com.payhub.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception handling. Every error response follows RFC 7807
 * (application/problem+json) so the Angular client can parse errors uniformly.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), "not-found");
    }

    @ExceptionHandler(ForbiddenResourceException.class)
    public ProblemDetail handleForbidden(ForbiddenResourceException ex) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), "forbidden");
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.CONFLICT, "Business rule violation", ex.getMessage(), "business-rule");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toValidationError)
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more fields are invalid", "validation");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body is missing or malformed", "malformed-request");
    }

    @ExceptionHandler(com.payhub.payment.exception.PaymentGatewayException.class)
    public ProblemDetail handlePaymentGateway(com.payhub.payment.exception.PaymentGatewayException ex) {
        return problem(HttpStatus.BAD_GATEWAY, "Payment provider error",
                "The payment provider could not be reached; please try again", "payment-gateway");
    }

    @ExceptionHandler(com.payhub.payment.exception.WebhookSignatureException.class)
    public ProblemDetail handleWebhookSignature(com.payhub.payment.exception.WebhookSignatureException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid webhook signature",
                "The webhook signature could not be verified", "webhook-signature");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred", "internal");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://payhub.dev/problems/" + type));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private static ValidationError toValidationError(FieldError fe) {
        return new ValidationError(fe.getField(), fe.getDefaultMessage());
    }

    /** Single field validation failure, exposed under the {@code errors} property. */
    public record ValidationError(String field, String message) {
    }
}
