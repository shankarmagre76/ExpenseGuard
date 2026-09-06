package com.expenseguard.transaction.controller;

import com.expenseguard.transaction.dto.AccountRequest;
import com.expenseguard.transaction.dto.AccountResponse;
import com.expenseguard.transaction.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Protected REST Controller mapping /api/v1/accounts endpoints for Account management.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Creates a new financial account for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all accounts belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> response = accountService.getAllAccountsForCurrentUser();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific account by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID clientUserId
    ) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing account owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AccountRequest request
    ) {
        AccountResponse response = accountService.updateAccount(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an account owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable("id") UUID id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
