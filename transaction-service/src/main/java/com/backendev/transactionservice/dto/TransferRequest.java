package com.backendev.transactionservice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

    @NotNull(message = "Account number is required")
    private Long fromAccountNumber;

    @NotNull(message = "Account number to which amount is to transferred is required")
    private Long toAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Please provide description")
    private String description;

    @AssertTrue(message = "From account and To account cannot be the same")
    private boolean isValidTransfer() {
        return fromAccountNumber == null || toAccountNumber == null ||
                !fromAccountNumber.equals(toAccountNumber);
    }
}
