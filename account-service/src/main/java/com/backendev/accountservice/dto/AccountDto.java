package com.backendev.accountservice.dto;

import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    private Long accountNumber;
    private String userId;
    private String email;
    private AccountType type;
    private AccountStatus status;
    private String accountName;
}
