package com.backendev.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {

    private Long accountNumber;
    private String userId;
    private String accountType;
    private String status;
    private Instant createdAt;
}
