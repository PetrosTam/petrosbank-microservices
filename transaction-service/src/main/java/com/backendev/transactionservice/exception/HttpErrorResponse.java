package com.backendev.transactionservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class HttpErrorResponse {

    private HttpStatus errorCode;
    private String errorMessage;
    private String errorDetails;
}
