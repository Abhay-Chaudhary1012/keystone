package com.keystone.keystone_backend.repository;

import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link User} entity.
 *
 * <p>This repository is critical for two areas:</p>
 * <ol>
 *   <li><strong>Authentication (Phase 2)</strong> — {@code findByUsername()} is used by
 *       the custom UserDetailsService to load user credentials during login.</li>
 *   <li><strong>Authorization</strong> — methods like {@code findByRole()} support
 *       querying technicians for assignment, customer users for portal access, etc.</li>
 * </ol>
 *
 * <p><strong>INTERVIEW TIP — @Repository annotation:</strong></p>
 * <p>Technically, Spring Data JPA auto-detects interfaces extending JpaRepository
 * and creates beans for them. The @Repository annotation is not strictly required here,
 * but we include it for two reasons:</p>
 * <ul>
 *   <li>Makes the intent explicit (this is a data access component)</li>
 *   <li>Enables Spring's PersistenceExceptionTranslationPostProcessor to translate
 *       low-level JDBC/Hibernate exceptions into Spring's DataAccessException hierarchy</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username — primary method for authentication.
     * The JWT login flow: username → findByUsername → verify BCrypt password → issue token.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email — alternative lookup for "forgot password" or registration checks.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username is taken (used during user creation to prevent duplicates).
     */
    boolean existsByUsername(String username);

    /**
     * Check if email is taken (used during user creation to prevent duplicates).
     */
    boolean existsByEmail(String email);

    /**
     * Find all users with a specific role.
     * Use case: "list all technicians" for the assignment dropdown in the dispatcher UI.
     */
    List<User> findByRole(Role role);

    /**
     * Find all active users with a specific role.
     * Filters out deactivated accounts.
     */
    List<User> findByRoleAndActiveTrue(Role role);
}
