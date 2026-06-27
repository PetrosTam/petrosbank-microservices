package com.backendev.transactionservice.integration.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import com.backendev.transactionservice.service.AccountService;
import com.backendev.transactionservice.service.BalanceManager;
import com.backendev.transactionservice.service.SecurityService;
import com.backendev.transactionservice.service.TransactionHandler;
import com.backendev.transactionservice.service.TransactionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionHandlerIT {

    @Mock
    private SecurityService securityService;

    @Mock
    private TransactionProcessor transactionProcessor;

    @Mock
    private AccountService accountService;

    @Mock
    private BalanceManager balanceManager;

    private TransactionHandler transactionHandler;

    private static final Long ACCOUNT_NUMBER = 1234567890L;
    private static final Long TO_ACCOUNT_NUMBER = 9876543210L;
    private static final String USER_ID = "user-123";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(500);

    @BeforeEach
    void setUp() {
        transactionHandler = new TransactionHandler(securityService, transactionProcessor, accountService, balanceManager);
    }

    private TransactionRequest createTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setAmount(AMOUNT);
        request.setDescription("Test transaction");
        return request;
    }

    private TransferRequest createTransferRequest() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber(ACCOUNT_NUMBER);
        request.setToAccountNumber(TO_ACCOUNT_NUMBER);
        request.setAmount(AMOUNT);
        request.setDescription("Test transfer");
        return request;
    }

    private Transaction createTransaction(String txnId, TransactionType type) {
        Transaction txn = new Transaction();
        txn.setTransactionId(txnId);
        txn.setFromAccountNumber(ACCOUNT_NUMBER);
        txn.setAmount(AMOUNT);
        txn.setType(type);
        txn.setStatus(TransactionStatus.PENDING);
        return txn;
    }

    private TransactionResponse createTransactionResponse(String txnId) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(txnId);
        response.setAmount(AMOUNT);
        response.setStatus(TransactionStatus.COMPLETED);
        return response;
    }


    @Nested
    class ProcessDeposit{

        @Test
        void processTransaction_DepositSuccessfully() {
            TransactionRequest request = createTransactionRequest();
            Transaction transaction = createTransaction("TXN001", TransactionType.DEPOSIT);
            TransactionResponse response = createTransactionResponse("TXN001");

            when(securityService.getCurrentUserId()).thenReturn(USER_ID);
            doNothing().when(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.DEPOSIT))
                    .thenReturn(transaction);
            when(transactionProcessor.completeTransaction(transaction, AMOUNT))
                    .thenReturn(response);
            doNothing().when(transactionProcessor).syncBalanceToAccountService(ACCOUNT_NUMBER, AMOUNT);

            TransactionHandler.BalanceOperation operation = () -> AMOUNT;

            TransactionResponse result = transactionHandler.processTransaction(request, TransactionType.DEPOSIT, operation);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            verify(securityService).getCurrentUserId();
            verify(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
            verify(transactionProcessor).syncBalanceToAccountService(ACCOUNT_NUMBER, AMOUNT);
        }

        @Test
        void processTransaction_DepositThrowsException_ExceptionBubblesUp() {
            TransactionRequest request = createTransactionRequest();
            Transaction transaction = createTransaction("TXN001", TransactionType.DEPOSIT);
            TransactionProcessingException exception = new TransactionProcessingException("Balance update failed");

            when(securityService.getCurrentUserId()).thenReturn(USER_ID);
            doNothing().when(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.DEPOSIT))
                    .thenReturn(transaction);

            TransactionHandler.BalanceOperation operation = () -> {
                throw exception;
            };

            assertThatThrownBy(() -> transactionHandler.processTransaction(request, TransactionType.DEPOSIT, operation))
                    .isInstanceOf(TransactionProcessingException.class)
                    .hasMessage("Balance update failed");

            verify(transactionProcessor, never()).failTransaction(any(), any(), any());
            verify(transactionProcessor, never()).completeTransaction(any(), any());
        }

        @Test
        void processTransaction_ValidateOwnershipFails_ExceptionBubblesUp() {
            TransactionRequest request = createTransactionRequest();

            when(securityService.getCurrentUserId()).thenReturn(USER_ID);
            doThrow(new SecurityException("Access denied"))
                    .when(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);

            TransactionHandler.BalanceOperation operation = () -> AMOUNT;

            assertThatThrownBy(() -> transactionHandler.processTransaction(request, TransactionType.DEPOSIT, operation))
                    .isInstanceOf(SecurityException.class)
                    .hasMessage("Access denied");

            verify(transactionProcessor, never()).createAndSaveTransaction(any(), any());
        }
    }
    

    @Nested
    class ProcessWithdrawal{

        @Test
        void processTransaction_WithdrawalSuccessfully() {
            TransactionRequest request = createTransactionRequest();
            Transaction transaction = createTransaction("TXN002", TransactionType.WITHDRAWAL);
            TransactionResponse response = createTransactionResponse("TXN002");
            BigDecimal balanceAfterWithdrawal = BigDecimal.valueOf(9500);

            when(securityService.getCurrentUserId()).thenReturn(USER_ID);
            doNothing().when(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.WITHDRAWAL))
                    .thenReturn(transaction);
            when(transactionProcessor.completeTransaction(transaction, balanceAfterWithdrawal))
                    .thenReturn(response);
            doNothing().when(transactionProcessor).syncBalanceToAccountService(ACCOUNT_NUMBER, balanceAfterWithdrawal);

            TransactionHandler.BalanceOperation operation = () -> balanceAfterWithdrawal;

            TransactionResponse result = transactionHandler.processTransaction(request, TransactionType.WITHDRAWAL, operation);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            verify(transactionProcessor).syncBalanceToAccountService(ACCOUNT_NUMBER, balanceAfterWithdrawal);
        }

        @Test
        void processTransaction_WithdrawalThrowsInsufficientFunds_ExceptionBubblesUp() {
            TransactionRequest request = createTransactionRequest();
            Transaction transaction = createTransaction("TXN002", TransactionType.WITHDRAWAL);
            InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");

            when(securityService.getCurrentUserId()).thenReturn(USER_ID);
            doNothing().when(accountService).validateAccountAndOwnership(ACCOUNT_NUMBER, USER_ID);
            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.WITHDRAWAL))
                    .thenReturn(transaction);

            TransactionHandler.BalanceOperation operation = () -> {
                throw exception;
            };

            assertThatThrownBy(() -> transactionHandler.processTransaction(request, TransactionType.WITHDRAWAL, operation))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessage("Insufficient funds");

            verify(transactionProcessor, never()).completeTransaction(any(), any());
            verify(transactionProcessor, never()).syncBalanceToAccountService(any(), any());
        }
    }
    

    @Nested
    class ProcessTransfer{

        @Test
        void processTransferTransaction_SuccessfullyTransfersAndSyncs() {
            TransferRequest request = createTransferRequest();
            Transaction transaction = createTransaction("TXN003", TransactionType.TRANSFER);
            TransactionResponse response = createTransactionResponse("TXN003");
            BigDecimal fromBalance = BigDecimal.valueOf(4500);
            BigDecimal toBalance = BigDecimal.valueOf(5500);

            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER))
                    .thenReturn(transaction);
            doNothing().when(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
            doNothing().when(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
            doNothing().when(balanceManager).updateAccountBalance(TO_ACCOUNT_NUMBER, AMOUNT);
            when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenReturn(fromBalance);
            when(balanceManager.getBalance(TO_ACCOUNT_NUMBER)).thenReturn(toBalance);
            when(transactionProcessor.completeTransaction(transaction, fromBalance))
                    .thenReturn(response);
            doNothing().when(transactionProcessor).syncBalanceToAccountService(anyLong(), any(BigDecimal.class));

            TransactionResponse result = transactionHandler.processTransferTransaction(request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            verify(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
            verify(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
            verify(balanceManager).updateAccountBalance(TO_ACCOUNT_NUMBER, AMOUNT);
            verify(transactionProcessor).syncBalanceToAccountService(ACCOUNT_NUMBER, fromBalance);
            verify(transactionProcessor).syncBalanceToAccountService(TO_ACCOUNT_NUMBER, toBalance);
        }

        @Test
        void processTransferTransaction_InsufficientFunds_ThrowsException() {
            TransferRequest request = createTransferRequest();
            Transaction transaction = createTransaction("TXN003", TransactionType.TRANSFER);
            InsufficientFundsException exception = new InsufficientFundsException("Insufficient funds");

            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER))
                    .thenReturn(transaction);
            doThrow(exception).when(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);

            assertThatThrownBy(() -> transactionHandler.processTransferTransaction(request))
                    .isInstanceOf(InsufficientFundsException.class);

            verify(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
            verify(balanceManager, never()).updateAccountBalance(anyLong(), any(BigDecimal.class));
            verify(transactionProcessor, never()).completeTransaction(any(), any());
        }

        @Test
        void processTransferTransaction_InvalidAccount_ThrowsException() {
            TransferRequest request = createTransferRequest();
            Transaction transaction = createTransaction("TXN003", TransactionType.TRANSFER);
            InvalidAccountException exception = new InvalidAccountException("Invalid account");

            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER))
                    .thenReturn(transaction);
            doNothing().when(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
            doThrow(exception).when(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());

            assertThatThrownBy(() -> transactionHandler.processTransferTransaction(request))
                    .isInstanceOf(InvalidAccountException.class);

            verify(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
            verify(balanceManager, never()).updateAccountBalance(TO_ACCOUNT_NUMBER, AMOUNT);
            verify(transactionProcessor, never()).completeTransaction(any(), any());
        }

        @Test
        void processTransferTransaction_BothAccountsUpdated() {
            TransferRequest request = createTransferRequest();
            Transaction transaction = createTransaction("TXN003", TransactionType.TRANSFER);
            TransactionResponse response = createTransactionResponse("TXN003");

            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER))
                    .thenReturn(transaction);
            doNothing().when(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
            doNothing().when(balanceManager).updateAccountBalance(anyLong(), any(BigDecimal.class));
            when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenReturn(BigDecimal.valueOf(4500));
            when(balanceManager.getBalance(TO_ACCOUNT_NUMBER)).thenReturn(BigDecimal.valueOf(5500));
            when(transactionProcessor.completeTransaction(any(Transaction.class), any(BigDecimal.class)))
                    .thenReturn(response);
            doNothing().when(transactionProcessor).syncBalanceToAccountService(anyLong(), any(BigDecimal.class));

            transactionHandler.processTransferTransaction(request);

            verify(balanceManager).updateAccountBalance(ACCOUNT_NUMBER, AMOUNT.negate());
            verify(balanceManager).updateAccountBalance(TO_ACCOUNT_NUMBER, AMOUNT);
        }

        @Test
        void processTransferTransaction_BothAccountsSynced() {
            TransferRequest request = createTransferRequest();
            Transaction transaction = createTransaction("TXN003", TransactionType.TRANSFER);
            TransactionResponse response = createTransactionResponse("TXN003");
            BigDecimal fromBalance = BigDecimal.valueOf(4500);
            BigDecimal toBalance = BigDecimal.valueOf(5500);

            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER))
                    .thenReturn(transaction);
            doNothing().when(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
            doNothing().when(balanceManager).updateAccountBalance(anyLong(), any(BigDecimal.class));
            when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenReturn(fromBalance);
            when(balanceManager.getBalance(TO_ACCOUNT_NUMBER)).thenReturn(toBalance);
            when(transactionProcessor.completeTransaction(any(Transaction.class), any(BigDecimal.class)))
                    .thenReturn(response);
            doNothing().when(transactionProcessor).syncBalanceToAccountService(anyLong(), any(BigDecimal.class));

            transactionHandler.processTransferTransaction(request);

            verify(transactionProcessor).syncBalanceToAccountService(ACCOUNT_NUMBER, fromBalance);
            verify(transactionProcessor).syncBalanceToAccountService(TO_ACCOUNT_NUMBER, toBalance);
        }

        @Test
        void processTransferTransaction_ProcessingException_ThrowsException() {
            TransferRequest request = createTransferRequest();
            Transaction transaction = createTransaction("TXN003", TransactionType.TRANSFER);
            TransactionProcessingException exception = new TransactionProcessingException("Processing error");

            when(transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER))
                    .thenReturn(transaction);
            doNothing().when(balanceManager).validateSufficientFunds(ACCOUNT_NUMBER, AMOUNT);
            doNothing().when(balanceManager).updateAccountBalance(anyLong(), any(BigDecimal.class));
            when(balanceManager.getBalance(ACCOUNT_NUMBER)).thenThrow(exception);

            assertThatThrownBy(() -> transactionHandler.processTransferTransaction(request))
                    .isInstanceOf(TransactionProcessingException.class);

            verify(transactionProcessor, never()).syncBalanceToAccountService(any(), any());
        }
    }
    
}
