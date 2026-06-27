package com.backendev.accountservice.exception;

public class AccountLimitExceededException extends RuntimeException{

    public AccountLimitExceededException(String message){
        super(message);
    }

}
