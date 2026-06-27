package com.backendev.accountservice.service;

import com.backendev.accountservice.constants.AccountConstants;
import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountResponse;
import com.backendev.accountservice.dto.AccountValidationDto;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.NotificationEvent;
import com.backendev.accountservice.dto.TransferValidationResponse;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.entity.Account;
import com.backendev.accountservice.enums.AccountLimits;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.exception.AccountLimitExceededException;
import com.backendev.accountservice.exception.AccountNotFoundException;
import com.backendev.accountservice.exception.InactiveAccountException;
import com.backendev.accountservice.mapper.AccountMapper;
import com.backendev.accountservice.messaging.AccountEventPublisher;
import com.backendev.accountservice.repository.AccountRepository;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class AccountService {

    private static final Random RANDOM = new Random();

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountEventPublisher accountEventPublisher;


    public AccountDto createAccount(String userId, String email, CreateAccountRequest accountRequest) {
        AccountType accountType = accountRequest.getAccountType();

        validateAccountLimit(userId, accountType);

        Account account = accountMapper.toEntity(accountRequest);
        account.setUserId(userId);
        account.setEmail(email);
        account.setAccountNumber(generateRandomAccountNumber());

        Account saved = accountRepository.save(account);

        AccountDto accountDto = accountMapper.toAccountDto(saved);

        publishNotificationEvent("Account created","Account created successfully",userId, accountDto.getEmail(),
                accountDto.getAccountName()+" created",
                "Your account has been created. Your account number is: "+ accountDto.getAccountNumber());

        log.info("Account created for user with user Id {}", userId);
        return accountDto;
    }

    private void validateAccountLimit(String userId, AccountType accountType) {
        long currentAccountCount = accountRepository.countByUserIdAndType(userId, accountType);
        int maxAccountsAllowed = AccountLimits.getMaxAccountsForType(accountType);

        if (currentAccountCount >= maxAccountsAllowed) {
            throw new AccountLimitExceededException(
                    String.format("User has reached the limit for %s accounts (%d/%d)",
                            accountType, currentAccountCount, maxAccountsAllowed)
            );
        }
    }

    private Long generateRandomAccountNumber() {
        long base = 1000000000L;
        long accountNumber;
        do {
            accountNumber = base + RANDOM.nextInt(900_000_000);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    public AccountDetailsDto fetchAccountDetails(Long accountNumber, String userId) {
        Account account = accountRepository.findByAccountNumberAndUserId(accountNumber, userId)
                .orElseThrow(() -> new AccountNotFoundException(AccountConstants.ACCOUNT_NOT_FOUND + accountNumber +
                        " and user id: " + userId));
        return accountMapper.toAccountDetailsDto(account);
    }

    public List<AccountDetailsDto> fetchAccountsForUser(String userId) {
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        return accountMapper.toAccountDetailsDto(userAccounts);
    }


    public AccountResponse deleteAccount(Long accountNumber) {
        Account account = fetchAccountFromAccountNumber(accountNumber);
        accountRepository.delete(account);

        AccountResponse accountResponse = new AccountResponse("DELETED", "Account deleted successfully");

        publishNotificationEvent("ACCOUNT DELETED","Your account has been deleted successfully", account.getUserId(), account.getEmail(),
                "Account Deleted" ,
                "Your account with account number "+ accountNumber +" has been deleted.");

        return accountResponse;
    }

    // From Transaction service
    public AccountDto updateAccountBalance(UpdateAccountBalanceRequest updateRequest) {
        Account account = fetchAccountFromAccountNumber(updateRequest.getAccountNumber());
        account.setBalance(updateRequest.getBalance());
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        return accountMapper.toAccountDto(account);
    }

    public AccountDto markAccountFrozen(@NotNull Long accountNumber) {
        Account account = fetchAccountFromAccountNumber(accountNumber);
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);
        return accountMapper.toAccountDto(account);
    }

    public boolean doesUserOwnAccount(String userId, @NotNull Long accountNumber) {
        return accountRepository.existsByAccountNumberAndUserId(accountNumber, userId);
    }

    public TransferValidationResponse validateTransferAccounts(Long fromAccountNumber, Long toAccountNumber) {
        Account fromAccount = fetchAccountFromAccountNumber(fromAccountNumber);
        Account toAccount = fetchAccountFromAccountNumber(toAccountNumber);

        if(!fromAccount.getStatus().equals(AccountStatus.ACTIVE)){
            throw new InactiveAccountException(String.format("The source account %d is inactive", fromAccountNumber));
        }
        if(!toAccount.getStatus().equals(AccountStatus.ACTIVE)){
            throw new InactiveAccountException(String.format("The destination account %d is inactive", toAccountNumber));
        }

        AccountValidationDto fromAccountDto = accountMapper.toAccountValidationDto(fromAccount);
        AccountValidationDto toAccountDto = accountMapper.toAccountValidationDto(toAccount);

        return new TransferValidationResponse(fromAccountDto, toAccountDto);
    }

    private Account fetchAccountFromAccountNumber(Long fromAccountNumber) {
        return accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException(AccountConstants.ACCOUNT_NOT_FOUND + fromAccountNumber));
    }

    private void publishNotificationEvent(String eventType, String status, String userId, String email, String subject, String message){
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .status(status)
                .userId(userId)
                .email(email)
                .subject(subject)
                .message(message)
                .build();
        accountEventPublisher.publishAccountEvent(event);
    }
}
