package com.backendev.transactionservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<HttpErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                "JWT authentication failed."
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(httpErrorResponse);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<HttpErrorResponse> handleInsufficientFundsException(InsufficientFundsException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "Insufficient Balance."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
    }

    @ExceptionHandler(InvalidAccountException.class)
    public ResponseEntity<HttpErrorResponse> handleInvalidAccountException(InvalidAccountException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                "Account not found. Invalid Account."
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(httpErrorResponse);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<HttpErrorResponse> handleServiceUnavailable(ServiceUnavailableException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                "Service unavailable."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(httpErrorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleGenericException(Exception exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                "An unexpected error occurred."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(httpErrorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HttpErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                "Validation failed."
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(httpErrorResponse);
    }

    private HttpErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            String details
    ) {
        HttpServletRequest request = getCurrentRequest();

        String path = request != null ? request.getRequestURI() : null;
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);

        if ((correlationId == null || correlationId.isBlank()) && request != null) {
            correlationId = request.getHeader(CORRELATION_ID_HEADER);
        }

        return new HttpErrorResponse(
                status,
                message,
                details,
                path,
                correlationId
        );
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        return attributes.getRequest();
    }
}