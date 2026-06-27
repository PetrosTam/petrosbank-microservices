package com.backendev.accountservice.exception;

public class InactiveAccountException extends RuntimeException{

    public InactiveAccountException(String message){
        super(message);
    }
}
