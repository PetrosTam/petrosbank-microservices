package com.backendev.accountservice.repository;

import com.backendev.accountservice.entity.Account;
import com.backendev.accountservice.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByUserId(String userId);

    boolean existsByAccountNumber(Long accountNumber);

    Optional<Account> findByAccountNumberAndUserId(Long accountNumber, String userId);

    long countByUserIdAndType(String userId, AccountType accountType);

    List<Account> findByUserId(String userId);

    Optional<Account> findByAccountNumber(Long accountNumber);

    boolean existsByAccountNumberAndUserId(@NotNull Long accountNumber, String userId);
}
