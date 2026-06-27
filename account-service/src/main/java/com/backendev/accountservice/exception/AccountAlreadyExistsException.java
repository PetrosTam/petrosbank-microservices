package com.backendev.accountservice.exception;

public class AccountAlreadyExistsException extends RuntimeException{

    public AccountAlreadyExistsException(String message){
        super(message);
    }

}
