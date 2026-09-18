package com.keystone.keystone_backend.repository;

import com.keystone.keystone_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the {@link Notification} entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Find all notifications for a user, newest first. */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Find unread notifications for a user (for badge count). */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    /** Count unread notifications for a user. */
    long countByUserIdAndReadFalse(Long userId);
}
