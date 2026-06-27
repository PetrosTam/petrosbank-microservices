package com.backendev.accountservice.mapper;

import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountValidationDto;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "balance", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "accountName", source = "accountName")
    @Mapping(target = "type", source = "accountType")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(CreateAccountRequest request);

    AccountDto toAccountDto(Account account);

    AccountDetailsDto toAccountDetailsDto(Account account);

    List<AccountDetailsDto> toAccountDetailsDto(List<Account> account);

    AccountValidationDto toAccountValidationDto(Account fromAccount);
}
