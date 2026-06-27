package com.backendev.transactionservice.exception;

public class InvalidAccountException extends RuntimeException{

    public InvalidAccountException(String message){
        super(message);
    }

    public InvalidAccountException(String message, Throwable ex){
        super(message, ex);
    }
}
