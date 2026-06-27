package com.backendev.accountservice.integration.controller;

import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.integration.config.TestSecurityConfig;
import com.backendev.accountservice.jwt.JwtAuthenticationFilter;
import com.backendev.accountservice.repository.AccountRepository;
import com.backendev.accountservice.service.AccountService;
import com.backendev.accountservice.service.SecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class AccountControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Mock
    private SecurityService securityService;

    @Autowired
    private AccountRepository accountRepository;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "/api/v1/accounts";
    private static final String USER_ID = "user-123";
    private static final String EMAIL_ID = "user@example.com";

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        reset(securityService);
        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Nested
    class CreateAccountTests {

        @Test
        void testCreateAccount_Success() throws Exception {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("My Savings Account");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("SAVINGS"))
                    .andExpect(jsonPath("$.accountName").value("My Savings Account"))
                    .andExpect(jsonPath("$.userId").value(USER_ID));

            // Verify account saved in database
            var accounts = accountRepository.findAll();
            assertThat(accounts).isNotEmpty();
        }

        @Test
        void testCreateAccount_WithoutAccountName() throws Exception {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.CHECKING);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        void testCreateAccount_InvalidAccountType() throws Exception {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(null);  // Invalid
            request.setAccountName("Test");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class FetchAccountDetailsTests {

        @Test
        void testFetchMyAccountDetails_Success() throws Exception {
            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.SAVINGS);
            createRequest.setAccountName("My Account");

            AccountDto createdAccount = accountService.createAccount(USER_ID, EMAIL_ID, createRequest);
            Long accountNumber = createdAccount.getAccountNumber();

            mockMvc.perform(get(BASE_URL + "/me/" + accountNumber))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                    .andExpect(jsonPath("$.userId").value(USER_ID));
        }

        @Test
        void testFetchMyAccountDetails_AccountNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me/99999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class FetchAccountForTransactionService {
        @Test
        void testFetchAccountDetails_Success() throws Exception {
            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.CHECKING);
            createRequest.setAccountName("Checking Account");

            AccountDto createdAccount = accountService.createAccount(USER_ID, EMAIL_ID, createRequest);
            Long accountNumber = createdAccount.getAccountNumber();

            mockMvc.perform(get(BASE_URL + "/" + accountNumber))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountNumber").value(accountNumber));
        }

        @Test
        @WithMockUser(username = "other-user", roles = "USER")
        void testFetchAccountDetails_AccessDenied() throws Exception {
            String otherUserId = "other-user-456";
            reset(securityService);
            when(securityService.getCurrentUserId()).thenReturn(otherUserId);

            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.SAVINGS);
            createRequest.setAccountName("User Account");

            accountService.createAccount(USER_ID, EMAIL_ID, createRequest);
            var accounts = accountRepository.findAll();
            Long accountNumber = accounts.get(0).getAccountNumber();

            mockMvc.perform(get(BASE_URL + "/" + accountNumber))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ValidateAccountsForTransfer {

        @Test
        void testValidateAccountsForTransfer_Success() throws Exception {
            CreateAccountRequest request = new CreateAccountRequest();
            request.setAccountType(AccountType.SAVINGS);
            request.setAccountName("From Account");

            AccountDto fromAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);

            request.setAccountName("To Account");
            AccountDto toAccount = accountService.createAccount(USER_ID, EMAIL_ID, request);

            mockMvc.perform(get(BASE_URL + "/validate-transfer")
                            .param("fromAccountNumber", fromAccount.getAccountNumber().toString())
                            .param("toAccountNumber", toAccount.getAccountNumber().toString())
                            .param("userId", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromAccount").exists())
                    .andExpect(jsonPath("$.toAccount").exists());
        }
    }

    @Nested
    class UpdateAccountBalance {

        @Test
        void testUpdateAccountBalance_Success() throws Exception {
            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.SAVINGS);
            createRequest.setAccountName("Savings Account");

            AccountDto createdAccount = accountService.createAccount(USER_ID, EMAIL_ID, createRequest);

            UpdateAccountBalanceRequest updateRequest = new UpdateAccountBalanceRequest();
            updateRequest.setAccountNumber(createdAccount.getAccountNumber());
            updateRequest.setBalance(BigDecimal.valueOf(5000.00));

            mockMvc.perform(put(BASE_URL + "/" + createdAccount.getAccountNumber())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());

            var accounts = accountRepository.findAll();
            assertThat(accounts.get(0).getBalance()).isEqualTo(BigDecimal.valueOf(5000.00));
        }
    }

    @Nested
    class FetchAccountForUser {

        @Test
        void testFetchAccountsForUser_Success() throws Exception {
            CreateAccountRequest request1 = new CreateAccountRequest();
            request1.setAccountType(AccountType.SAVINGS);
            request1.setAccountName("Savings");
            accountService.createAccount(USER_ID, EMAIL_ID, request1);

            CreateAccountRequest request2 = new CreateAccountRequest();
            request2.setAccountType(AccountType.CHECKING);
            request2.setAccountName("Checking");
            accountService.createAccount(USER_ID, EMAIL_ID, request2);

            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void testFetchAccountsForUser_NoAccounts() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class DeleteAccount{

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void testDeleteAccount_Success() throws Exception {
            reset(securityService);
            when(securityService.getCurrentUserId()).thenReturn(USER_ID);

            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.SAVINGS);
            createRequest.setAccountName("Account to Delete");

            AccountDto createdAccount = accountService.createAccount(USER_ID, EMAIL_ID, createRequest);

            mockMvc.perform(delete(BASE_URL + "/" + createdAccount.getAccountNumber()))
                    .andExpect(status().isOk());

            var accounts = accountRepository.findAll();
            assertThat(accounts).isEmpty();
        }

        @Test
        void testDeleteAccount_NonAdminForbidden() throws Exception {
            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.SAVINGS);
            createRequest.setAccountName("Account");

            AccountDto createdAccount = accountService.createAccount(USER_ID, EMAIL_ID, createRequest);

            mockMvc.perform(delete(BASE_URL + "/" + createdAccount.getAccountNumber()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class MarkAccountFrozen{

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void testMarkAccountFrozen_Success() throws Exception {
            reset(securityService);
            when(securityService.getCurrentUserId()).thenReturn(USER_ID);

            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.SAVINGS);
            createRequest.setAccountName("Account");

            AccountDto createdAccount = accountService.createAccount(USER_ID, EMAIL_ID, createRequest);

            mockMvc.perform(put(BASE_URL + "/" + createdAccount.getAccountNumber() + "/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FROZEN"));

            var accounts = accountRepository.findAll();
            assertThat(accounts.get(0).getStatus()).isEqualTo(AccountStatus.FROZEN);
        }

        @Test
        void testMarkAccountFrozen_NonAdminForbidden() throws Exception {
            CreateAccountRequest createRequest = new CreateAccountRequest();
            createRequest.setAccountType(AccountType.SAVINGS);
            createRequest.setAccountName("Account");

            AccountDto createdAccount = accountService.createAccount(USER_ID, EMAIL_ID, createRequest);

            mockMvc.perform(put(BASE_URL + "/" + createdAccount.getAccountNumber() + "/status"))
                    .andExpect(status().isForbidden());
        }
    }
}
