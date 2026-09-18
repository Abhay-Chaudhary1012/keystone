package com.keystone.keystone_backend.repository;

import com.keystone.keystone_backend.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the {@link Site} entity.
 *
 * <p>Provides data access for facility sites. Key use cases:</p>
 * <ul>
 *   <li>List sites for a customer (dropdown when creating a Work Order)</li>
 *   <li>Filter active sites only</li>
 * </ul>
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    /** Find all sites belonging to a customer organization. */
    List<Site> findByCustomerId(Long customerId);

    /** Find active sites for a customer (for dropdowns/selection). */
    List<Site> findByCustomerIdAndActiveTrue(Long customerId);
}
