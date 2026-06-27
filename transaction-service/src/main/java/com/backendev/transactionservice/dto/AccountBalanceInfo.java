package com.backendev.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountBalanceInfo {

    private Long accountNumber;
    private BigDecimal balance;
    private String lastUpdated;
}
