package com.backendev.accountservice.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<HttpErrorResponse> handleUserExistsException(AccountAlreadyExistsException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), "The account already exists.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(httpErrorResponse);
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<HttpErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), "JWT authentication failed.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(httpErrorResponse);
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    public ResponseEntity<HttpErrorResponse> handleAccountAccessDeniedException(AccountAccessDeniedException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), "Account access denied.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(httpErrorResponse);
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<HttpErrorResponse> handleInactiveAccountException(InactiveAccountException exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(), "Inactive account.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(httpErrorResponse);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<HttpErrorResponse> handleAccountNotFoundException(AccountNotFoundException exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.NOT_FOUND,
                exception.getMessage(), "Account not found.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(httpErrorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpErrorResponse> handleGenericException(Exception exception) {
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(), "An unexpected error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(httpErrorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpErrorResponse> handleAccessDeniedException(AccessDeniedException exception){
        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(
                HttpStatus.FORBIDDEN, exception.getMessage(), "You do not have permission to access this resource"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(httpErrorResponse);
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
