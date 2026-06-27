package com.backendev.accountservice.controller;

import com.backendev.accountservice.dto.AccountDetailsDto;
import com.backendev.accountservice.dto.AccountDto;
import com.backendev.accountservice.dto.AccountResponse;
import com.backendev.accountservice.dto.CreateAccountRequest;
import com.backendev.accountservice.dto.TransferValidationResponse;
import com.backendev.accountservice.dto.UpdateAccountBalanceRequest;
import com.backendev.accountservice.exception.AccountAccessDeniedException;
import com.backendev.accountservice.service.AccountService;
import com.backendev.accountservice.service.SecurityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
public class AccountController {

    private final AccountService accountService;

    private final SecurityService securityService;


    public AccountController(AccountService accountService, SecurityService securityService) {
        this.accountService = accountService;
        this.securityService = securityService;
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest accountRequest){
        String userId = securityService.getCurrentUserId();
        String currentUserEmail = securityService.getCurrentUserEmail();
        AccountDto accountDto = accountService.createAccount(userId, currentUserEmail, accountRequest);
        return new ResponseEntity<>(accountDto,HttpStatus.CREATED);
    }

    @GetMapping("/me/{accountNumber}")
    public ResponseEntity<AccountDetailsDto> fetchMyAccountDetails(@NotNull @PathVariable Long accountNumber){
        String userId = securityService.getCurrentUserId();
        AccountDetailsDto accountDetailsDto = accountService.fetchAccountDetails(accountNumber, userId);
        return new ResponseEntity<>(accountDetailsDto,HttpStatus.OK);
    }

    //For transaction service
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDetailsDto> fetchAccountDetails(@NotNull @PathVariable Long accountNumber){
        String userId = securityService.getCurrentUserId();
        if (!accountService.doesUserOwnAccount(userId, accountNumber)) {
            throw new AccountAccessDeniedException("User does not own this account");
        }
        AccountDetailsDto accountDetailsDto = accountService.fetchAccountDetails(accountNumber, userId);
        return new ResponseEntity<>(accountDetailsDto,HttpStatus.OK);
    }

    //For transaction service -  validating accounts for transfer
    @GetMapping("/validate-transfer")
    public ResponseEntity<TransferValidationResponse> validateAccountsForTransfer(@RequestParam @NotNull Long fromAccountNumber,
                                                                                  @RequestParam @NotNull Long toAccountNumber,
                                                                                  @RequestParam String userId){
        if (!accountService.doesUserOwnAccount(userId, fromAccountNumber)) {
            throw new AccountAccessDeniedException("User does not own this account");
        }
        TransferValidationResponse transferValidationResponse =
                accountService.validateTransferAccounts(fromAccountNumber, toAccountNumber);
        return new ResponseEntity<>(transferValidationResponse,HttpStatus.OK);

    }

    //For transaction service to update balance
    @PutMapping("/{accountNumber}")
    public ResponseEntity<AccountDto> updateAccountBalance(@PathVariable Long accountNumber, @Valid @RequestBody UpdateAccountBalanceRequest updateAccountBalanceRequest){
        log.info("Received request for account: {}, balance: {}",
                updateAccountBalanceRequest.getAccountNumber(),
                updateAccountBalanceRequest.getBalance());
        AccountDto accountDto = accountService.updateAccountBalance(updateAccountBalanceRequest);
        return new ResponseEntity<>(accountDto, HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AccountDetailsDto>> fetchAccountsForUser(){
        String userId = securityService.getCurrentUserId();
        List<AccountDetailsDto> userAccounts = accountService.fetchAccountsForUser(userId);
        return new ResponseEntity<>(userAccounts, HttpStatus.OK);
    }

    @GetMapping("/{accountNumber}/status")
    public ResponseEntity<AccountDto> fetchAccountBalance(@NotNull @PathVariable Long accountNumber){
    return null;
    }

    @DeleteMapping("/{accountNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> deleteMapping(@NotNull @PathVariable Long accountNumber){
        AccountResponse accountResponse = accountService.deleteAccount(accountNumber);
        return new ResponseEntity<>(accountResponse, HttpStatus.OK);

    }

    @PutMapping("/{accountNumber}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountDto> markAccountFrozen(@NotNull @PathVariable Long accountNumber){
        AccountDto accountDto = accountService.markAccountFrozen(accountNumber);
        return new ResponseEntity<>(accountDto, HttpStatus.OK);
    }

}
