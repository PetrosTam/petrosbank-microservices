package com.backendev.transactionservice.unit.controller;

import com.backendev.transactionservice.controller.TransactionController;
import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.exception.GlobalExceptionHandler;
import com.backendev.transactionservice.exception.InsufficientFundsException;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void should_deposit_money_successfully() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(123456L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setDescription("Deposit");

        TransactionResponse response = new TransactionResponse(
                "TXN-001",
                null,
                123456L,
                new BigDecimal("1000.00"),
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                "Deposit",
                Instant.now(),
                new BigDecimal("5000.00")
        );

        when(transactionService.deposit(any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                .andExpect(jsonPath("$.toAccountNumber").value(123456L))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accountBalance").value(5000.00));

        verify(transactionService).deposit(any(TransactionRequest.class));
    }

    @Test
    void should_withdraw_money_successfully() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(123456L);
        request.setAmount(new BigDecimal("500.00"));
        request.setDescription("Withdrawal");

        TransactionResponse response = new TransactionResponse(
                "TXN-002",
                123456L,
                null,
                new BigDecimal("500.00"),
                TransactionType.WITHDRAWAL,
                TransactionStatus.COMPLETED,
                "Withdrawal",
                Instant.now(),
                new BigDecimal("4500.00")
        );

        when(transactionService.withdraw(any(TransactionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-002"))
                .andExpect(jsonPath("$.fromAccountNumber").value(123456L))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accountBalance").value(4500.00));

        verify(transactionService).withdraw(any(TransactionRequest.class));
    }

    @Test
    void should_transfer_money_successfully() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber(123456L);
        request.setToAccountNumber(789012L);
        request.setAmount(new BigDecimal("2000.00"));
        request.setDescription("Transfer to friend");

        TransactionResponse response = new TransactionResponse(
                "TXN-003",
                123456L,
                789012L,
                new BigDecimal("2000.00"),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                "Transfer to friend",
                Instant.now(),
                new BigDecimal("2500.00")
        );

        when(transactionService.transfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-003"))
                .andExpect(jsonPath("$.fromAccountNumber").value(123456L))
                .andExpect(jsonPath("$.toAccountNumber").value(789012L))
                .andExpect(jsonPath("$.amount").value(2000.00))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(transactionService).transfer(any(TransferRequest.class));
    }

    @Test
    void should_fetch_transactions_by_account() throws Exception {
        Long accountNumber = 123456L;
        List<TransactionInfo> transactions = List.of(
                new TransactionInfo(
                        "TXN-001",
                        null,
                        123456L,
                        new BigDecimal("1000.00"),
                        TransactionType.DEPOSIT,
                        TransactionStatus.COMPLETED,
                        "Deposit",
                        "2024-01-01T10:00:00Z",
                        "2024-01-01T10:00:00Z"
                ),
                new TransactionInfo(
                        "TXN-002",
                        123456L,
                        null,
                        new BigDecimal("500.00"),
                        TransactionType.WITHDRAWAL,
                        TransactionStatus.COMPLETED,
                        "Withdrawal",
                        "2024-01-02T10:00:00Z",
                        "2024-01-02T10:00:00Z"
                )
        );

        when(transactionService.fetchTransactionsByAccount(accountNumber)).thenReturn(transactions);

        mockMvc.perform(get("/api/v1/transactions/account/{accountNumber}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value("TXN-001"))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[1].transactionId").value("TXN-002"))
                .andExpect(jsonPath("$[1].type").value("WITHDRAWAL"));

        verify(transactionService).fetchTransactionsByAccount(accountNumber);
    }

    @Test
    void should_fetch_account_balance() throws Exception {
        Long accountNumber = 123456L;
        AccountBalanceInfo balanceInfo = new AccountBalanceInfo(
                accountNumber,
                new BigDecimal("5000.00"),
                "2024-01-01T10:00:00Z"
        );

        when(transactionService.fetchAccountBalance(accountNumber)).thenReturn(balanceInfo);

        mockMvc.perform(get("/api/v1/transactions/balance/{accountNumber}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(123456L))
                .andExpect(jsonPath("$.balance").value(5000.00))
                .andExpect(jsonPath("$.lastUpdated").value("2024-01-01T10:00:00Z"));

        verify(transactionService).fetchAccountBalance(accountNumber);
    }

    @Test
    void should_return_empty_list_when_no_transactions_found() throws Exception {
        Long accountNumber = 999999L;

        when(transactionService.fetchTransactionsByAccount(accountNumber)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/transactions/account/{accountNumber}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionService).fetchTransactionsByAccount(accountNumber);
    }

    @Test
    void should_return_conflict_when_insufficient_funds() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(123456L);
        request.setAmount(new BigDecimal("10000.00"));
        request.setDescription("Withdrawal");

        when(transactionService.withdraw(any(TransactionRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient balance for withdrawal"));

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMessage").value("Insufficient balance for withdrawal"))
                .andExpect(jsonPath("$.errorDetails").value("Insufficient Balance."));

        verify(transactionService).withdraw(any(TransactionRequest.class));
    }

    @Test
    void should_return_not_found_when_invalid_account() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(999999L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setDescription("Deposit");

        when(transactionService.deposit(any(TransactionRequest.class)))
                .thenThrow(new InvalidAccountException("Account not found"));

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("Account not found"))
                .andExpect(jsonPath("$.errorDetails").value("Account not found. Invalid Account."));

        verify(transactionService).deposit(any(TransactionRequest.class));
    }

    @Test
    void should_return_internal_server_error_for_unexpected_exception() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setAccountNumber(123456L);
        request.setAmount(new BigDecimal("1000.00"));
        request.setDescription("Deposit");

        when(transactionService.deposit(any(TransactionRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("Database connection failed"))
                .andExpect(jsonPath("$.errorDetails").value("An unexpected error occurred."));

        verify(transactionService).deposit(any(TransactionRequest.class));
    }

    @Test
    void should_return_bad_request_for_invalid_deposit_request() throws Exception {
        TransactionRequest request = new TransactionRequest();

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).deposit(any(TransactionRequest.class));
    }

    @Test
    void should_return_bad_request_for_invalid_transfer_request() throws Exception {
        TransferRequest request = new TransferRequest();

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transfer(any(TransferRequest.class));
    }
}