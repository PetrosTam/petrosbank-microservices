package com.backendev.accountservice.dto;

import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDetailsDto {

    private Long accountNumber;
    private String userId;
    private AccountType type;
    private AccountStatus status;
    private String accountName;
    private BigDecimal balance;
    private Instant createdAt;
}
