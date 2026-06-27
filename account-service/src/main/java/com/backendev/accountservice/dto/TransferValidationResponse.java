package com.backendev.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferValidationResponse {

    AccountValidationDto fromAccount;
    AccountValidationDto toAccount;
}
