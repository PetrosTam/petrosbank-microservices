package com.backendev.transactionservice.unit.service;

import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.service.BalanceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceManagerTest {

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    @InjectMocks
    private BalanceManager balanceManager;

    private static final Long ACCOUNT_NUMBER = 12345L;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");

    private AccountBalance accountBalance;

    @BeforeEach
    void setUp() {
        accountBalance = new AccountBalance(ACCOUNT_NUMBER, INITIAL_BALANCE, Instant.now());
    }

    @Test
    void updateAccountBalance_ExistingAccount_UpdatesBalance() {
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(accountBalance));
        when(accountBalanceRepository.save(any(AccountBalance.class)))
                .thenReturn(accountBalance);

        balanceManager.updateAccountBalance(ACCOUNT_NUMBER, AMOUNT);

        verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
        verify(accountBalanceRepository).save(accountBalance);
        assertEquals(new BigDecimal("1250.00"), accountBalance.getBalance());
        assertNotNull(accountBalance.getLastUpdated());
    }

    @Test
    void updateAccountBalance_NewAccount_CreatesAndUpdatesBalance() {
        AccountBalance newAccountBalance = new AccountBalance(ACCOUNT_NUMBER, BigDecimal.ZERO, Instant.now());

        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.empty());
        when(accountBalanceMapper.createAccountBalance(ACCOUNT_NUMBER, BigDecimal.ZERO))
                .thenReturn(newAccountBalance);
        when(accountBalanceRepository.save(any(AccountBalance.class)))
                .thenReturn(newAccountBalance);

        balanceManager.updateAccountBalance(ACCOUNT_NUMBER, AMOUNT);

        verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
        verify(accountBalanceMapper).createAccountBalance(ACCOUNT_NUMBER, BigDecimal.ZERO);
        verify(accountBalanceRepository).save(newAccountBalance);
        assertEquals(AMOUNT, newAccountBalance.getBalance());
    }

    @Test
    void updateAccountBalance_NegativeAmount_DecreasesBalance() {
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(accountBalance));
        when(accountBalanceRepository.save(any(AccountBalance.class)))
                .thenReturn(accountBalance);

        balanceManager.updateAccountBalance(ACCOUNT_NUMBER, negativeAmount);

        verify(accountBalanceRepository).save(accountBalance);
        assertEquals(new BigDecimal("900.00"), accountBalance.getBalance());
    }

    @Test
    void validateSufficientFunds_SufficientBalance_NoException() {
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(accountBalance));

        balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);

        verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
    }

    @Test
    void validateSufficientFunds_ExactBalance_NoException() {
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(accountBalance));

        balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, INITIAL_BALANCE);

        verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
    }

    @Test
    void validateSufficientFunds_InsufficientBalance_ThrowsException() {
        BigDecimal largeAmount = new BigDecimal("2000.00");
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(accountBalance));

        assertThrows(InsufficientFundsException.class,
                () -> balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, largeAmount));

        verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
    }

    @Test
    void validateSufficientFunds_AccountNotFound_ThrowsException() {
        BigDecimal amount = new BigDecimal("100.00");
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(InsufficientFundsException.class,
                () -> balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, amount));
    }

    @Test
    void getBalance_ExistingAccount_ReturnsBalance() {
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(accountBalance));

        BigDecimal result = balanceManager.getBalance(ACCOUNT_NUMBER);

        assertEquals(INITIAL_BALANCE, result);
        verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
    }

    @Test
    void getBalance_AccountNotFound_ReturnsZero() {
        when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                .thenReturn(Optional.empty());

        BigDecimal result = balanceManager.getBalance(ACCOUNT_NUMBER);

        assertEquals(BigDecimal.ZERO, result);
        verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
    }
}