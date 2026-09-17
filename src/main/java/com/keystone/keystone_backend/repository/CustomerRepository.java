package com.keystone.keystone_backend.repository;

import com.keystone.keystone_backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Customer} entity.
 *
 * <p><strong>HOW THIS WORKS (Interview-critical concept):</strong></p>
 * <p>You write an interface — Spring generates the implementation at runtime.
 * {@link JpaRepository} provides standard CRUD operations (save, findById, findAll,
 * delete, count, existsById) plus pagination and sorting out of the box.</p>
 *
 * <p><strong>Method Name Query Derivation:</strong></p>
 * <p>Spring Data parses method names to generate SQL queries automatically:</p>
 * <ul>
 *   <li>{@code findByCode("CUST-001")} →
 *       {@code SELECT * FROM customers WHERE code = 'CUST-001'}</li>
 *   <li>{@code existsByCode("CUST-001")} →
 *       {@code SELECT EXISTS(SELECT 1 FROM customers WHERE code = 'CUST-001')}</li>
 * </ul>
 *
 * <p><strong>INTERVIEW TIP:</strong> "What's the difference between JpaRepository,
 * CrudRepository, and PagingAndSortingRepository?"</p>
 * <ul>
 *   <li>CrudRepository — basic CRUD (save, findById, delete)</li>
 *   <li>PagingAndSortingRepository extends CrudRepository — adds Pageable support</li>
 *   <li>JpaRepository extends PagingAndSortingRepository — adds batch operations,
 *       flush, and JPA-specific methods like getById()</li>
 * </ul>
 * <p>We use JpaRepository because we need pagination (search APIs) and JPA features.</p>
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find a customer by their unique code (e.g., "CUST-001").
     * Returns Optional to force null-safety — no NullPointerExceptions.
     */
    Optional<Customer> findByCode(String code);

    /**
     * Check if a customer code already exists (for duplicate prevention).
     * More efficient than findByCode() when you only need a boolean check.
     */
    boolean existsByCode(String code);
}
