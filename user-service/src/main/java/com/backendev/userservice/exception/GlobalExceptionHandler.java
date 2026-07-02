package com.backendev.userservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<HttpErrorResponse> handleUserExistsException(
            UserAlreadyExistsException exception
    ) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                "The user already exists."
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(httpErrorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<HttpErrorResponse> handleUserNotFoundException(
            UserNotFoundException exception
    ) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                "User not found."
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(httpErrorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<HttpErrorResponse> handleBadCredentialsException(
            BadCredentialsException exception
    ) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                "Bad credentials."
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(httpErrorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<HttpErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                "Invalid refresh token."
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(httpErrorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception
    ) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                "Access denied."
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(httpErrorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<HttpErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException exception
    ) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "The requested resource was not found.",
                "No endpoint exists for the requested path."
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(httpErrorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleGenericException(
            Exception exception
    ) {
        HttpErrorResponse httpErrorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                "An unexpected error occurred."
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(httpErrorResponse);
    }

    private HttpErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            String details
    ) {
        HttpServletRequest request = getCurrentRequest();

        String path = request != null
                ? request.getRequestURI()
                : null;

        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);

        if ((correlationId == null || correlationId.isBlank())
                && request != null) {
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
                (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        return attributes.getRequest();
    }
}