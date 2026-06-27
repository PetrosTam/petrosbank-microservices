package com.backendev.transactionservice.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<HttpErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), "JWT authentication failed.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(httpErrorResponse);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<HttpErrorResponse> handleInsufficientFundsException(InsufficientFundsException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), "Insufficient Balance.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
    }

    @ExceptionHandler(InvalidAccountException.class)
    public ResponseEntity<HttpErrorResponse> handleInvalidAccountException(InvalidAccountException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), "Account not found. Invalid Account.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(httpErrorResponse);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<HttpErrorResponse> handleServiceUnavailable(ServiceUnavailableException exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), "Service unavailable.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(httpErrorResponse);

    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleGenericException(Exception exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(), "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(httpErrorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HttpErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.BAD_REQUEST, message, "Validation failed.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(httpErrorResponse);
    }
}
