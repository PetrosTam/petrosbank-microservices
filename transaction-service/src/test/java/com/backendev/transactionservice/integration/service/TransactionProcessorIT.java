package com.backendev.transactionservice.integration.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.repository.TransactionRepository;
import com.backendev.transactionservice.service.AccountService;
import com.backendev.transactionservice.service.TransactionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorIT {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private AccountService accountService;

    private TransactionProcessor transactionProcessor;

    private static final Long ACCOUNT_NUMBER = 1234567890L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(500);

    @BeforeEach
    void setUp() {
        transactionProcessor = new TransactionProcessor(transactionRepository, transactionMapper, accountService);
    }

    private Transaction createTransaction(String txnId, TransactionType type) {
        Transaction txn = new Transaction();
        txn.setTransactionId(txnId);
        txn.setFromAccountNumber(ACCOUNT_NUMBER);
        txn.setAmount(AMOUNT);
        txn.setType(type);
        txn.setStatus(TransactionStatus.COMPLETED);
        txn.setDescription("Test transaction");
        return txn;
    }

    private TransactionRequest createTransactionRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setAmount(AMOUNT);
        request.setDescription("Deposit");
        return request;
    }


    @Nested
    class CreateAndSaveTransaction{

        @Test
        void shouldCreateAndSaveDepositTransaction() {
            TransactionRequest request = createTransactionRequest();
            Transaction transaction = createTransaction("TXN123", TransactionType.DEPOSIT);

            when(transactionMapper.fromDepositRequest(request)).thenReturn(transaction);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            Transaction result = transactionProcessor.createAndSaveTransaction(request, TransactionType.DEPOSIT);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(result.getCreatedAt()).isNotNull();
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void shouldCreateAndSaveWithdrawalTransaction() {
            TransactionRequest request = createTransactionRequest();
            Transaction transaction = createTransaction("TXN124", TransactionType.WITHDRAWAL);

            when(transactionMapper.fromWithdrawalRequest(request)).thenReturn(transaction);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            Transaction result = transactionProcessor.createAndSaveTransaction(request, TransactionType.WITHDRAWAL);

            assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
            verify(transactionMapper).fromWithdrawalRequest(request);
        }

        @Test
        void shouldCreateAndSaveTransferTransaction() {
            TransferRequest request = new TransferRequest();
            request.setFromAccountNumber(ACCOUNT_NUMBER);
            request.setToAccountNumber(9876543210L);
            request.setAmount(AMOUNT);
            request.setDescription("Transfer");

            Transaction transaction = createTransaction("TXN125", TransactionType.TRANSFER);

            when(transactionMapper.fromTransferRequest(request)).thenReturn(transaction);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            Transaction result = transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER);

            assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
            verify(transactionMapper).fromTransferRequest(request);
        }

        @Test
        void shouldGenerateUniqueTransactionId() {
            TransactionRequest request = createTransactionRequest();
            Transaction transaction = createTransaction("TXN123", TransactionType.DEPOSIT);

            when(transactionMapper.fromDepositRequest(request)).thenReturn(transaction);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

            Transaction result = transactionProcessor.createAndSaveTransaction(request, TransactionType.DEPOSIT);

            assertThat(result.getTransactionId()).isNotEmpty();
            assertThat(result.getTransactionId()).startsWith("TXN");
        }
    }
    
    
    @Nested
    class CompleteTransaction{
        
        @Test
        void shouldCompleteTransaction() {
            Transaction transaction = createTransaction("TXN123", TransactionType.DEPOSIT);
            TransactionResponse response = new TransactionResponse();
            response.setTransactionId("TXN123");
            response.setStatus(TransactionStatus.COMPLETED);

            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
            when(transactionMapper.toResponseWithBalance(any(Transaction.class), eq(AMOUNT)))
                    .thenReturn(response);

            TransactionResponse result = transactionProcessor.completeTransaction(transaction, AMOUNT);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void shouldSetUpdatedAtWhenCompletingTransaction() {
            Transaction transaction = createTransaction("TXN123", TransactionType.DEPOSIT);
            TransactionResponse response = new TransactionResponse();

            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                assertThat(txn.getUpdatedAt()).isNotNull();
                return txn;
            });
            when(transactionMapper.toResponseWithBalance(any(Transaction.class), any(BigDecimal.class)))
                    .thenReturn(response);

            transactionProcessor.completeTransaction(transaction, AMOUNT);

            verify(transactionRepository).save(any(Transaction.class));
        }
    }
    
    
    @Nested
    class SyncBalance{

        @Test
        void shouldSyncBalanceSuccessfully() {
            doNothing().when(accountService).syncBalanceWithAccountService(ACCOUNT_NUMBER, AMOUNT);

            assertThatCode(() -> transactionProcessor.syncBalanceToAccountService(ACCOUNT_NUMBER, AMOUNT))
                    .doesNotThrowAnyException();

            verify(accountService).syncBalanceWithAccountService(ACCOUNT_NUMBER, AMOUNT);
        }

        @Test
        void shouldHandleSyncBalanceException() {
            doThrow(new RuntimeException("Sync failed")).when(accountService)
                    .syncBalanceWithAccountService(ACCOUNT_NUMBER, AMOUNT);

            assertThatCode(() -> transactionProcessor.syncBalanceToAccountService(ACCOUNT_NUMBER, AMOUNT))
                    .doesNotThrowAnyException();

            verify(accountService).syncBalanceWithAccountService(ACCOUNT_NUMBER, AMOUNT);
        }
    }
    

    @Nested
    class FailTransaction{

        @Test
        void shouldFailTransaction() {
            Transaction transaction = createTransaction("TXN123", TransactionType.DEPOSIT);
            Exception cause = new RuntimeException("Insufficient funds");

            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            assertThatThrownBy(() -> transactionProcessor.failTransaction(transaction, "Deposit failed", cause))
                    .isInstanceOf(TransactionProcessingException.class)
                    .hasMessage("Deposit failed");

            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void shouldSetFailedStatusAndUpdatedAt() {
            Transaction transaction = createTransaction("TXN123", TransactionType.DEPOSIT);
            Exception cause = new RuntimeException("Error");

            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
                Transaction txn = invocation.getArgument(0);
                assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FAILED);
                assertThat(txn.getUpdatedAt()).isNotNull();
                return txn;
            });

            assertThatThrownBy(() -> transactionProcessor.failTransaction(transaction, "Failed", cause))
                    .isInstanceOf(TransactionProcessingException.class);

            verify(transactionRepository).save(any(Transaction.class));
        }
    }
    
}
