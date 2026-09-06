package com.expenseguard.auth.repository;

import com.expenseguard.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their unique email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email address.
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user exists with the given email address, ignoring case.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find a user by their email address ignoring case.
     */
    Optional<User> findByEmailIgnoreCase(String email);
}
