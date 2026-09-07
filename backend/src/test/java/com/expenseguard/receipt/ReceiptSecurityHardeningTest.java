package com.expenseguard.receipt;

import com.expenseguard.ExpenseGuardApplication;
import com.expenseguard.auth.dto.RegisterRequest;
import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.repository.UserRepository;
import com.expenseguard.auth.security.JwtService;
import com.expenseguard.auth.service.AuthService;
import com.expenseguard.receipt.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 6.1 Hardening Test Suite: Receipt Security, Path Traversal Prevention, and File Boundaries.
 */
@SpringBootTest(classes = ExpenseGuardApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReceiptSecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        receiptRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest register = RegisterRequest.builder()
                .name("Receipt Sec User")
                .email("receiptsec@example.com")
                .password("Password@123")
                .build();
        authService.registerUser(register);
        user = userRepository.findByEmail("receiptsec@example.com").orElseThrow();
        token = "Bearer " + jwtService.generateToken(user);
    }

    @Test
    @DisplayName("Receipt Sec 1: Path traversal filename (../../etc/passwd) is safely rejected or sanitized")
    void pathTraversalFilenameHandledSafely() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd",
                "image/jpeg",
                "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Receipt Sec 2: Empty file upload returns 400 Bad Request")
    void emptyFileReturns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(emptyFile)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Receipt Sec 3: Unsupported MIME type (text/html) returns 400 Bad Request")
    void unsupportedMimeTypeReturns400() throws Exception {
        MockMultipartFile htmlFile = new MockMultipartFile(
                "file",
                "malicious.html",
                "text/html",
                "<html><body>script</body></html>".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/receipts")
                        .file(htmlFile)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }
}
