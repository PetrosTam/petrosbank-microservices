package com.backendev.transactionservice.client;

import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.ServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class AccountServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 403 -> new SecurityException("Access denied: Source account does not belong to the current user");
            case 404 -> new InvalidAccountException("One or more accounts not found");
            case 400 -> new InvalidAccountException("Invalid account request");
            case 500, 502, 503 -> new ServiceUnavailableException("Account service temporarily unavailable");
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
