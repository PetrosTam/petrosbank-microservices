package com.backendev.transactionservice.service;

import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.dto.NotificationEvent;
import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.mapper.AccountBalanceMapper;
import com.backendev.transactionservice.mapper.TransactionMapper;
import com.backendev.transactionservice.messaging.TransactionEventPublisher;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class TransactionService {

    private final AccountService accountService;
    private final BalanceManager balanceManager;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountBalanceMapper accountBalanceMapper;
    private final TransactionHandler transactionHandler;
    private final TransactionEventPublisher eventPublisher;
    private final SecurityService securityService;

    public TransactionResponse deposit(@Valid TransactionRequest request) {
        String userId = securityService.getCurrentUserId();
        String userEmail = securityService.getCurrentUserEmail();
        TransactionResponse transactionResponse = transactionHandler.processTransaction(request, TransactionType.DEPOSIT,
                () -> {
                    balanceManager.updateAccountBalance(request.getAccountNumber(), request.getAmount());
                    return balanceManager.getBalance(request.getAccountNumber());
                });
        publishNotificationEvent("TRANSACTION_DEPOSITED", transactionResponse, userId, userEmail, "Transaction Alert - Deposit", "Your account has been debited with $");
        return transactionResponse;
    }

    public TransactionResponse withdraw(TransactionRequest request) {
        String userId = securityService.getCurrentUserId();
        String userEmail = securityService.getCurrentUserEmail();
        TransactionResponse transactionResponse = transactionHandler.processTransaction(request, TransactionType.WITHDRAWAL, () -> {
            balanceManager.validateSufficientFunds(request.getAccountNumber(), request.getAmount());
            balanceManager.updateAccountBalance(request.getAccountNumber(), request.getAmount().negate());
            return balanceManager.getBalance(request.getAccountNumber());
        });
        publishNotificationEvent("TRANSACTION_WITHDRAWAL", transactionResponse, userId, userEmail, "Transaction Alert - Withdrawal", "Your account has been credited with $");
        return transactionResponse;
    }

    public TransactionResponse transfer(TransferRequest request) {
        String userId = securityService.getCurrentUserId();
        String userEmail = securityService.getCurrentUserEmail();
        accountService.validateTransferAccounts(request.getFromAccountNumber(), request.getToAccountNumber(), userId);
        TransactionResponse transactionResponse = transactionHandler.processTransferTransaction(request);
        publishNotificationEvent("TRANSACTION_TRANSFER", transactionResponse, userId, userEmail, "Transaction Alert - Transfer", "Your account has been credited with $");

        return transactionResponse;
    }

    @Transactional(readOnly = true)
    public List<TransactionInfo> fetchTransactionsByAccount(Long accountNumber) {
        log.debug("Fetching transactions for account: {}", accountNumber);

        accountBalanceRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new InvalidAccountException(
                        "Account number does not exist: " + accountNumber));

        List<Transaction> transactions =
                transactionRepository.findAllByFromAccountNumberOrToAccountNumberOrderByCreatedAtDesc(
                        accountNumber,
                        accountNumber
                );

        log.info("Found {} transactions for account: {}", transactions.size(), accountNumber);
        return transactionMapper.toTransactionInfoList(transactions);
    }

    @Transactional(readOnly = true)
    public AccountBalanceInfo fetchAccountBalance(Long accountNumber) {
        log.debug("Fetching account balance for account: {}", accountNumber);

        return accountBalanceRepository.findByAccountNumber(accountNumber)
                .map(accountBalanceMapper::toAccountBalanceInfo)
                .orElseThrow(() -> new InvalidAccountException(
                        "Account number does not exist: " + accountNumber));
    }

    private void publishNotificationEvent(String transactionType, TransactionResponse transactionResponse, String userId, String email, String subject, String message) {
        String accountMessage = transactionResponse.getAmount() + ". The account balance is $" + transactionResponse.getAccountBalance();
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(transactionType)
                .status(transactionResponse.getStatus().toString())
                .userId(userId)
                .email(email)
                .subject(subject)
                .message(message + accountMessage)
                .build();
        eventPublisher.publishTransactionEvent(event);
    }
}