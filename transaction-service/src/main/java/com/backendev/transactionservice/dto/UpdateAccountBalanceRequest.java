package com.backendev.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UpdateAccountBalanceRequest {

    private Long accountNumber;
    private BigDecimal balance;
}
