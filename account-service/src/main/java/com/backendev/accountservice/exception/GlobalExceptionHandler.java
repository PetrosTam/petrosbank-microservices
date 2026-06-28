package com.backendev.accountservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<HttpErrorResponse> handleUserExistsException(AccountAlreadyExistsException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "The account already exists."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<HttpErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                "JWT authentication failed."
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(httpErrorResponse);
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    public ResponseEntity<HttpErrorResponse> handleAccountAccessDeniedException(AccountAccessDeniedException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                "Account access denied."
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(httpErrorResponse);
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<HttpErrorResponse> handleInactiveAccountException(InactiveAccountException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                "Inactive account."
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(httpErrorResponse);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<HttpErrorResponse> handleAccountNotFoundException(AccountNotFoundException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                "Account not found."
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(httpErrorResponse);
    }

    @ExceptionHandler(AccountLimitExceededException.class)
    public ResponseEntity<HttpErrorResponse> handleAccountLimitExceededException(AccountLimitExceededException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "Account limit exceeded."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                "You do not have permission to access this resource"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(httpErrorResponse);
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