package com.backendev.transactionservice.integration.repository;

import com.backendev.transactionservice.entity.AccountBalance;
import com.backendev.transactionservice.exception.InvalidAccountException;
import com.backendev.transactionservice.repository.AccountBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AccountBalanceRepositoryIT {

    @Autowired
    private AccountBalanceRepository repository;

    private static final Long ACC_NUM_1 = 1234567890L;
    private static final Long ACC_NUM_2 = 9876543210L;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testSaveAndFindById() {
        AccountBalance balance = new AccountBalance(ACC_NUM_1, BigDecimal.valueOf(5000), Instant.now());
        repository.save(balance);

        Optional<AccountBalance> accountBalance = repository.findById(ACC_NUM_1);
        assertTrue(accountBalance.isPresent());
        assertEquals(BigDecimal.valueOf(5000), accountBalance.orElseThrow().getBalance());
    }

    @Test
    void testFindByAccountNumber() {
        repository.save(new AccountBalance(ACC_NUM_1, BigDecimal.valueOf(3000), Instant.now()));

        Optional<AccountBalance> accountBalance = repository.findByAccountNumber(ACC_NUM_1);
        assertTrue(accountBalance.isPresent());
        assertEquals(BigDecimal.valueOf(3000), accountBalance.orElseThrow().getBalance());
    }

    @Test
    void testUpdateBalance() {
        repository.save(new AccountBalance(ACC_NUM_1, BigDecimal.valueOf(1000), Instant.now()));

        AccountBalance balance = repository.findById(ACC_NUM_1).orElseThrow(() -> new InvalidAccountException("Account not found "+ ACC_NUM_1));
        balance.setBalance(BigDecimal.valueOf(2000));
        repository.save(balance);

        assertEquals(BigDecimal.valueOf(2000), repository.findById(ACC_NUM_1).orElseThrow().getBalance());
    }

    @Test
    void testFindByIdNotFound() {
        assertTrue(repository.findById(ACC_NUM_1).isEmpty());
    }

    @Test
    void testDeleteById() {
        repository.save(new AccountBalance(ACC_NUM_1, BigDecimal.valueOf(5000), Instant.now()));
        repository.deleteById(ACC_NUM_1);

        assertTrue(repository.findById(ACC_NUM_1).isEmpty());
    }

    @Test
    void testMultipleAccounts() {
        repository.save(new AccountBalance(ACC_NUM_1, BigDecimal.valueOf(1000), Instant.now()));
        repository.save(new AccountBalance(ACC_NUM_2, BigDecimal.valueOf(5000), Instant.now()));

        assertEquals(2, repository.count());
        assertEquals(BigDecimal.valueOf(1000), repository.findById(ACC_NUM_1).orElseThrow().getBalance());
        assertEquals(BigDecimal.valueOf(5000), repository.findById(ACC_NUM_2).orElseThrow().getBalance());
    }
}
