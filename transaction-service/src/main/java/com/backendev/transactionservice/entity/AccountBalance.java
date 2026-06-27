package com.backendev.transactionservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_balances")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountBalance {
    @Id
    private Long accountNumber;

    private BigDecimal balance;

    @UpdateTimestamp
    private Instant lastUpdated;

}
