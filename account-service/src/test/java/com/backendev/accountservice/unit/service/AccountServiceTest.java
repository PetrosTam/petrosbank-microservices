package com.backendev.accountservice.unit.service;

import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountResponse;
import com.backendev.accountservice.dto.AccountValidationDto;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.NotificationEvent;
import com.backendev.accountservice.dto.TransferValidationResponse;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.entity.Account;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.exception.AccountLimitExceededException;
import com.backendev.accountservice.exception.AccountNotFoundException;
import com.backendev.accountservice.exception.InactiveAccountException;
import com.backendev.accountservice.mapper.AccountMapper;
import com.backendev.accountservice.messaging.AccountEventPublisher;
import com.backendev.accountservice.repository.AccountRepository;
import com.backendev.accountservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountEventPublisher accountEventPublisher;

    @InjectMocks
    private AccountService accountService;

    private CreateAccountRequest createAccountRequest;
    private Account account;
    private AccountDto accountDto;
    private AccountValidationDto fromAccountDto;
    private AccountValidationDto toAccountDto;

    private final String userId = "user123";
    private final String email = "user@exmaple.com";
    private final Long accountNumber = 1234567890L;
    private Account fromAccount;
    private Account toAccount;
    private static final Long FROM_ACCOUNT_NUMBER = 12345L;
    private static final Long TO_ACCOUNT_NUMBER = 67890L;

    @BeforeEach
    void setUp() {
        createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setAccountType(AccountType.SAVINGS);

        account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUserId(userId);
        account.setBalance(BigDecimal.valueOf(1000));

        accountDto = new AccountDto();
        accountDto.setAccountNumber(accountNumber);

        fromAccount = new Account();
        fromAccount.setAccountNumber(FROM_ACCOUNT_NUMBER);
        fromAccount.setUserId("user123");
        fromAccount.setStatus(AccountStatus.ACTIVE);
        fromAccount.setType(AccountType.CHECKING);

        toAccount = new Account();
        toAccount.setAccountNumber(TO_ACCOUNT_NUMBER);
        toAccount.setUserId("user456");
        toAccount.setStatus(AccountStatus.ACTIVE);
        toAccount.setType(AccountType.SAVINGS);

        fromAccountDto = new AccountValidationDto();
        fromAccountDto.setAccountNumber(FROM_ACCOUNT_NUMBER);
        fromAccountDto.setStatus(AccountStatus.ACTIVE);

        toAccountDto = new AccountValidationDto();
        toAccountDto.setAccountNumber(TO_ACCOUNT_NUMBER);
        toAccountDto.setStatus(AccountStatus.ACTIVE);
    }

    @Nested
    class CreateAccountTests {

        @Test
        void shouldCreateAccount_whenValidRequest() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(0L);
            when(accountRepository.existsByAccountNumber(anyLong())).thenReturn(false);
            when(accountMapper.toEntity(createAccountRequest)).thenReturn(account);
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.createAccount(userId, email, createAccountRequest);

            assertNotNull(result);
            assertEquals(accountNumber, result.getAccountNumber());
            verify(accountRepository).save(account);
        }

        @Test
        void shouldPublishEvent_whenAccountCreatedSuccessfully() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(0L);
            when(accountRepository.existsByAccountNumber(anyLong())).thenReturn(false);
            when(accountMapper.toEntity(createAccountRequest)).thenReturn(account);
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            accountService.createAccount(userId, email, createAccountRequest);

            verify(accountEventPublisher, times(1)).publishAccountEvent(any(NotificationEvent.class));
        }

        @Test
        void shouldPublishEventWithCorrectDetails_whenAccountCreated() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(0L);
            when(accountRepository.existsByAccountNumber(anyLong())).thenReturn(false);
            when(accountMapper.toEntity(createAccountRequest)).thenReturn(account);
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            accountService.createAccount(userId, email, createAccountRequest);

            verify(accountEventPublisher).publishAccountEvent(argThat(event ->
                    "Account created".equals(event.getEventType()) &&
                            userId.equals(event.getUserId())
            ));
        }


        @Test
        void shouldThrowException_whenAccountLimitExceeded() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(5L);

            assertThrows(AccountLimitExceededException.class,
                    () -> accountService.createAccount(userId, email, createAccountRequest));
        }

        @Test
        void shouldNotPublishEvent_whenAccountLimitExceeded() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(5L);

            assertThrows(AccountLimitExceededException.class,
                    () -> accountService.createAccount(userId, email, createAccountRequest));

            verify(accountEventPublisher, never()).publishAccountEvent(any());
        }

        @Test
        void shouldGenerateUniqueAccountNumber_whenDuplicateExists() {
            when(accountRepository.countByUserIdAndType(userId, AccountType.SAVINGS)).thenReturn(0L);
            when(accountRepository.existsByAccountNumber(anyLong()))
                    .thenReturn(true)  // First call - duplicate exists
                    .thenReturn(false); // Second call - unique number
            when(accountMapper.toEntity(createAccountRequest)).thenReturn(account);
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.createAccount(userId, email, createAccountRequest);

            assertNotNull(result);
            verify(accountRepository, atLeast(2)).existsByAccountNumber(anyLong());
        }
    }

    @Nested
    class FetchAccountDetailsTests {

        @Test
        void shouldFetchAccountDetails_whenAccountExists() {
            AccountDetailsDto expectedDto = new AccountDetailsDto();
            when(accountRepository.findByAccountNumberAndUserId(accountNumber, userId)).thenReturn(Optional.of(account));
            when(accountMapper.toAccountDetailsDto(account)).thenReturn(expectedDto);

            AccountDetailsDto result = accountService.fetchAccountDetails(accountNumber, userId);

            assertNotNull(result);
            assertEquals(expectedDto, result);
            verify(accountEventPublisher, never()).publishAccountEvent(any());
        }

        @Test
        void shouldThrowException_whenAccountNotFound() {
            when(accountRepository.findByAccountNumberAndUserId(accountNumber, userId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.fetchAccountDetails(accountNumber, userId));
        }
    }

    @Nested
    class FetchAccountsForUserTests {

        @Test
        void shouldFetchUserAccounts_whenAccountsExist() {
            List<Account> accounts = List.of(account);
            List<AccountDetailsDto> expectedDtos = List.of(new AccountDetailsDto());

            when(accountRepository.findByUserId(userId)).thenReturn(accounts);
            when(accountMapper.toAccountDetailsDto(accounts)).thenReturn(expectedDtos);

            List<AccountDetailsDto> result = accountService.fetchAccountsForUser(userId);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(expectedDtos, result);

            verify(accountEventPublisher, never()).publishAccountEvent(any());
        }
    }

    @Nested
    class DeleteAccountTests {

        @Test
        void shouldDeleteAccount_whenAccountExists() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

            AccountResponse result = accountService.deleteAccount(accountNumber);

            assertNotNull(result);
            assertEquals("DELETED", result.getStatus());
            assertEquals("Account deleted successfully", result.getMessage());
            verify(accountRepository).delete(account);
        }

        @Test
        void shouldPublishEvent_whenAccountDeletedSuccessfully() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

            accountService.deleteAccount(accountNumber);

            verify(accountEventPublisher, times(1)).publishAccountEvent(any(NotificationEvent.class));
        }

        @Test
        void shouldPublishEventWithCorrectDetails_whenAccountDeleted() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

            accountService.deleteAccount(accountNumber);

            verify(accountEventPublisher).publishAccountEvent(argThat(event ->
                    "ACCOUNT DELETED".equals(event.getEventType()) &&
                            "Account Deleted".equals(event.getSubject())
            ));
        }

        @Test
        void shouldNotPublishEvent_whenAccountNotFoundForDeletion() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.deleteAccount(accountNumber));

            verify(accountEventPublisher, never()).publishAccountEvent(any());
        }

        @Test
        void shouldThrowException_whenAccountNotFoundForDeletion() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () -> accountService.deleteAccount(accountNumber));
        }
    }

    @Nested
    class UpdateAccountBalanceTests {

        @Test
        void shouldUpdateBalance_whenAccountExists() {
            UpdateAccountBalanceRequest request = new UpdateAccountBalanceRequest();
            request.setAccountNumber(accountNumber);
            request.setBalance(BigDecimal.valueOf(2000));

            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.updateAccountBalance(request);

            assertNotNull(result);
            verify(accountRepository).save(account);
            assertEquals(BigDecimal.valueOf(2000), account.getBalance());
            assertNotNull(account.getUpdatedAt());

            verify(accountEventPublisher, never()).publishAccountEvent(any());
        }

        @Test
        void shouldThrowException_whenAccountNotFoundForUpdate() {
            UpdateAccountBalanceRequest request = new UpdateAccountBalanceRequest();
            request.setAccountNumber(accountNumber);

            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () -> accountService.updateAccountBalance(request));
        }
    }

    @Nested
    class MarkAccountFrozenTests {

        @Test
        void shouldFreezeAccount_whenAccountExists() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenReturn(account);
            when(accountMapper.toAccountDto(account)).thenReturn(accountDto);

            AccountDto result = accountService.markAccountFrozen(accountNumber);

            assertNotNull(result);
            verify(accountRepository).save(account);
            assertEquals(AccountStatus.FROZEN, account.getStatus());
        }

        @Test
        void shouldThrowException_whenAccountNotFoundForFreezing() {
            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());
            assertThrows(AccountNotFoundException.class, () -> accountService.markAccountFrozen(accountNumber));
        }
    }

    @Nested
    class DoesUserOwnAccountTests {

        @Test
        void shouldReturnTrue_whenUserOwnsAccount() {
            when(accountRepository.existsByAccountNumberAndUserId(accountNumber, userId)).thenReturn(true);

            boolean result = accountService.doesUserOwnAccount(userId, accountNumber);

            assertTrue(result);
        }

        @Test
        void shouldReturnFalse_whenUserDoesNotOwnAccount() {
            when(accountRepository.existsByAccountNumberAndUserId(accountNumber, userId)).thenReturn(false);

            boolean result = accountService.doesUserOwnAccount(userId, accountNumber);

            assertFalse(result);
        }
    }

    @Nested
    class ValidateTransferBetweenAccounts {

        @Test
        void validateTransferAccounts_BothAccountsActive_Success() {
            when(accountRepository.findByAccountNumber(FROM_ACCOUNT_NUMBER)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber(TO_ACCOUNT_NUMBER)).thenReturn(Optional.of(toAccount));
            when(accountMapper.toAccountValidationDto(fromAccount)).thenReturn(fromAccountDto);
            when(accountMapper.toAccountValidationDto(toAccount)).thenReturn(toAccountDto);

            TransferValidationResponse result = accountService.validateTransferAccounts(FROM_ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER);

            assertNotNull(result);
            assertNotNull(result.getFromAccount());
            assertNotNull(result.getToAccount());
            assertEquals(FROM_ACCOUNT_NUMBER, result.getFromAccount().getAccountNumber());
            assertEquals(TO_ACCOUNT_NUMBER, result.getToAccount().getAccountNumber());
            verify(accountRepository).findByAccountNumber(FROM_ACCOUNT_NUMBER);
            verify(accountRepository).findByAccountNumber(TO_ACCOUNT_NUMBER);
            verify(accountMapper).toAccountValidationDto(fromAccount);
            verify(accountMapper).toAccountValidationDto(toAccount);
        }

        @Test
        void validateTransferAccounts_FromAccountInactive_ThrowsException() {
            fromAccount.setStatus(AccountStatus.FROZEN);

            when(accountRepository.findByAccountNumber(FROM_ACCOUNT_NUMBER)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber(TO_ACCOUNT_NUMBER)).thenReturn(Optional.of(toAccount));

            InactiveAccountException exception = assertThrows(InactiveAccountException.class,
                    () -> accountService.validateTransferAccounts(FROM_ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER));

            assertTrue(exception.getMessage().contains("inactive"));
            assertTrue(exception.getMessage().contains(FROM_ACCOUNT_NUMBER.toString()));
            verify(accountRepository).findByAccountNumber(FROM_ACCOUNT_NUMBER);
            verify(accountRepository).findByAccountNumber(TO_ACCOUNT_NUMBER);
            verify(accountMapper, never()).toAccountValidationDto(any());
        }

        @Test
        void validateTransferAccounts_BothAccountsInactive_ThrowsExceptionForFromAccount() {
            fromAccount.setStatus(AccountStatus.FROZEN);
            toAccount.setStatus(AccountStatus.FROZEN);

            when(accountRepository.findByAccountNumber(FROM_ACCOUNT_NUMBER)).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findByAccountNumber(TO_ACCOUNT_NUMBER)).thenReturn(Optional.of(toAccount));

            InactiveAccountException exception = assertThrows(InactiveAccountException.class,
                    () -> accountService.validateTransferAccounts(FROM_ACCOUNT_NUMBER, TO_ACCOUNT_NUMBER));

            assertTrue(exception.getMessage().contains("inactive"));
            verify(accountRepository).findByAccountNumber(FROM_ACCOUNT_NUMBER);
            verify(accountRepository).findByAccountNumber(TO_ACCOUNT_NUMBER);
        }
    }
}