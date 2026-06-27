package com.backendev.transactionservice.integration.service;

import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.service.BalanceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceManagerIT {

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AccountBalanceMapper accountBalanceMapper;

    private BalanceManager balanceManager;

    private static final Long ACCOUNT_NUMBER = 1234567890L;

    @BeforeEach
    void setUp() {
        balanceManager = new BalanceManager(accountBalanceRepository, accountBalanceMapper);
    }

    private AccountBalance createAccountBalance(BigDecimal balance) {
        AccountBalance accountBalance = new AccountBalance();
        accountBalance.setAccountNumber(ACCOUNT_NUMBER);
        accountBalance.setBalance(balance);
        accountBalance.setLastUpdated(Instant.now());
        return accountBalance;
    }


    @Nested
    class UpdateAccountBalance{

        @Test
        void shouldUpdateExistingAccountBalance() {
            AccountBalance existing = createAccountBalance(BigDecimal.valueOf(1000));
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existing));
            when(accountBalanceRepository.save(any(AccountBalance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, BigDecimal.valueOf(500));

            verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
            verify(accountBalanceRepository).save(any(AccountBalance.class));
        }

        @Test
        void shouldCreateNewAccountBalanceIfNotExists() {
            AccountBalance newBalance = createAccountBalance(BigDecimal.ZERO);
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.empty());
            when(accountBalanceMapper.createAccountBalance(ACCOUNT_NUMBER, BigDecimal.ZERO))
                    .thenReturn(newBalance);
            when(accountBalanceRepository.save(any(AccountBalance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, BigDecimal.valueOf(500));

            verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
            verify(accountBalanceMapper).createAccountBalance(ACCOUNT_NUMBER, BigDecimal.ZERO);
            verify(accountBalanceRepository).save(any(AccountBalance.class));
        }

        @Test
        void shouldIncrementBalance() {
            AccountBalance existing = createAccountBalance(BigDecimal.valueOf(1000));
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existing));
            when(accountBalanceRepository.save(any(AccountBalance.class)))
                    .thenAnswer(invocation -> {
                        AccountBalance ab = invocation.getArgument(0);
                        assertThat(ab.getBalance()).isEqualTo(BigDecimal.valueOf(1500));
                        return ab;
                    });

            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, BigDecimal.valueOf(500));

            verify(accountBalanceRepository).save(any(AccountBalance.class));
        }

        @Test
        void shouldDecrementBalance() {
            AccountBalance existing = createAccountBalance(BigDecimal.valueOf(1000));
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existing));
            when(accountBalanceRepository.save(any(AccountBalance.class)))
                    .thenAnswer(invocation -> {
                        AccountBalance ab = invocation.getArgument(0);
                        assertThat(ab.getBalance()).isEqualTo(BigDecimal.valueOf(500));
                        return ab;
                    });

            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, BigDecimal.valueOf(-500));

            verify(accountBalanceRepository).save(any(AccountBalance.class));
        }

        @Test
        void shouldUpdateLastUpdatedTimestamp() {
            AccountBalance existing = createAccountBalance(BigDecimal.valueOf(1000));
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existing));
            when(accountBalanceRepository.save(any(AccountBalance.class)))
                    .thenAnswer(invocation -> {
                        AccountBalance ab = invocation.getArgument(0);
                        assertThat(ab.getLastUpdated()).isNotNull();
                        return ab;
                    });

            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, BigDecimal.valueOf(500));

            verify(accountBalanceRepository).save(any(AccountBalance.class));
        }
    }
    
    
    @Nested
    class GetBalance{

        @Test
        void shouldGetExistingBalance() {
            AccountBalance existing = createAccountBalance(BigDecimal.valueOf(2000));
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(existing));

            BigDecimal result = balanceManager.getBalance(ACCOUNT_NUMBER);

            assertThat(result).isEqualTo(BigDecimal.valueOf(2000));
            verify(accountBalanceRepository).findById(ACCOUNT_NUMBER);
        }

        @Test
        void shouldReturnZeroWhenAccountNotFound() {
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.empty());

            BigDecimal result = balanceManager.getBalance(ACCOUNT_NUMBER);

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }
    }
    
    
    @Nested
    class ValidateSufficientFunds{

        @Test
        void shouldValidateSufficientFunds() {
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(createAccountBalance(BigDecimal.valueOf(1000))));

            assertThatCode(() -> balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, BigDecimal.valueOf(500)))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowExceptionWhenInsufficientFunds() {
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(createAccountBalance(BigDecimal.valueOf(50))));

            BigDecimal testBalance = BigDecimal.valueOf(100);

            assertThatThrownBy(() ->
                    balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, testBalance))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient balance");
        }

        @Test
        void shouldThrowExceptionWhenBalanceEqualsRequired() {
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.of(createAccountBalance(BigDecimal.valueOf(500))));

            assertThatCode(() -> balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, BigDecimal.valueOf(500)))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldValidateFundsWhenAccountNotFound() {
            when(accountBalanceRepository.findById(ACCOUNT_NUMBER))
                    .thenReturn(Optional.empty());

            BigDecimal testBalance = BigDecimal.valueOf(100);

            assertThatThrownBy(() ->
                    balanceManager.validateSufficientFunds(ACCOUNT_NUMBER, testBalance))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient balance");
        }
    }
    
}
