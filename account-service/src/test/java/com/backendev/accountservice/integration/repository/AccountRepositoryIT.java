package com.backendev.accountservice.integration.repository;

import com.backendev.accountservice.entity.Account;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AccountRepositoryIT {

    @Autowired
    private AccountRepository repository;

    private static final String USER_ID = "user-123";
    private static final String EMAIL_ID = "user@example.com";
    private static final Long ACC_NUM_1 = 1234567890L;
    private static final Long ACC_NUM_2 = 9876543210L;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testExistsByUserId() {
        Account account = new Account(null, ACC_NUM_1, USER_ID, EMAIL_ID, BigDecimal.valueOf(5000),
                "Savings Account", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(account);

        assertTrue(repository.existsByUserId(USER_ID));
        assertFalse(repository.existsByUserId("other-user"));
    }

    @Test
    void testExistsByAccountNumber() {
        Account account = new Account(null, ACC_NUM_1, USER_ID, EMAIL_ID, BigDecimal.valueOf(5000),
                "Savings Account", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(account);

        assertTrue(repository.existsByAccountNumber(ACC_NUM_1));
        assertFalse(repository.existsByAccountNumber(999999999L));
    }

    @Test
    void testFindByAccountNumberAndUserId() {
        Account account = new Account(null, ACC_NUM_1, USER_ID, EMAIL_ID, BigDecimal.valueOf(5000),
                "Savings Account", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(account);

        Optional<Account> found = repository.findByAccountNumberAndUserId(ACC_NUM_1, USER_ID);
        assertTrue(found.isPresent());
        assertEquals(USER_ID, found.get().getUserId());

        assertTrue(repository.findByAccountNumberAndUserId(ACC_NUM_1, "other-user").isEmpty());
    }

    @Test
    void testCountByUserIdAndType() {
        Account acc1 = new Account(null, ACC_NUM_1, USER_ID, EMAIL_ID, BigDecimal.valueOf(5000),
                "Savings Account 1", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(acc1);

        Account acc2 = new Account(null, ACC_NUM_2, USER_ID, EMAIL_ID, BigDecimal.valueOf(3000),
                "Savings Account 2", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(acc2);

        assertEquals(2, repository.countByUserIdAndType(USER_ID, AccountType.SAVINGS));
        assertEquals(0, repository.countByUserIdAndType(USER_ID, AccountType.CHECKING));
    }

    @Test
    void testFindByUserId() {
        Account acc1 = new Account(null, ACC_NUM_1, USER_ID, EMAIL_ID, BigDecimal.valueOf(5000),
                "Savings Account", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(acc1);

        Account acc2 = new Account(null, ACC_NUM_2, USER_ID, EMAIL_ID, BigDecimal.valueOf(3000),
                "Checking Account", AccountType.CHECKING, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(acc2);

        List<Account> accounts = repository.findByUserId(USER_ID);
        assertEquals(2, accounts.size());
        assertTrue(accounts.stream().allMatch(a -> a.getUserId().equals(USER_ID)));

        assertTrue(repository.findByUserId("other-user").isEmpty());
    }

    @Test
    void testFindByAccountNumber() {
        Account account = new Account(null, ACC_NUM_1, USER_ID, EMAIL_ID, BigDecimal.valueOf(5000),
                "Savings Account", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(account);

        Optional<Account> found = repository.findByAccountNumber(ACC_NUM_1);
        assertTrue(found.isPresent());
        assertEquals(ACC_NUM_1, found.get().getAccountNumber());

        assertTrue(repository.findByAccountNumber(999999999L).isEmpty());
    }

    @Test
    void testExistsByAccountNumberAndUserId() {
        Account account = new Account(null, ACC_NUM_1, USER_ID, EMAIL_ID, BigDecimal.valueOf(5000),
                "Savings Account", AccountType.SAVINGS, AccountStatus.ACTIVE, Instant.now(), null);
        repository.save(account);

        assertTrue(repository.existsByAccountNumberAndUserId(ACC_NUM_1, USER_ID));
        assertFalse(repository.existsByAccountNumberAndUserId(ACC_NUM_1, "other-user"));
        assertFalse(repository.existsByAccountNumberAndUserId(999999999L, USER_ID));
    }
}
