package com.backendev.transactionservice.mapper;

import com.backendev.transactionservice.dto.TransactionInfo;
import com.backendev.transactionservice.dto.TransactionRequest;
import com.backendev.transactionservice.dto.TransactionResponse;
import com.backendev.transactionservice.dto.TransferRequest;
import com.backendev.transactionservice.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    // Enhanced mapping with user's account balance only
    @Mapping(target = "timestamp", source = "transaction.createdAt")
    @Mapping(target = "fromAccountNumber", source = "transaction.fromAccountNumber")
    @Mapping(target = "toAccountNumber", source = "transaction.toAccountNumber")
    TransactionResponse toResponseWithBalance(Transaction transaction, BigDecimal accountBalance);

    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "type", constant = "TRANSFER")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Transaction fromTransferRequest(TransferRequest request);

    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "fromAccountNumber", source = "accountNumber")
    @Mapping(target = "toAccountNumber",  source = "accountNumber")
    @Mapping(target = "type", constant = "DEPOSIT")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Transaction fromDepositRequest(TransactionRequest request);

    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "fromAccountNumber", source = "accountNumber")
    @Mapping(target = "toAccountNumber", source = "accountNumber")
    @Mapping(target = "type", constant = "WITHDRAWAL")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", ignore = true)
    Transaction fromWithdrawalRequest(TransactionRequest request);

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToString")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "instantToString")
    TransactionInfo toTransactionInfo(Transaction transactions);

    List<TransactionInfo> toTransactionInfoList(List<Transaction> transactions);

    @Named("instantToString")
    default String instantToString(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

}
