package com.keystone.keystone_backend.security;

import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter between our JPA {@link User} entity and Spring Security's {@link UserDetails}.
 *
 * <p><strong>WHY A SEPARATE CLASS? WHY NOT MAKE User IMPLEMENT UserDetails?</strong></p>
 * <ul>
 *   <li><strong>Separation of concerns:</strong> The User entity belongs to the JPA/database
 *       layer. UserDetails belongs to the security layer. Mixing them creates tight coupling
 *       between two unrelated frameworks.</li>
 *   <li><strong>Clean entity:</strong> User entity stays focused on database mapping. It doesn't
 *       need methods like {@code isAccountNonExpired()} which are security concepts.</li>
 *   <li><strong>Flexibility:</strong> If we change the User entity (add fields, change structure),
 *       the security layer is unaffected as long as this adapter is updated.</li>
 * </ul>
 *
 * <p><strong>HOW IT'S USED:</strong></p>
 * <ol>
 *   <li>{@code CustomUserDetailsService.loadUserByUsername()} loads User from DB, converts
 *       to UserPrincipal via {@link #from(User)}</li>
 *   <li>Spring Security stores the UserPrincipal in the SecurityContext</li>
 *   <li>Controllers access it via {@code @AuthenticationPrincipal UserPrincipal principal}</li>
 * </ol>
 *
 * <p><strong>THE "ROLE_" PREFIX:</strong></p>
 * <p>Spring Security requires authorities to have a "ROLE_" prefix when using
 * {@code hasRole("DISPATCHER")}. The prefix is added here so we can write
 * {@code hasRole("DISPATCHER")} instead of {@code hasAuthority("ROLE_DISPATCHER")}
 * in security config. This is a Spring Security convention.</p>
 *
 * <p><strong>CUSTOMER OWNERSHIP:</strong></p>
 * <p>The {@link #customerId} field is critical for data isolation. When a CUSTOMER user
 * makes a request, the controller/service reads {@code principal.getCustomerId()} to
 * scope all database queries to that customer's organization only. This is how we enforce:
 * "Customers must only see their own organization's data."</p>
 *
 * <p><strong>INTERVIEW TIP:</strong> "What is the difference between Authentication and
 * UserDetails in Spring Security?"
 * <br>→ UserDetails represents the USER (who they are, their credentials, authorities).
 * Authentication represents the AUTHENTICATION STATE (is this request authenticated?
 * what principal is it? what authorities does it have?). After successful authentication,
 * the UserDetails object becomes the "principal" inside the Authentication object.</p>
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String fullName;
    private final String password;
    private final Role role;
    private final Long customerId;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Factory method to create a UserPrincipal from a JPA User entity.
     *
     * <p>This is called by {@code CustomUserDetailsService} after loading the User from DB.</p>
     *
     * @param user The JPA User entity loaded from the database
     * @return A UserPrincipal wrapping the user's security-relevant information
     */
    public static UserPrincipal from(User user) {
        // Convert our Role enum to Spring Security's GrantedAuthority with "ROLE_" prefix
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPasswordHash(),       // Spring Security needs this for authentication
                user.getRole(),
                // Safely extract customer ID — null for internal staff (DISPATCHER, TECH, MANAGER)
                user.getCustomer() != null ? user.getCustomer().getId() : null,
                user.getActive(),
                authorities
        );
    }

    // ===== UserDetails interface methods =====
    // These methods control account status. Spring Security checks them during authentication.

    /** Account never expires in our system (we use the 'active' flag instead) */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Account is locked when deactivated (active = false) */
    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    /** Credentials never expire (we don't implement password rotation) */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** User is enabled when active */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
