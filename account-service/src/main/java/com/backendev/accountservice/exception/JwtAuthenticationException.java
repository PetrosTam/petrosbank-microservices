package com.backendev.accountservice.exception;

public class JwtAuthenticationException extends RuntimeException{
    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable ex) {
        super(message, ex);
    }
}
