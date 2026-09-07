package com.expenseguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the ExpenseGuard Spring Boot Backend Application.
 */
@SpringBootApplication
@EnableScheduling
public class ExpenseGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseGuardApplication.class, args);
    }

}
