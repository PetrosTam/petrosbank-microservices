package com.backendev.transactionservice.integration.controller;

import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.integration.config.TestFeignConfig;
import com.backendev.transactionservice.integration.config.TestSecurityConfig;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import com.backendev.transactionservice.repository.TransactionRepository;
import com.backendev.transactionservice.service.BalanceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, TestFeignConfig.class})
@ActiveProfiles("test")
class TransactionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BalanceManager balanceManager;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountBalanceRepository accountBalanceRepository;

    private static final Long ACCOUNT_NUMBER = 123456L;
    private static final Long TO_ACCOUNT_NUMBER = 654321L;
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final String DESCRIPTION = "Test transaction";

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountBalanceRepository.deleteAll();
    }

    @Nested
    class DepositMoney{

        @Test
        void depositMoney_WithValidRequest_ReturnsCreatedStatus() throws Exception {
           TransactionRequest request = new TransactionRequest(ACCOUNT_NUMBER, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("DEPOSIT"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.amount").value(100.00));
        }

        @Test
        void depositMoney_WithNullAccountNumber_ReturnsBadRequest() throws Exception {
           TransactionRequest request = new TransactionRequest(null, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void depositMoney_WithInvalidAmount_ReturnsBadRequest() throws Exception {
           TransactionRequest request = new TransactionRequest(ACCOUNT_NUMBER, new BigDecimal("0.00"), DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void depositMoney_WithNullDescription_ReturnsBadRequest() throws Exception {
           TransactionRequest request = new TransactionRequest(ACCOUNT_NUMBER, AMOUNT, null);

            mockMvc.perform(post("/api/v1/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class WithdrawMoney{

        @Test
        void withdrawMoney_WithValidRequest_ReturnsCreatedStatus() throws Exception {
            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, new BigDecimal("500.00"));
            TransactionRequest request = new TransactionRequest(ACCOUNT_NUMBER, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.accountBalance").value(400.00));
        }

        @Test
        void withdrawMoney_WithInsufficientFunds_Returns409Conflict() throws Exception {
            TransactionRequest request = new TransactionRequest(ACCOUNT_NUMBER, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
        }
    }

    @Nested
    class TransferMoney{

        @Test
        void transferMoney_WithValidRequest_ReturnsCreatedStatus() throws Exception {
            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, new BigDecimal("500.00"));
            TransferRequest request = new TransferRequest(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("TRANSFER"))
                    .andExpect(jsonPath("$.fromAccountNumber").value(ACCOUNT_NUMBER))
                    .andExpect(jsonPath("$.toAccountNumber").value(TO_ACCOUNT_NUMBER))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void transferMoney_WithNullToAccount_ReturnsBadRequest() throws Exception {
           TransferRequest request = new TransferRequest(ACCOUNT_NUMBER, null, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void transferMoney_WithNullFromAccount_ReturnsBadRequest() throws Exception {
           TransferRequest request = new TransferRequest(null, TO_ACCOUNT_NUMBER, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void transferMoney_WithInsufficientFunds_Returns409Conflict() throws Exception {
            // Arrange - No balance available
            TransferRequest request = new TransferRequest(ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER, AMOUNT, DESCRIPTION);

            mockMvc.perform(post("/api/v1/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }
    

    @Nested
    class FetchTransactions{

        @Test
        void fetchTransactionsByAccount_WithValidAccount_ReturnsOkStatus() throws Exception {
            // Arrange - Create a transaction
            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, AMOUNT);

            TransactionRequest request = new TransactionRequest(ACCOUNT_NUMBER, AMOUNT, DESCRIPTION);
            mockMvc.perform(post("/api/v1/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/transactions/account/{accountNumber}", ACCOUNT_NUMBER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$[0].type").value("DEPOSIT"));
        }

        @Test
        void fetchTransactionsByAccount_WithInvalidAccount_Returns404NotFound() throws Exception {
            mockMvc.perform(get("/api/v1/transactions/account/{accountNumber}", 999999L))
                    .andExpect(status().isNotFound());
        }
    }
    

    @Nested
    class FetchAccountBalance{

        @Test
        void fetchAccountBalance_WithValidAccount_ReturnsOkStatus() throws Exception {
            balanceManager.updateAccountBalance(ACCOUNT_NUMBER, new BigDecimal("5000.00"));

            mockMvc.perform(get("/api/v1/transactions/balance/{accountNumber}", ACCOUNT_NUMBER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                    .andExpect(jsonPath("$.balance").value(5000.00));
        }

        @Test
        void fetchAccountBalance_WithInvalidAccount_Returns404NotFound() throws Exception {
            mockMvc.perform(get("/api/v1/transactions/balance/{accountNumber}", 999999L))
                    .andExpect(status().isNotFound());
        }
    }
    
}
