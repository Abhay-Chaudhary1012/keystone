package com.keystone.keystone_backend.security;

import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService}.
 *
 * <p><strong>WHAT THIS CLASS DOES:</strong></p>
 * <p>Loads a user from the PostgreSQL database and converts it to a {@link UserPrincipal}
 * (which implements {@link UserDetails}). This is the bridge between our database
 * and Spring Security's authentication system.</p>
 *
 * <p><strong>WHERE IT'S USED:</strong></p>
 * <ul>
 *   <li>{@code JwtAuthenticationFilter} — on every authenticated request, this service
 *       loads the user from the DB to verify the JWT's subject still exists and is active</li>
 * </ul>
 *
 * <p><strong>WHY @Transactional(readOnly = true)?</strong></p>
 * <p>The {@link User} entity has a LAZY {@code @ManyToOne} relationship to {@code Customer}.
 * When {@code UserPrincipal.from(user)} accesses {@code user.getCustomer()}, Hibernate
 * needs an active session to resolve the lazy proxy. {@code @Transactional} ensures the
 * Hibernate session stays open during the entire method call.</p>
 * <ul>
 *   <li>{@code readOnly = true} — tells Hibernate this is a read-only transaction,
 *       enabling performance optimizations (no dirty checking, no flush at commit)</li>
 * </ul>
 *
 * <p><strong>INTERVIEW TIP:</strong> "What is LazyInitializationException?"
 * <br>→ It occurs when you try to access a LAZY-loaded relationship AFTER the
 * Hibernate session has closed. The fix is to either: (1) access the relationship
 * within a @Transactional method, (2) use a JOIN FETCH query, or (3) use
 * a DTO projection. We use option (1) here.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by username and converts to Spring Security's UserDetails.
     *
     * <p>Called by:</p>
     * <ul>
     *   <li>{@code JwtAuthenticationFilter} — to validate the JWT subject on every request</li>
     * </ul>
     *
     * @param username The username to look up
     * @return UserPrincipal wrapping the found user
     * @throws UsernameNotFoundException if no user exists with the given username
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username
                ));

        return UserPrincipal.from(user);
    }
}
