package com.backendev.accountservice.dto;

import com.backendev.accountservice.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountValidationDto {

    private Long accountNumber;
    private AccountStatus status;

}
