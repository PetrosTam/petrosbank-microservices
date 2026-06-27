package com.backendev.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferValidationResponse {

    AccountResponse fromAccountResponse;
    AccountResponse toAccountResponse;
}
