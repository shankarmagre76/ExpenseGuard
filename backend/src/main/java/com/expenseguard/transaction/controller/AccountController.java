package com.expenseguard.transaction.controller;

import com.expenseguard.transaction.dto.AccountResponse;
import com.expenseguard.transaction.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Protected REST Controller mapping /api/v1/accounts endpoints.
 * Demonstrates ownership-constrained resource access.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Retrieves an account by ID for the currently authenticated user.
     * Note: Client-supplied userId query parameter or body is deliberately ignored for ownership security.
     *
     * @param id Account UUID
     * @param clientUserId Optional client-supplied userId (ignored for server-side security)
     * @return 200 OK with AccountResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable("id") UUID id,
            @RequestParam(value = "userId", required = false) UUID clientUserId
    ) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }
}
