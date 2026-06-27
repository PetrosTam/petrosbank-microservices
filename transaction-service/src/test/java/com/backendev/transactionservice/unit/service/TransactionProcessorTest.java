package com.backendev.transactionservice.unit.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.repository.TransactionRepository;
import com.backendev.transactionservice.service.AccountService;
import com.backendev.transactionservice.service.TransactionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private TransactionProcessor transactionProcessor;

    private static final Long ACCOUNT_NUMBER = 12345L;
    private static final Long TO_ACCOUNT_NUMBER = 67890L;
    private static final BigDecimal AMOUNT = new BigDecimal("500.00");
    private static final BigDecimal NEW_BALANCE = new BigDecimal("1500.00");
    private static final String TRANSACTION_ID = "TXN123456";

    private TransactionRequest transactionRequest;
    private TransferRequest transferRequest;
    private Transaction transaction;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        transactionRequest = new TransactionRequest();
        transactionRequest.setAccountNumber(ACCOUNT_NUMBER);
        transactionRequest.setAmount(AMOUNT);

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountNumber(ACCOUNT_NUMBER);
        transferRequest.setToAccountNumber(TO_ACCOUNT_NUMBER);
        transferRequest.setAmount(AMOUNT);

        transaction = new Transaction();
        transaction.setTransactionId(TRANSACTION_ID);
        transaction.setFromAccountNumber(ACCOUNT_NUMBER);
        transaction.setAmount(AMOUNT);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(Instant.now());

        transactionResponse = new TransactionResponse();
        transactionResponse.setTransactionId(TRANSACTION_ID);
        transactionResponse.setStatus(TransactionStatus.COMPLETED);
        transactionResponse.setAccountBalance(NEW_BALANCE);
    }

    @Test
    void createAndSaveTransaction_Deposit_Success() {
        when(transactionMapper.fromDepositRequest(transactionRequest)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionProcessor.createAndSaveTransaction(transactionRequest, TransactionType.DEPOSIT);

        assertNotNull(result);
        assertNotNull(result.getTransactionId());
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());
        verify(transactionMapper).fromDepositRequest(transactionRequest);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void createAndSaveTransaction_Withdrawal_Success() {
        when(transactionMapper.fromWithdrawalRequest(transactionRequest)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionProcessor.createAndSaveTransaction(transactionRequest, TransactionType.WITHDRAWAL);

        assertNotNull(result);
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(transactionMapper).fromWithdrawalRequest(transactionRequest);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void createAndSaveTransaction_Transfer_Success() {
        when(transactionMapper.fromTransferRequest(transferRequest)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionProcessor.createAndSaveTransaction(transferRequest, TransactionType.TRANSFER);

        assertNotNull(result);
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(transactionMapper).fromTransferRequest(transferRequest);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void completeTransaction_Success() {
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponseWithBalance(transaction, NEW_BALANCE)).thenReturn(transactionResponse);

        transactionProcessor.completeTransaction(transaction, NEW_BALANCE);

        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertNotNull(transaction.getUpdatedAt());
        verify(transactionRepository).save(transaction);
        verify(transactionMapper).toResponseWithBalance(transaction, NEW_BALANCE);
    }

    @Test
    void syncBalanceToAccountService_Success() {
        transactionProcessor.syncBalanceToAccountService(ACCOUNT_NUMBER, NEW_BALANCE);
        verify(accountService).syncBalanceWithAccountService(ACCOUNT_NUMBER, NEW_BALANCE);
    }

    @Test
    void completeTransaction_UpdatesTransactionStatus() {
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponseWithBalance(transaction, NEW_BALANCE)).thenReturn(transactionResponse);

        transactionProcessor.completeTransaction(transaction, NEW_BALANCE);

        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertNotNull(transaction.getUpdatedAt());
    }

    @Test
    void failTransaction_ThrowsTransactionProcessingException() {
        Exception cause = new RuntimeException("Test error");
        String errorMessage = "Transaction failed";

        assertThrows(TransactionProcessingException.class,
                () -> transactionProcessor.failTransaction(transaction, errorMessage, cause));

        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertNotNull(transaction.getUpdatedAt());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void failTransaction_SavesFailedTransaction() {
        Exception cause = new InsufficientFundsException("Insufficient funds");
        String errorMessage = "Withdrawal failed";

        try {
            transactionProcessor.failTransaction(transaction, errorMessage, cause);
        } catch (TransactionProcessingException e) {
            // Expected exception
        }

        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void failTransaction_WrapsOriginalException() {
        InsufficientFundsException cause = new InsufficientFundsException("Insufficient funds");
        String errorMessage = "Transaction failed";

        TransactionProcessingException exception = assertThrows(TransactionProcessingException.class,
                () -> transactionProcessor.failTransaction(transaction, errorMessage, cause));

        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void generateTransactionId_StartsWithTXN() {
        when(transactionMapper.fromDepositRequest(transactionRequest)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        Transaction result = transactionProcessor.createAndSaveTransaction(transactionRequest, TransactionType.DEPOSIT);

        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("TXN"));
    }
}