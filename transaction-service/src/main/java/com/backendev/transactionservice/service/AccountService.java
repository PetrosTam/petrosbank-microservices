package com.backendev.transactionservice.service;

import com.backendev.transactionservice.client.AccountServiceClient;
import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.TransferValidationResponse;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class AccountService {

    private static final String ACCOUNT_ACTIVE = "ACTIVE";

    private final AccountServiceClient accountServiceClient;

    public AccountService(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    public void validateAccountAndOwnership(Long accountNumber, String currentUserId) {
        log.info("Validating account {} for the user: {}", accountNumber, currentUserId);
        AccountResponse account = validateAccount(accountNumber);
        if (!currentUserId.equals(account.getUserId())) {
            log.warn("Access denied: User {} attempted to access account {} owned by {}",
                    currentUserId, account.getAccountNumber(), account.getUserId());
            throw new SecurityException("Access denied: Account does not belong to current user");
        }
    }

    public AccountResponse validateAccount(Long accountNumber) {
        try {
            log.info("Validating account {} via account service", accountNumber);
            ResponseEntity<AccountResponse> response = accountServiceClient.getAccount(accountNumber);

            return Optional.ofNullable(response.getBody())
                    .filter(acc -> ACCOUNT_ACTIVE.equals(acc.getStatus()))
                    .orElseThrow(() -> new InvalidAccountException("Account not found or inactive"));

        } catch (FeignException e) {
            log.error("Feign exception", e);
            throw new InvalidAccountException("Account validation failed", e);
        }
    }

    public void syncBalanceWithAccountService(Long accountNumber, BigDecimal newBalance) {
        try {
            log.info("Updating account {} with balance: {}", accountNumber, newBalance);

            if (newBalance == null) {
                throw new IllegalArgumentException("Balance cannot be null");
            }

            UpdateAccountBalanceRequest updateRequest = new UpdateAccountBalanceRequest(accountNumber, newBalance);
            ResponseEntity<AccountResponse> response = accountServiceClient.updateAccountBalance(accountNumber, updateRequest);

            Optional.ofNullable(response.getBody())
                    .filter(acc -> ACCOUNT_ACTIVE.equals(acc.getStatus()))
                    .orElseThrow(() -> new InvalidAccountException("Account not found or inactive"));

        } catch (FeignException e) {
            log.error("Unexpected error calling account service", e);
            throw new InvalidAccountException("Account validation failed", e);
        }
    }

    public void validateTransferAccounts(Long fromAccount, Long toAccount, String currentUserId){

        try {
            log.info("Validating transfer from account {} to account {} for user {}",
                    fromAccount, toAccount, currentUserId);

            ResponseEntity<TransferValidationResponse> response =
                    accountServiceClient.validateTransfer(fromAccount, toAccount, currentUserId);

            Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new InvalidAccountException(
                            "Failed to validate transfer accounts"));

            log.info("Transfer validation successful");

        }  catch (RetryableException exception) {
            log.error("Account service is unavailable", exception);
            throw new ServiceUnavailableException("Account service is currently unavailable. Please try again later.");

        } catch (FeignException exception) {
            log.error("Unexpected error calling account service", exception);
            throw exception;
        }
    }

}
