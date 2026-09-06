package com.expenseguard.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health Check REST Controller providing system status verification.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "ExpenseGuard backend is running successfully",
                "timestamp", Instant.now().toString()
        ));
    }
}
