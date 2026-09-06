package com.expenseguard.transaction.repository;

import com.expenseguard.transaction.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Account entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Find all accounts associated with a given user ID.
     */
    List<Account> findByUserId(UUID userId);
}
