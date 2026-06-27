package com.backendev.transactionservice.dto;

import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {

    private String transactionId;
    private Long fromAccountNumber;
    private Long toAccountNumber;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private Instant timestamp;
    private BigDecimal accountBalance;  // Balance after transaction for the user's account

}
