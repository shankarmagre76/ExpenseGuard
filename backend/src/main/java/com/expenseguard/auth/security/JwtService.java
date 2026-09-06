package com.expenseguard.auth.security;

import com.expenseguard.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service for generating, signing, validating, and parsing JSON Web Tokens (JWT).
 */
@Slf4j
@Service
public class JwtService {

    @Value("${expenseguard.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    @Getter
    @Value("${expenseguard.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /**
     * Generates a signed JWT access token containing subject and non-sensitive claims.
     *
     * @param user Authenticated user entity
     * @return Compact serialized JWT token string
     */
    public String generateToken(User user) {
        return generateToken(user, jwtExpirationMs);
    }

    /**
     * Generates a signed JWT access token with a custom expiration duration.
     * Useful for testing token expiration handling.
     *
     * @param user Authenticated user entity
     * @param expirationMs Expiration duration in milliseconds
     * @return Compact serialized JWT token string
     */
    public String generateToken(User user, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validates JWT token structure, signature, and expiration.
     *
     * @param token JWT token string
     * @return true if token is valid and not expired; false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts user ID (subject claim) from the token.
     *
     * @param token JWT token string
     * @return User ID string (UUID)
     */
    public String getUserIdFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts user email claim from the token.
     *
     * @param token JWT token string
     * @return User email string
     */
    public String getEmailFromToken(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * Parses and verifies JWT claims using the secret key.
     *
     * @param token Compact JWT token string
     * @return Extracted Claims
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Resolves secret key bytes supporting Base64-encoded strings and UTF-8 fallbacks.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
