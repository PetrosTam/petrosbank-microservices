package com.backendev.transactionservice.controller;

import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> depositMoney(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.deposit(request);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdrawMoney(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.withdraw(request);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transferMoney(@Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(request);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionInfo>> fetchTransactionsByAccount(@PathVariable @NotNull Long accountNumber){
        List<TransactionInfo> transactionInfo = transactionService.fetchTransactionsByAccount(accountNumber);
        return new ResponseEntity<>(transactionInfo,HttpStatus.OK);
    }

    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<AccountBalanceInfo> fetchAccountBalance(@PathVariable @NotNull Long accountNumber){
        AccountBalanceInfo accountBalance = transactionService.fetchAccountBalance(accountNumber);
        return new ResponseEntity<>(accountBalance, HttpStatus.OK);
    }
}
