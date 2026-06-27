package com.backendev.transactionservice.client;

import com.backendev.transactionservice.config.FeignConfig;
import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.TransferValidationResponse;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "account-service", configuration = FeignConfig.class, primary = false)
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long accountNumber);

    @GetMapping("/api/v1/accounts/validate-transfer")
    ResponseEntity<TransferValidationResponse> validateTransfer(@RequestParam Long fromAccountNumber,
                                                                @RequestParam Long toAccountNumber,
                                                                @RequestParam String userId);

    @PutMapping("/api/v1/accounts/{accountNumber}")
    ResponseEntity<AccountResponse> updateAccountBalance(@PathVariable Long accountNumber,
                                                         UpdateAccountBalanceRequest updateRequest);


}
