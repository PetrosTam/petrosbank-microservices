package com.backendev.transactionservice.unit.service;

import com.backendev.transactionservice.client.AccountServiceClient;
import com.backendev.transactionservice.dto.AccountResponse;
import com.backendev.transactionservice.dto.UpdateAccountBalanceRequest;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.service.AccountService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private AccountService accountService;

    private AccountResponse activeAccount;

    private static final BigDecimal TEST_BALANCE = new BigDecimal("500.00");


    @BeforeEach
    void setUp() {
        activeAccount = new AccountResponse(12345L, "user123", "CHECKING", "ACTIVE", Instant.now());
    }

    @Test
    void validateAccountAndOwnership_Success() {
        when(accountServiceClient.getAccount(12345L))
                .thenReturn(ResponseEntity.ok(activeAccount));

        accountService.validateAccountAndOwnership(12345L, "user123");
        verify(accountServiceClient).getAccount(12345L);
    }

    @Test
    void validateAccountAndOwnership_WrongUser_ThrowsSecurityException() {
        when(accountServiceClient.getAccount(12345L))
                .thenReturn(ResponseEntity.ok(activeAccount));

        assertThrows(SecurityException.class,
                () -> accountService.validateAccountAndOwnership(12345L, "wrongUser"));
    }

    @Test
    void validateAccount_Success() {
        when(accountServiceClient.getAccount(12345L))
                .thenReturn(ResponseEntity.ok(activeAccount));

        AccountResponse result = accountService.validateAccount(12345L);

        assertNotNull(result);
        assertEquals(12345L, result.getAccountNumber());
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void validateAccount_NullBody_ThrowsInvalidAccountException() {
        when(accountServiceClient.getAccount(12345L))
                .thenReturn(ResponseEntity.ok(null));

        assertThrows(InvalidAccountException.class,
                () -> accountService.validateAccount(12345L));
    }

    @Test
    void validateAccount_InactiveAccount_ThrowsInvalidAccountException() {
        activeAccount.setStatus("SUSPENDED");
        when(accountServiceClient.getAccount(12345L))
                .thenReturn(ResponseEntity.ok(activeAccount));

        assertThrows(InvalidAccountException.class,
                () -> accountService.validateAccount(12345L));
    }

    @Test
    void validateAccount_FeignException_ThrowsInvalidAccountException() {
        when(accountServiceClient.getAccount(12345L))
                .thenThrow(mock(FeignException.class));

        assertThrows(InvalidAccountException.class,
                () -> accountService.validateAccount(12345L));
    }


    @Test
    void syncBalanceWithAccountService_Success() {
        when(accountServiceClient.updateAccountBalance(eq(12345L), any()))
                .thenReturn(ResponseEntity.ok(activeAccount));

        accountService.syncBalanceWithAccountService(12345L, new BigDecimal("1000.00"));

        verify(accountServiceClient).updateAccountBalance(eq(12345L), any(UpdateAccountBalanceRequest.class));
    }

    @Test
    void syncBalanceWithAccountService_NullBalance_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.syncBalanceWithAccountService(12345L, null));

        verifyNoInteractions(accountServiceClient);
    }

    @Test
    void syncBalanceWithAccountService_NullResponse_ThrowsInvalidAccountException() {
        when(accountServiceClient.updateAccountBalance(eq(12345L), any()))
                .thenReturn(ResponseEntity.ok(null));

        assertThrows(InvalidAccountException.class,
                () -> accountService.syncBalanceWithAccountService(12345L, TEST_BALANCE));
    }

    @Test
    void syncBalanceWithAccountService_InactiveAccount_ThrowsInvalidAccountException() {
        activeAccount.setStatus("CLOSED");
        when(accountServiceClient.updateAccountBalance(eq(12345L), any()))
                .thenReturn(ResponseEntity.ok(activeAccount));

        assertThrows(InvalidAccountException.class,
                () -> accountService.syncBalanceWithAccountService(12345L, TEST_BALANCE));
    }

    @Test
    void syncBalanceWithAccountService_FeignException_ThrowsInvalidAccountException() {
        when(accountServiceClient.updateAccountBalance(eq(12345L), any()))
                .thenThrow(mock(FeignException.class));

        assertThrows(InvalidAccountException.class,
                () -> accountService.syncBalanceWithAccountService(12345L, TEST_BALANCE));
    }

}