package com.backendev.accountservice.integration.service;

import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountResponse;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.TransferValidationResponse;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.enums.AccountLimits;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.exception.AccountLimitExceededException;
import com.backendev.accountservice.exception.AccountNotFoundException;
import com.backendev.accountservice.exception.InactiveAccountException;
import com.backendev.accountservice.integration.config.TestSecurityConfig;
import com.backendev.accountservice.messaging.AccountEventPublisher;
import com.backendev.accountservice.repository.AccountRepository;
import com.backendev.accountservice.service.AccountService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AccountServiceIT {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private AccountEventPublisher accountEventPublisher;

    private static final String USER_ID = "user-123";
    private static final String EMAIL_ID = "user@example.com";


    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        entityManager.flush();
        doNothing().when(accountEventPublisher).publishAccountEvent(any());
    }

    @Nested
    class CreateAccount{

        @Test
        void testCreateAccount_Success() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("My Savings Account");

            AccountDto result = accountService.createAccount(USER_ID, EMAIL_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getAccountNumber()).isNotNull();
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getType()).isEqualTo(AccountType.SAVINGS);
            assertThat(result.getAccountName()).isEqualTo("My Savings Account");
            assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);

            var accounts = accountRepository.findAll();
            assertThat(accounts).hasSize(1);
        }

        @Test
        void testCreateAccount_shouldPublishKafkaEvent_onSuccess() {
            accountService.createAccount(USER_ID, EMAIL_ID, buildRequest(AccountType.SAVINGS, "My Savings Account"));

            verify(accountEventPublisher, times(1)).publishAccountEvent(argThat(event ->
                    "Account created".equals(event.getEventType()) &&
                            USER_ID.equals(event.getUserId())
            ));
        }

        @Test
        void testCreateAccount_GeneratesUniqueAccountNumber() {
            CreateAccountRequest request1 = new CreateAccountRequest();
            request1.setAccountType(AccountType.SAVINGS);
            request1.setAccountName("Account 1");

            CreateAccountRequest request2 = new CreateAccountRequest();
            request2.setAccountType(AccountType.CHECKING);
            request2.setAccountName("Account 2");

            AccountDto account1 = accountService.createAccount(USER_ID, EMAIL_ID, request1);
            AccountDto account2 = accountService.createAccount(USER_ID, EMAIL_ID, request2);

            assertThat(account1.getAccountNumber()).isNotEqualTo(account2.getAccountNumber());
        }

        @Test
        void testCreateAccount_WithoutAccountName() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.CHECKING);

            AccountDto result = accountService.createAccount(USER_ID, EMAIL_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(AccountType.CHECKING);
            assertThat(result.getAccountName()).isNull();
        }

        @Test
        void testCreateAccount_ExceedsLimit() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Savings 1");

            int maxLimit = AccountLimits.getMaxAccountsForType(AccountType.SAVINGS);

            for (int i = 0; i < maxLimit; i++) {
                accountService.createAccount(USER_ID, EMAIL_ID, request);
            }

            assertThatThrownBy(() -> accountService.createAccount(USER_ID, EMAIL_ID, request))
                    .isInstanceOf(AccountLimitExceededException.class)
                    .hasMessageContaining("reached the limit");
        }

        @Test
        void testCreateAccount_DifferentUsersCanHaveMultipleAccounts() {
            String user2 = "user-456";
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Savings");

            int maxLimit = AccountLimits.getMaxAccountsForType(AccountType.SAVINGS);

            for (int i = 0; i < maxLimit; i++) {
                accountService.createAccount(USER_ID, EMAIL_ID, request);
            }

            AccountDto user2Account = accountService.createAccount(user2, EMAIL_ID, request);

            assertThat(user2Account.getUserId()).isEqualTo(user2);
            var allAccounts = accountRepository.findAll();
            assertThat(allAccounts).hasSize(maxLimit + 1);
        }
    }

    @Nested
    class FetchAccountDetails{

        @Test
        void testFetchAccountDetails_Success() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("My Account");

            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, request);
            Long accountNumber = created.getAccountNumber();

            AccountDetailsDto result = accountService.fetchAccountDetails(accountNumber, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getAccountNumber()).isEqualTo(accountNumber);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getType()).isEqualTo(AccountType.SAVINGS);

            verify(accountEventPublisher, times(1)).publishAccountEvent(any());        }

        @Test
        void testFetchAccountDetails_AccountNotFound() {
            assertThatThrownBy(() -> accountService.fetchAccountDetails(99999L, USER_ID))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        void testFetchAccountDetails_WrongUserId() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, request);
            Long accountNumber = created.getAccountNumber();

            assertThatThrownBy(() -> accountService.fetchAccountDetails(accountNumber, "wrong-user"))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    class FetchAccountForUser{

        @Test
        void testFetchAccountsForUser_Success() {
            CreateAccountRequest request1 = new CreateAccountRequest();
            request1.setAccountType(AccountType.SAVINGS);
            request1.setAccountName("Savings");

            CreateAccountRequest request2 = new CreateAccountRequest();
            request2.setAccountType(AccountType.CHECKING);
            request2.setAccountName("Checking");

            accountService.createAccount(USER_ID, EMAIL_ID, request1);
            accountService.createAccount(USER_ID, EMAIL_ID, request2);

            List<AccountDetailsDto> result = accountService.fetchAccountsForUser(USER_ID);

            assertThat(result)
                    .hasSize(2)
                    .allMatch(acc -> acc.getUserId().equals(USER_ID));
        }

        @Test
        void testFetchAccountsForUser_Empty() {
            List<AccountDetailsDto> result = accountService.fetchAccountsForUser(USER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void testFetchAccountsForUser_OnlyUserAccounts() {
            String user2 = "user-456";
            String emailId2 = "user456@example.com";
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            accountService.createAccount(USER_ID, EMAIL_ID, request);
            accountService.createAccount(user2, emailId2, request);

            List<AccountDetailsDto> userAccounts = accountService.fetchAccountsForUser(USER_ID);

            assertThat(userAccounts)
                    .hasSize(1)
                    .allMatch(acc -> acc.getUserId().equals(USER_ID));
        }
    }

    @Nested
    class DeleteAccount{

        @Test
        void testDeleteAccount_Success() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account to Delete");

            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, request);
            Long accountNumber = created.getAccountNumber();

            AccountResponse result = accountService.deleteAccount(accountNumber);

            assertThat(result.getStatus()).isEqualTo("DELETED");
            assertThat(result.getMessage()).isEqualTo("Account deleted successfully");

            var accounts = accountRepository.findAll();
            assertThat(accounts).isEmpty();
        }

        @Test
        void testDeleteAccount_shouldPublishKafkaEvent_onSuccess() {
            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, buildRequest(AccountType.CHECKING, "Account"));

            accountService.deleteAccount(created.getAccountNumber());

            verify(accountEventPublisher, atLeastOnce()).publishAccountEvent(argThat(event ->
                    "Account Deleted".equals(event.getEventType())
            ));
        }

        @Test
        void testDeleteAccount_NotFound() {
            assertThatThrownBy(() -> accountService.deleteAccount(99999L))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    class UpdateAccountBalance{

        @Test
        void testUpdateAccountBalance_Success() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, request);

            UpdateAccountBalanceRequest updateRequest = new UpdateAccountBalanceRequest();
            updateRequest.setAccountNumber(created.getAccountNumber());
            updateRequest.setBalance(BigDecimal.valueOf(5000.00));

            AccountDto result = accountService.updateAccountBalance(updateRequest);

            assertThat(result.getAccountNumber()).isEqualTo(created.getAccountNumber());

            var accounts = accountRepository.findAll();
            assertThat(accounts.get(0).getBalance()).isEqualTo(BigDecimal.valueOf(5000.00));
        }

        @Test
        void testUpdateAccountBalance_AccountNotFound() {
            UpdateAccountBalanceRequest updateRequest = new UpdateAccountBalanceRequest();
            updateRequest.setAccountNumber(99999L);
            updateRequest.setBalance(BigDecimal.valueOf(1000));

            assertThatThrownBy(() -> accountService.updateAccountBalance(updateRequest))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    class MarkAccountFrozen{

        @Test
        void testMarkAccountFrozen_Success() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, request);
            Long accountNumber = created.getAccountNumber();

            AccountDto result = accountService.markAccountFrozen(accountNumber);

            assertThat(result.getStatus()).isEqualTo(AccountStatus.FROZEN);

            var accounts = accountRepository.findAll();
            assertThat(accounts.get(0).getStatus()).isEqualTo(AccountStatus.FROZEN);
        }

        @Test
        void testMarkAccountFrozen_AccountNotFound() {
            assertThatThrownBy(() -> accountService.markAccountFrozen(99999L))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }


    // ==================== DOES USER OWN ACCOUNT TESTS ====================

    @Nested
    class UserAccountOwnership{

        @Test
        void testDoesUserOwnAccount_True() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, request);
            Long accountNumber = created.getAccountNumber();

            boolean owns = accountService.doesUserOwnAccount(USER_ID, accountNumber);

            assertThat(owns).isTrue();
        }

        @Test
        void testDoesUserOwnAccount_False() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto created = accountService.createAccount(USER_ID, EMAIL_ID, request);
            Long accountNumber = created.getAccountNumber();

            boolean owns = accountService.doesUserOwnAccount("different-user", accountNumber);

            assertThat(owns).isFalse();
        }
    }


    // ==================== VALIDATE TRANSFER TESTS ====================

    @Nested
    class ValidateTransfer{

        @Test
        void testValidateTransferAccounts_Success() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("From Account");

            AccountDto fromAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);

            request.setAccountName("To Account");
            AccountDto toAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);

            TransferValidationResponse result = accountService.validateTransferAccounts(
                    fromAccount.getAccountNumber(),
                    toAccount.getAccountNumber()
            );

            assertThat(result).isNotNull();
            assertThat(result.getFromAccount()).isNotNull();
            assertThat(result.getToAccount()).isNotNull();
        }

        @Test
        void testValidateTransferAccounts_FromAccountNotFound() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto account = accountService.createAccount(USER_ID, EMAIL_ID, request);

            Long invalidUserId = 99999L;
            Long accountNumber = account.getAccountNumber();

            assertThatThrownBy(() -> accountService.validateTransferAccounts(invalidUserId, accountNumber))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        void testValidateTransferAccounts_FromAccountInactive() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto fromAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);
            AccountDto toAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);

            accountService.markAccountFrozen(fromAccount.getAccountNumber());

            Long fromAccountNumber = fromAccount.getAccountNumber();
            Long toAccountNumber = toAccount.getAccountNumber();
            assertThatThrownBy(() -> accountService.validateTransferAccounts(
                    fromAccountNumber, toAccountNumber))
                    .isInstanceOf(InactiveAccountException.class)
                    .hasMessageContaining("source account");
        }

        @Test
        void testValidateTransferAccounts_ToAccountInactive() {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("Account");

            AccountDto fromAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);
            AccountDto toAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);

            accountService.markAccountFrozen(toAccount.getAccountNumber());

            Long fromAccountNumber = fromAccount.getAccountNumber();
            Long toAccountNumber = toAccount.getAccountNumber();
            assertThatThrownBy(() -> accountService.validateTransferAccounts(
                    fromAccountNumber, toAccountNumber))
                    .isInstanceOf(InactiveAccountException.class)
                    .hasMessageContaining("destination account");
        }
    }

    private CreateAccountRequest buildRequest(AccountType type, String name) {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(type);
        request.setAccountName(name);
        return request;
    }

}
