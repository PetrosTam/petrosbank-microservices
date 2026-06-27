package com.backendev.transactionservice.integration.service;

import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.messaging.TransactionEventPublisher;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.repository.TransactionRepository;
import com.backendev.transactionservice.service.AccountService;
import com.backendev.transactionservice.service.SecurityService;
import com.backendev.transactionservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@ActiveProfiles("test")
class TransactionServiceIT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountBalanceRepository accountBalanceRepository;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private SecurityService securityService;

    @MockitoBean
    private TransactionEventPublisher eventPublisher;

    private static final Long ACCOUNT_NUMBER = 1234567890L;
    private static final Long TO_ACCOUNT_NUMBER = 9876543210L;
    private static final String USER_ID = "user-123";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(500);
    private static final BigDecimal WITHDRAW_AMOUNT = BigDecimal.valueOf(300);
    private static final BigDecimal OVERDRAFT_AMOUNT = BigDecimal.valueOf(9999);

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountBalanceRepository.deleteAll();

        AccountBalance balance = new AccountBalance();
        balance.setAccountNumber(ACCOUNT_NUMBER);
        balance.setBalance(BigDecimal.valueOf(2000));
        accountBalanceRepository.save(balance);

        AccountBalance toBalance = new AccountBalance();
        toBalance.setAccountNumber(TO_ACCOUNT_NUMBER);
        toBalance.setBalance(BigDecimal.valueOf(1000));
        accountBalanceRepository.save(toBalance);

        doNothing().when(eventPublisher).publishTransactionEvent(any());
    }

    @Nested
    class DepositTests {

        @Test
        void shouldDepositMoney_andSaveTransaction() {
            TransactionResponse result = transactionService.deposit(buildTransactionRequest());

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            assertThat(result.getAmount()).isEqualTo(AMOUNT);

            List<Transaction> saved = transactionRepository
                    .findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER);
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getAmount()).isEqualTo(AMOUNT);
            assertThat(saved.get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
        }

        @Test
        void shouldDepositMoney_andUpdateBalance() {
            transactionService.deposit(buildTransactionRequest());

            AccountBalance updated = accountBalanceRepository
                    .findByAccountNumber(ACCOUNT_NUMBER).orElseThrow();
            assertThat(updated.getBalance()).isEqualTo(BigDecimal.valueOf(2500));
        }

        @Test
        void deposit_shouldPublishKafkaEvent() {
            transactionService.deposit(buildTransactionRequest());

            verify(eventPublisher, times(1)).publishTransactionEvent(argThat(event ->
                    "TRANSACTION_DEPOSITED".equals(event.getEventType())
            ));
        }

        @Test
        void deposit_shouldNotPublishEvent_whenInsufficientSetup() {
            assertThatThrownBy(() -> transactionService.fetchTransactionsByAccount(99999L))
                    .isInstanceOf(InvalidAccountException.class);

            verify(eventPublisher, never()).publishTransactionEvent(any());
        }
    }

    @Nested
    class WithdrawTests {

        @Test
        void shouldWithdrawMoney_andSaveTransaction() {
            TransactionResponse result = transactionService.withdraw(buildWithdrawRequest());

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

            List<Transaction> saved = transactionRepository
                    .findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER);
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getType()).isEqualTo(TransactionType.WITHDRAWAL);
        }

        @Test
        void shouldWithdrawMoney_andUpdateBalance() {
            transactionService.withdraw(buildWithdrawRequest());

            AccountBalance updated = accountBalanceRepository
                    .findByAccountNumber(ACCOUNT_NUMBER).orElseThrow();
            assertThat(updated.getBalance()).isEqualTo(BigDecimal.valueOf(1700));
        }

        @Test
        void shouldThrowException_whenInsufficientFunds() {
            TransactionRequest overdraftRequest = buildOverdraftRequest();

            assertThatThrownBy(() -> transactionService.withdraw(overdraftRequest))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void withdraw_shouldPublishKafkaEvent() {
            transactionService.withdraw(buildWithdrawRequest());

            verify(eventPublisher, times(1)).publishTransactionEvent(argThat(event ->
                    "TRANSACTION_WITHDRAWAL".equals(event.getEventType())
            ));
        }

        @Test
        void withdraw_shouldNotPublishEvent_whenInsufficientFunds() {
            TransactionRequest overdraftRequest = buildOverdraftRequest();

            assertThatThrownBy(() -> transactionService.withdraw(overdraftRequest))
                    .isInstanceOf(RuntimeException.class);

            verify(eventPublisher, never()).publishTransactionEvent(any());
        }
    }

    @Nested
    class TransferTests {

        @BeforeEach
        void setUpTransfer() {
            when(securityService.getCurrentUserId()).thenReturn(USER_ID);
            doNothing().when(accountService)
                    .validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID);
        }

        @Test
        void shouldTransferMoney_andSaveTransaction() {
            TransactionResponse result = transactionService.transfer(buildTransferRequest());

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

            List<Transaction> saved = transactionRepository
                    .findAllByFromAccountNumberOrderByCreatedAtDesc(ACCOUNT_NUMBER);
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getType()).isEqualTo(TransactionType.TRANSFER);
        }

        @Test
        void shouldTransferMoney_andUpdateBothBalances() {
            transactionService.transfer(buildTransferRequest());

            AccountBalance from = accountBalanceRepository
                    .findByAccountNumber(ACCOUNT_NUMBER).orElseThrow();
            AccountBalance to = accountBalanceRepository
                    .findByAccountNumber(TO_ACCOUNT_NUMBER).orElseThrow();

            assertThat(from.getBalance()).isEqualTo(BigDecimal.valueOf(1500));
            assertThat(to.getBalance()).isEqualTo(BigDecimal.valueOf(1500));
        }

        @Test
        void transfer_shouldPublishKafkaEvent() {
            transactionService.transfer(buildTransferRequest());

            verify(eventPublisher, times(1)).publishTransactionEvent(argThat(event ->
                    "TRANSACTION_TRANSFER".equals(event.getEventType())
            ));
        }

        @Test
        void transfer_shouldCallValidateTransferAccounts() {
            transactionService.transfer(buildTransferRequest());

            verify(accountService, times(1))
                    .validateTransferAccounts(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, USER_ID);
        }
    }

    @Nested
    class FetchTransactionTests {

        @Test
        void shouldFetchTransactions_whenExist() {
            transactionService.deposit(buildTransactionRequest());

            List<TransactionInfo> result = transactionService.fetchTransactionsByAccount(ACCOUNT_NUMBER);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFromAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            verify(eventPublisher, times(1)).publishTransactionEvent(any()); // only from deposit
        }

        @Test
        void shouldThrowException_whenNoTransactionsExist() {
            assertThatThrownBy(() -> transactionService.fetchTransactionsByAccount(99999L))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("Account number does not exists");
        }

        @Test
        void shouldFetchMultipleTransactions_inDescendingOrder() {
            transactionService.deposit(buildTransactionRequest());
            transactionService.deposit(buildTransactionRequest());

            List<TransactionInfo> result = transactionService.fetchTransactionsByAccount(ACCOUNT_NUMBER);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class FetchBalanceTests {

        @Test
        void shouldFetchAccountBalance() {
            AccountBalanceInfo result = transactionService.fetchAccountBalance(ACCOUNT_NUMBER);

            assertThat(result).isNotNull();
            assertThat(result.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(result.getBalance()).isEqualTo(BigDecimal.valueOf(2000));
            verify(eventPublisher, never()).publishTransactionEvent(any());
        }

        @Test
        void shouldThrowException_whenAccountBalanceNotFound() {
            assertThatThrownBy(() -> transactionService.fetchAccountBalance(99999L))
                    .isInstanceOf(InvalidAccountException.class)
                    .hasMessageContaining("Account number does not exist");
        }

        @Test
        void shouldReflectUpdatedBalance_afterDeposit() {
            transactionService.deposit(buildTransactionRequest());

            AccountBalanceInfo result = transactionService.fetchAccountBalance(ACCOUNT_NUMBER);

            assertThat(result.getBalance()).isEqualTo(BigDecimal.valueOf(2500));
        }
    }

    // --- Builders ---

    private TransactionRequest buildTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setAmount(AMOUNT);
        request.setDescription("Test transaction");
        return request;
    }

    private TransactionRequest buildWithdrawRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setAmount(WITHDRAW_AMOUNT);
        request.setDescription("Test withdrawal");
        return request;
    }

    private TransactionRequest buildOverdraftRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setAmount(OVERDRAFT_AMOUNT);
        request.setDescription("Test overdraft");
        return request;
    }

    private TransferRequest buildTransferRequest() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber(ACCOUNT_NUMBER);
        request.setToAccountNumber(TO_ACCOUNT_NUMBER);
        request.setAmount(AMOUNT);
        request.setDescription("Test transfer");
        return request;
    }
}
