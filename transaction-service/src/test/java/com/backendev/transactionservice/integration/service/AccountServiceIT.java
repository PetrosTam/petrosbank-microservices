package com.backendev.transactionservice.integration.service;

import com.backendev.transactionservice.client.AccountServiceClient;
import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.TransferValidationResponse;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.ServiceUnavailableException;
import com.backendev.transactionservice.service.AccountService;
import feign.FeignException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceIT {

    @Mock
    private AccountServiceClient accountServiceClient;

    private AccountService accountService;

    private static final Long ACCOUNT_NUMBER = 1234567890L;
    private static final Long TO_ACCOUNT_NUMBER = 9876543210L;
    private static final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountServiceClient);
    }

    private AccountResponse createAccountResponse(Long accountNumber, String userId) {
        AccountResponse response = new AccountResponse();
        response.setAccountNumber(accountNumber);
        response.setUserId(userId);
        response.setStatus("ACTIVE");
        return response;
    }
    
    @Nested
    class ValidateAccount{

        @Test
        void shouldValidateActiveAccount() {
            AccountResponse accountResponse = createAccountResponse(ACCOUNT_NUMBER, USER_ID);
            when(accountServiceClient.getAccount(ACCOUNT_NUMBER))
                    .thenReturn(ResponseEntity.ok(accountResponse));

            AccountResponse result = accountService.validateAccount(ACCOUNT_NUMBER);

            assertThat(result).isNotNull();
            assertThat(result.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            verify(accountServiceClient).getAccount(ACCOUNT_NUMBER);
        }

        @Test
        void shouldThrowExceptionWhenAccountInactive() {
            AccountResponse accountResponse = createAccountResponse(ACCOUNT_NUMBER, USER_ID);
            accountResponse.setStatus("INACTIVE");
            when(accountServiceClient.getAccount(ACCOUNT_NUMBER))
                    .thenReturn(ResponseEntity.ok(accountResponse));

            assertThatThrownBy(() -> accountService.validateAccount(ACCOUNT_NUMBER))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessage("Account not found or inactive");
        }

        @Test
        void shouldThrowExceptionWhenAccountNotFound() {
            when(accountServiceClient.getAccount(ACCOUNT_NUMBER))
                    .thenReturn(ResponseEntity.ok(null));

            assertThatThrownBy(() -> accountService.validateAccount(ACCOUNT_NUMBER))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessage("Account not found or inactive");
        }

        @Test
        void shouldThrowExceptionOnFeignException() {
            FeignException feignException = Mockito.mock(FeignException.class);
            when(accountServiceClient.getAccount(ACCOUNT_NUMBER))
                    .thenThrow(feignException);

            assertThatThrownBy(() -> accountService.validateAccount(ACCOUNT_NUMBER))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessage("Account validation failed");
        }
    }
    

    @Nested
    class ValidateAccountOwnership{

        @Test
        void shouldValidateAccountAndOwnership() {
            AccountResponse accountResponse = createAccountResponse(ACCOUNT_NUMBER, USER_ID);
            when(accountServiceClient.getAccount(ACCOUNT_NUMBER))
                    .thenReturn(ResponseEntity.ok(accountResponse));

            assertThatCode(() -> accountService.validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowExceptionWhenOwnershipMismatch() {
            AccountResponse accountResponse = createAccountResponse(ACCOUNT_NUMBER, "other-user");
            when(accountServiceClient.getAccount(ACCOUNT_NUMBER))
                    .thenReturn(ResponseEntity.ok(accountResponse));

            assertThatThrownBy(() -> accountService.validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID))
                    .isInstanceOf(SecurityException.class)
                    .hasMessage("Access denied: Account does not belong to current user");
        }
    }


    @Nested
    class SyncBalance{

        @Test
        void shouldSyncBalanceWithAccountService() {
            AccountResponse accountResponse = createAccountResponse(ACCOUNT_NUMBER, USER_ID);
            when(accountServiceClient.updateAccountBalance(eq(ACCOUNT_NUMBER), any(UpdateAccountBalanceRequest.class)))
                    .thenReturn(ResponseEntity.ok(accountResponse));

            assertThatCode(() -> accountService.syncBalanceWithAccountService(ACCOUNT_NUMBER, BigDecimal.valueOf(5000)))
                    .doesNotThrowAnyException();

            verify(accountServiceClient).updateAccountBalance(eq(ACCOUNT_NUMBER), any(UpdateAccountBalanceRequest.class));
        }

        @Test
        void shouldThrowExceptionWhenBalanceIsNull() {
            assertThatThrownBy(() -> accountService.syncBalanceWithAccountService(ACCOUNT_NUMBER, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Balance cannot be null");
        }

        @Test
        void shouldThrowExceptionWhenSyncFails() {
            FeignException feignException = Mockito.mock(FeignException.class);
            when(accountServiceClient.updateAccountBalance(eq(ACCOUNT_NUMBER), any(UpdateAccountBalanceRequest.class)))
                    .thenThrow(feignException);

            BigDecimal testBalance = BigDecimal.valueOf(5000);

            assertThatThrownBy(() -> accountService.syncBalanceWithAccountService(ACCOUNT_NUMBER, testBalance))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessage("Account validation failed");
        }
    }


    @Nested
    class ValidateTransfer{

        @Test
        void shouldValidateTransferAccounts() {
            TransferValidationResponse transferResponse = new TransferValidationResponse(
                    createAccountResponse(ACCOUNT_NUMBER, USER_ID),
                    createAccountResponse(TO_ACCOUNT_NUMBER, "other-user")
            );
            when(accountServiceClient.validateTransfer(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .thenReturn(ResponseEntity.ok(transferResponse));

            assertThatCode(() -> accountService.validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .doesNotThrowAnyException();

            verify(accountServiceClient).validateTransfer(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID);
        }

        @Test
        void shouldThrowExceptionWhenTransferValidationReturnsNull() {
            when(accountServiceClient.validateTransfer(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .thenReturn(ResponseEntity.ok(null));

            assertThatThrownBy(() -> accountService.validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessage("Failed to validate transfer accounts");
        }

        @Test
        void shouldThrowServiceUnavailableExceptionOnRetryableException() {
            RetryableException retryableException = Mockito.mock(RetryableException.class);
            when(accountServiceClient.validateTransfer(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .thenThrow(retryableException);

            assertThatThrownBy(() -> accountService.validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessage("Account service is currently unavailable. Please try again later.");
        }

        @Test
        void shouldThrowFeignExceptionOnOtherErrors() {
            FeignException feignException = Mockito.mock(FeignException.class);
            when(accountServiceClient.validateTransfer(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .thenThrow(feignException);

            assertThatThrownBy(() -> accountService.validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID))
                    .isInstanceOf(FeignException.class);
        }
    }
    
}
