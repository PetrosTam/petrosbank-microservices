package com.backendev.transactionservice.unit.client;

import com.backendev.transactionservice.client.AccountServiceErrorDecoder;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.ServiceUnavailableException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;

class AccountServiceErrorDecoderTest {

    private AccountServiceErrorDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new AccountServiceErrorDecoder();
    }

    private Response createResponse(int status) {
        return Response.builder()
                .status(status)
                .reason("Testing")
                .request(Request.create("GET","http://localhost/test", emptyMap(), null, StandardCharsets.UTF_8))
                .build();
    }

    @Test
    void shouldReturnSecurityExceptionFor403() {
        Response response = createResponse(403);
        Exception exception = decoder.decode("methodKey", response);

        assertInstanceOf(SecurityException.class, exception);
        assertEquals("Access denied: Source account does not belong to the current user", exception.getMessage());
    }

    @Test
    void shouldReturnInvalidAccountExceptionFor404() {
        Response response = createResponse(404);
        Exception exception = decoder.decode("methodKey", response);

        assertInstanceOf(InvalidAccountException.class, exception);
        assertEquals("One or more accounts not found", exception.getMessage());
    }

    @Test
    void shouldReturnInvalidAccountExceptionFor400() {
        Response response = createResponse(400);
        Exception exception = decoder.decode("methodKey", response);

        assertInstanceOf(InvalidAccountException.class, exception);
        assertEquals("Invalid account request", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void shouldReturnServiceUnavailableExceptionForServerErrors(int status) {
        Response response = createResponse(status);
        Exception exception = decoder.decode("methodKey", response);

        assertInstanceOf(ServiceUnavailableException.class, exception);
        assertEquals("Account service temporarily unavailable", exception.getMessage());
    }

    @Test
    void shouldDelegateToDefaultDecoderForOtherStatusCodes() {
        Response response = createResponse(418);
        Exception exception = decoder.decode("methodKey", response);

        assertNotNull(exception);
        assertFalse(exception instanceof SecurityException);
        assertFalse(exception instanceof InvalidAccountException);
        assertFalse(exception instanceof ServiceUnavailableException);
    }
}