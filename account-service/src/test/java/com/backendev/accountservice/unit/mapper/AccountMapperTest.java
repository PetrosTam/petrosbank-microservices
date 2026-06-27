package com.backendev.accountservice.unit.mapper;

import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.entity.Account;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.mapper.AccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {
    private AccountMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AccountMapper.class);
    }

    @Test
    void testToEntity() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountName("Savings Account");
        request.setAccountType(AccountType.SAVINGS);

        Account account = mapper.toEntity(request);

        assertThat(account).isNotNull();
        assertThat(account.getId()).isNull(); 
        assertThat(account.getAccountNumber()).isNull(); 
        assertThat(account.getUserId()).isNull(); 
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getAccountName()).isEqualTo("Savings Account");
        assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getCreatedAt()).isNull(); 
        assertThat(account.getUpdatedAt()).isNull(); 
    }

    @Test
    void testCreateNewAccount() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountName("Checking Account");
        request.setAccountType(AccountType.CHECKING);

        String userId = "user-123";
        Long accountNumber = 99999L;

        Account account = mapper.toEntity(request);
        account.setUserId(userId);
        account.setAccountNumber(accountNumber);

        assertThat(account).isNotNull();
        assertThat(account.getUserId()).isEqualTo(userId);
        assertThat(account.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(account.getAccountName()).isEqualTo("Checking Account");
        assertThat(account.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void testToAccountDto() {
        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber(12345L);
        account.setAccountName("Test Account");
        account.setType(AccountType.SAVINGS);
        account.setBalance(BigDecimal.TEN);

        AccountDto accountDto = mapper.toAccountDto(account);

        assertThat(accountDto).isNotNull();
        assertThat(accountDto.getAccountNumber()).isEqualTo(12345L);
        assertThat(accountDto.getAccountName()).isEqualTo("Test Account");
        assertThat(accountDto.getType()).isEqualTo(AccountType.SAVINGS);
    }

    @Test
    void testToAccountDetailsDto() {
        Account account = new Account();
        account.setId(2L);
        account.setAccountNumber(22222L);
        account.setAccountName("Details Account");
        account.setType(AccountType.SAVINGS);
        account.setBalance(BigDecimal.ONE);

        AccountDetailsDto accountDetailsDto = mapper.toAccountDetailsDto(account);

        assertThat(accountDetailsDto).isNotNull();
        assertThat(accountDetailsDto.getAccountNumber()).isEqualTo(22222L);
        assertThat(accountDetailsDto.getAccountName()).isEqualTo("Details Account");
        assertThat(accountDetailsDto.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(accountDetailsDto.getBalance()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void testListToAccountDetailsDto() {
        Account fromAccount = new Account();
        fromAccount.setAccountNumber(111L);
        fromAccount.setAccountName("Acc1");
        fromAccount.setType(AccountType.SAVINGS);
        fromAccount.setBalance(BigDecimal.valueOf(100));

        Account toAccount = new Account();
        toAccount.setAccountNumber(222L);
        toAccount.setAccountName("Acc2");
        toAccount.setType(AccountType.SAVINGS);
        toAccount.setBalance(BigDecimal.valueOf(200));

        List<AccountDetailsDto> dtos = mapper.toAccountDetailsDto(List.of(fromAccount, toAccount));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getAccountName()).isEqualTo("Acc1");
        assertThat(dtos.get(1).getAccountName()).isEqualTo("Acc2");
    }

}