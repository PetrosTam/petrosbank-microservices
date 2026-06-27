package com.backendev.accountservice.unit.controller;

import com.backendev.accountservice.controller.AccountController;
import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountResponse;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.enums.AccountStatus;
import com.backendev.accountservice.enums.AccountType;
import com.backendev.accountservice.exception.AccountAlreadyExistsException;
import com.backendev.accountservice.exception.GlobalExceptionHandler;
import com.backendev.accountservice.service.AccountService;
import com.backendev.accountservice.service.SecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String USER_ID = "user123";
    private static final String EMAIL_ID = "user@exmple.com";
    private static final Long ACCOUNT_NUMBER = 1234567890L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createAccount_ShouldReturnCreatedAccount_WhenValidRequest() throws Exception {
        CreateAccountRequest request = createValidAccountRequest();
        AccountDto expectedResponse = createAccountDto();

        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
        when(securityService.getCurrentUserEmail()).thenReturn(EMAIL_ID);
        when(accountService.createAccount(USER_ID, EMAIL_ID, request)).thenReturn(expectedResponse);

        
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value(expectedResponse.getAccountNumber()))
                .andExpect(jsonPath("$.userId").value(expectedResponse.getUserId()))
                .andExpect(jsonPath("$.type").value(expectedResponse.getType().toString()))
                .andExpect(jsonPath("$.status").value(expectedResponse.getStatus().toString()));

        verify(securityService).getCurrentUserId();
        verify(accountService).createAccount(USER_ID, EMAIL_ID, request);
    }

    @Test
    void createAccount_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        CreateAccountRequest invalidRequest = new CreateAccountRequest();
        invalidRequest.setAccountName("Test Account");
        // accountType is null, which should trigger validation error
        
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(securityService, never()).getCurrentUserId();
        verify(accountService, never()).createAccount(anyString(), anyString(), any());
    }

    @Test
    void fetchMyAccountDetails_ShouldReturnAccountDetails_WhenValidRequest() throws Exception {
        AccountDetailsDto expectedResponse = createAccountDetailsDto();

        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
        when(accountService.fetchAccountDetails(ACCOUNT_NUMBER, USER_ID)).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/v1/accounts/me/{accountNumber}", ACCOUNT_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(expectedResponse.getAccountNumber()))
                .andExpect(jsonPath("$.userId").value(expectedResponse.getUserId()))
                .andExpect(jsonPath("$.type").value(expectedResponse.getType().toString()))
                .andExpect(jsonPath("$.status").value(expectedResponse.getStatus().toString()))
                .andExpect(jsonPath("$.balance").value(expectedResponse.getBalance()));

        verify(securityService).getCurrentUserId();
        verify(accountService).fetchAccountDetails(ACCOUNT_NUMBER, USER_ID);
    }

    @Test
    void fetchAccountDetails_ShouldReturnAccountDetails_WhenUserOwnsAccount() throws Exception {
        AccountDetailsDto expectedResponse = createAccountDetailsDto();

        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
        when(accountService.doesUserOwnAccount(USER_ID, ACCOUNT_NUMBER)).thenReturn(true);
        when(accountService.fetchAccountDetails(ACCOUNT_NUMBER, USER_ID)).thenReturn(expectedResponse);
        
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", ACCOUNT_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(expectedResponse.getAccountNumber()))
                .andExpect(jsonPath("$.userId").value(expectedResponse.getUserId()))
                .andExpect(jsonPath("$.type").value(expectedResponse.getType().toString()))
                .andExpect(jsonPath("$.status").value(expectedResponse.getStatus().toString()))
                .andExpect(jsonPath("$.balance").value(expectedResponse.getBalance()));

        verify(securityService).getCurrentUserId();
        verify(accountService).doesUserOwnAccount(USER_ID, ACCOUNT_NUMBER);
        verify(accountService).fetchAccountDetails(ACCOUNT_NUMBER, USER_ID);
    }

    @Test
    void fetchAccountDetails_ShouldReturnForbidden_WhenUserDoesNotOwnAccount() throws Exception {
        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
        when(accountService.doesUserOwnAccount(USER_ID, ACCOUNT_NUMBER)).thenReturn(false);

        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", ACCOUNT_NUMBER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.errorMessage").value("User does not own this account"))
                .andExpect(jsonPath("$.errorDetails").value("Account access denied."));

        verify(securityService).getCurrentUserId();
        verify(accountService).doesUserOwnAccount(USER_ID, ACCOUNT_NUMBER);
        verify(accountService, never()).fetchAccountDetails(anyLong(), anyString());
    }



    @Test
    void updateAccountBalance_ShouldUpdateBalance_WhenValidRequest() throws Exception {
        UpdateAccountBalanceRequest request = createUpdateBalanceRequest();
        AccountDto expectedResponse = createAccountDto();

        when(accountService.updateAccountBalance(request)).thenReturn(expectedResponse);
        
        mockMvc.perform(put("/api/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(expectedResponse.getAccountNumber()))
                .andExpect(jsonPath("$.userId").value(expectedResponse.getUserId()))
                .andExpect(jsonPath("$.type").value(expectedResponse.getType().toString()))
                .andExpect(jsonPath("$.status").value(expectedResponse.getStatus().toString()));

        verify(accountService).updateAccountBalance(request);
    }

    @Test
    void updateAccountBalance_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        UpdateAccountBalanceRequest invalidRequest = new UpdateAccountBalanceRequest();
        
        mockMvc.perform(put("/api/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(accountService, never()).updateAccountBalance(any());
    }

    @Test
    void fetchAccountsForUser_ShouldReturnUserAccounts() throws Exception {
        List<AccountDetailsDto> expectedAccounts = Arrays.asList(
                createAccountDetailsDto(),
                createAccountDetailsDto()
        );

        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
        when(accountService.fetchAccountsForUser(USER_ID)).thenReturn(expectedAccounts);

        
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(expectedAccounts.size()));

        verify(securityService).getCurrentUserId();
        verify(accountService).fetchAccountsForUser(USER_ID);
    }

    @Test
    void fetchAccountBalance_ShouldReturnNull() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}/status", ACCOUNT_NUMBER))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deleteAccount_ShouldDeleteAccount_WhenAdminRole() throws Exception {
        AccountResponse expectedResponse = new AccountResponse();
        expectedResponse.setMessage("Account deleted successfully");

        when(accountService.deleteAccount(ACCOUNT_NUMBER)).thenReturn(expectedResponse);

        
        mockMvc.perform(delete("/api/v1/accounts/{accountNumber}", ACCOUNT_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(accountService).deleteAccount(ACCOUNT_NUMBER);
    }

    @Test
    void markAccountFrozen_ShouldFreezeAccount_WhenAdminRole() throws Exception {
        AccountDto expectedResponse = createAccountDto();
        expectedResponse.setStatus(AccountStatus.FROZEN);

        when(accountService.markAccountFrozen(ACCOUNT_NUMBER)).thenReturn(expectedResponse);

        mockMvc.perform(put("/api/v1/accounts/{accountNumber}/status", ACCOUNT_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(expectedResponse.getAccountNumber()))
                .andExpect(jsonPath("$.status").value(AccountStatus.FROZEN.toString()));

        verify(accountService).markAccountFrozen(ACCOUNT_NUMBER);
    }

    @Test
    void createAccount_ShouldReturnInternalServerError_WhenServiceException() throws Exception {
        CreateAccountRequest request = createValidAccountRequest();

        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
        when(securityService.getCurrentUserEmail()).thenReturn(EMAIL_ID);

        when(accountService.createAccount(USER_ID, EMAIL_ID, request))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("Service error"))
                .andExpect(jsonPath("$.errorDetails").value("An unexpected error occurred."));

        verify(securityService).getCurrentUserId();
        verify(securityService).getCurrentUserEmail();
        verify(accountService).createAccount(USER_ID, EMAIL_ID, request);
    }

    @Test
    void createAccount_ShouldReturnConflict_WhenAccountAlreadyExists() throws Exception {
        CreateAccountRequest request = createValidAccountRequest();

        when(securityService.getCurrentUserId()).thenReturn(USER_ID);
        when(securityService.getCurrentUserEmail()).thenReturn(EMAIL_ID);
        when(accountService.createAccount(USER_ID, EMAIL_ID, request))
                .thenThrow(new AccountAlreadyExistsException("Account already exists"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.errorMessage").value("Account already exists"))
                .andExpect(jsonPath("$.errorDetails").value("The account already exists."));

        verify(securityService).getCurrentUserId();
        verify(securityService).getCurrentUserEmail();
        verify(accountService).createAccount(USER_ID, EMAIL_ID, request);
    }

    @Test
    void fetchMyAccountDetails_ShouldReturnInternalServerError_WhenInvalidPathVariable() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me/null"))
                .andExpect(status().isInternalServerError());
    }

    private CreateAccountRequest createValidAccountRequest() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(AccountType.SAVINGS);
        request.setAccountName("My Savings Account");
        return request;
    }

    private AccountDto createAccountDto() {
        AccountDto dto = new AccountDto();
        dto.setAccountNumber(ACCOUNT_NUMBER);
        dto.setUserId(USER_ID);
        dto.setType(AccountType.SAVINGS);
        dto.setStatus(AccountStatus.ACTIVE);
        dto.setAccountName("My Savings Account");
        return dto;
    }

    private AccountDetailsDto createAccountDetailsDto() {
        AccountDetailsDto dto = new AccountDetailsDto();
        dto.setAccountNumber(ACCOUNT_NUMBER);
        dto.setUserId(USER_ID);
        dto.setType(AccountType.SAVINGS);
        dto.setStatus(AccountStatus.ACTIVE);
        dto.setAccountName("My Savings Account");
        dto.setBalance(BigDecimal.valueOf(1000.00));
        dto.setCreatedAt(Instant.now());
        return dto;
    }

    private UpdateAccountBalanceRequest createUpdateBalanceRequest() {
        UpdateAccountBalanceRequest request = new UpdateAccountBalanceRequest();
        request.setAccountNumber(ACCOUNT_NUMBER);
        request.setBalance(BigDecimal.valueOf(1500.00));
        return request;
    }
}