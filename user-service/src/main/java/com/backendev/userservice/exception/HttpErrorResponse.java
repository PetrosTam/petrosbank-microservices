package com.backendev.userservice.exception;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Data
@NoArgsConstructor
public class HttpErrorResponse {

    private HttpStatus errorCode;
    private String errorMessage;
    private String errorDetails;
    private Instant timestamp;
    private String path;
    private String correlationId;

    public HttpErrorResponse(HttpStatus errorCode, String errorMessage, String errorDetails) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        this.timestamp = Instant.now();
    }

    public HttpErrorResponse(
            HttpStatus errorCode,
            String errorMessage,
            String errorDetails,
            String path,
            String correlationId
    ) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        this.timestamp = Instant.now();
        this.path = path;
        this.correlationId = correlationId;
    }
}