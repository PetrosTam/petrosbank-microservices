package com.backendev.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<HttpErrorResponse> handleUserExistsException(UserAlreadyExistsException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), "The user already exists.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<HttpErrorResponse> handleUserNotFoundException(UserNotFoundException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), "User not found.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(httpErrorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleGenericException(Exception exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(), "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(httpErrorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<HttpErrorResponse> handleBadCredentialsException(BadCredentialsException exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.UNAUTHORIZED,
                exception.getMessage(), "Bad credentials.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(httpErrorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.FORBIDDEN,
                exception.getMessage(), "Access denied.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(httpErrorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
