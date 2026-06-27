package com.backendev.transactionservice.integration.repository;

import com.backendev.transactionservice.entity.Transaction;
import com.backendev.transactionservice.enums.TransactionStatus;
import com.backendev.transactionservice.enums.TransactionType;
import com.backendev.transactionservice.repository.TransactionRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TransactionRepositoryIT {

    @Autowired
    private TransactionRepository repository;

    private static final Long ACC_NUM_1 = 1234567890L;
    private static final Long ACC_NUM_2 = 9876543210L;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testSaveDeposit() {
        Transaction txn = new Transaction("TXN-001", ACC_NUM_1, null, BigDecimal.valueOf(500),
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "Deposit salary", Instant.now(), null);
        repository.save(txn);

        Optional<Transaction> found = repository.findById("TXN-001");
        assertTrue(found.isPresent());
        assertEquals(TransactionType.DEPOSIT, found.get().getType());
    }

    @Test
    void testSaveTransfer() {
        Transaction txn = new Transaction("TXN-002", ACC_NUM_1, ACC_NUM_2, BigDecimal.valueOf(1000),
                TransactionType.TRANSFER, TransactionStatus.SUCCESS, "Transfer", Instant.now(), null);
        repository.save(txn);

        Transaction found = repository.findById("TXN-002").orElseThrow();
        assertEquals(ACC_NUM_2, found.getToAccountNumber());
    }

    @Test
    void testFindByAccountNumberOrderByCreatedDesc() {
        repository.save(new Transaction("TXN-001", ACC_NUM_1, null, BigDecimal.valueOf(100),
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "T1", Instant.now().minusSeconds(20), null));
        repository.save(new Transaction("TXN-002", ACC_NUM_1, null, BigDecimal.valueOf(200),
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "T2", Instant.now(), null));

        List<Transaction> txns = repository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACC_NUM_1);
        assertEquals(2, txns.size());
        assertEquals("TXN-002", txns.get(0).getTransactionId()); // Most recent first
    }

    @Test
    void testFindByAccountNumberEmpty() {
        List<Transaction> txns = repository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACC_NUM_1);
        assertTrue(txns.isEmpty());
    }

    @Test
    void testUpdateTransactionStatus() {
        repository.save(new Transaction("TXN-001", ACC_NUM_1, null, BigDecimal.valueOf(500),
                TransactionType.DEPOSIT, TransactionStatus.PENDING, "Test", Instant.now(), null));

        Transaction txn = repository.findById("TXN-001").orElseThrow();
        txn.setStatus(TransactionStatus.COMPLETED);
        repository.save(txn);

        assertEquals(TransactionStatus.COMPLETED, repository.findById("TXN-001").orElseThrow().getStatus());
    }

    @Test
    void testDeleteTransaction() {
        repository.save(new Transaction("TXN-001", ACC_NUM_1, null, BigDecimal.valueOf(500),
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "Test", Instant.now(), null));
        repository.deleteById("TXN-001");

        assertTrue(repository.findById("TXN-001").isEmpty());
    }

    @Test
    void testMultipleAccountsTransactions() {
        repository.save(new Transaction("TXN-001", ACC_NUM_1, null, BigDecimal.valueOf(100),
                TransactionType.DEPOSIT, TransactionStatus.SUCCESS, "T1", Instant.now(), null));
        repository.save(new Transaction("TXN-002", ACC_NUM_2, null, BigDecimal.valueOf(200),
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCESS, "T2", Instant.now(), null));

        List<Transaction> acc1 = repository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACC_NUM_1);
        List<Transaction> acc2 = repository.findAllByFromAccountNumberOrderByCreatedAtDesc(ACC_NUM_2);

        assertEquals(1, acc1.size());
        assertEquals(1, acc2.size());
    }
}
