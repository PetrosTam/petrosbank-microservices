package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.exception.TransactionProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Slf4j
@AllArgsConstructor
public class TransactionHandler {

    private final SecurityService securityService;
    private final TransactionProcessor transactionProcessor;
    private final AccountService accountService;
    private final BalanceManager balanceManager;

    public TransactionResponse processTransaction(TransactionRequest request,
                                                   TransactionType type,
                                                   BalanceOperation operation) {
        String currentUserId = securityService.getCurrentUserId();
        accountService.validateAccountAndOwnership(request.getAccountNumber(), currentUserId);

        Transaction transaction = transactionProcessor.createAndSaveTransaction(request, type);


        BigDecimal newBalance = operation.processBalanceChange();
        TransactionResponse response = transactionProcessor.completeTransaction(transaction, newBalance);

        //Sync after transaction commits
        transactionProcessor.syncBalanceToAccountService(request.getAccountNumber(), newBalance);

        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse processTransferTransaction(TransferRequest request) {
        Transaction transaction = transactionProcessor.createAndSaveTransaction(request, TransactionType.TRANSFER);

        balanceManager.validateSufficientFunds(request.getFromAccountNumber(), request.getAmount());
        balanceManager.updateAccountBalance(request.getFromAccountNumber(), request.getAmount().negate());
        balanceManager.updateAccountBalance(request.getToAccountNumber(), request.getAmount());

        BigDecimal fromBalance = balanceManager.getBalance(request.getFromAccountNumber());
        BigDecimal toBalance = balanceManager.getBalance(request.getToAccountNumber());

        TransactionResponse response = transactionProcessor.completeTransaction(transaction, fromBalance);

        // Sync both accounts after transaction commits
        transactionProcessor.syncBalanceToAccountService(request.getFromAccountNumber(), fromBalance);
        transactionProcessor.syncBalanceToAccountService(request.getToAccountNumber(), toBalance);

        return response;
    }

    @FunctionalInterface
    public interface BalanceOperation {
        BigDecimal processBalanceChange() throws InsufficientFundsException, InvalidAccountException, TransactionProcessingException;
    }
}
