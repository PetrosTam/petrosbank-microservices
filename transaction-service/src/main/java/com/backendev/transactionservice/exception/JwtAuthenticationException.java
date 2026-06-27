package com.backendev.transactionservice.exception;

public class JwtAuthenticationException extends RuntimeException{

    public JwtAuthenticationException(String message){
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable ex){
        super(message, ex);
    }
}
