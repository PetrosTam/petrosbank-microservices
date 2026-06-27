package com.backendev.transactionservice.mapper;

import com.backendev.transactionservice.dto.AccountBalanceInfo;
import com.backendev.transactionservice.entity.AccountBalance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface AccountBalanceMapper {

    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "lastUpdated", expression = "java(java.time.Instant.now())")
    AccountBalance createAccountBalance(Long accountNumber, BigDecimal balance);

    @Mapping(source = "lastUpdated", target = "lastUpdated", qualifiedByName = "instantToString")
    AccountBalanceInfo toAccountBalanceInfo(AccountBalance accountBalance);

    @Named("instantToString")
    default String instantToString(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }
}
