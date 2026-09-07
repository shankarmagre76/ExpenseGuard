package com.expenseguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ExpenseGuardApplication.class)
@ActiveProfiles("test")
class ExpenseGuardApplicationTests {

    @Test
    void contextLoads() {
    }

}
