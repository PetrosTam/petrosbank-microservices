package com.backendev.accountservice.enums;

import com.backendev.accountservice.exception.AccountAlreadyExistsException;
import lombok.Getter;

@Getter
public enum AccountLimits {
    SAVINGS(3),
    BUSINESS(1),
    CHECKING(1);

    private final int maxAllowed;

    AccountLimits(int maxAllowed) {
        this.maxAllowed = maxAllowed;
    }

    public static int getMaxAccountsForType(AccountType accountType){
        try {
            return AccountLimits.valueOf(accountType.name()).getMaxAllowed();
        } catch (IllegalArgumentException e) {
            throw new AccountAlreadyExistsException("Unsupported account type: " + accountType);
        }
    }
}
