package com.backendev.transactionservice.integration.config;

import com.backendev.transactionservice.client.AccountServiceClient;
import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.TransferValidationResponse;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestFeignConfig {

    @Bean(name = "accountServiceClient")
    public AccountServiceClient mockAccountServiceClient() {
        AccountServiceClient mock = Mockito.mock(AccountServiceClient.class);

        AccountResponse response = new AccountResponse();
        response.setAccountNumber(1234567890L);
        response.setUserId("user-123");
        response.setAccountType("SAVINGS");
        response.setStatus("ACTIVE");
        response.setCreatedAt(Instant.now());

        when(mock.getAccount(anyLong()))
                .thenReturn(ResponseEntity.ok(response));

        when(mock.updateAccountBalance(anyLong(), any(UpdateAccountBalanceRequest.class)))
                .thenReturn(ResponseEntity.ok(response));

        when(mock.validateTransfer(anyLong(), anyLong(), anyString()))
                .thenReturn(ResponseEntity.ok(
                        new TransferValidationResponse(response, response)
                ));

        return mock;
    }
}
