package com.keystone.keystone_backend.service;

import com.keystone.keystone_backend.entity.Notification;
import com.keystone.keystone_backend.entity.User;
import com.keystone.keystone_backend.entity.WorkOrder;
import com.keystone.keystone_backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating and managing in-app notifications.
 *
 * <p><strong>SCOPE:</strong> This is a minimal notification foundation.
 * Notifications are stored in the database only. No external delivery
 * (email, SMS, push) is implemented.</p>
 *
 * <p><strong>USAGE:</strong> Other services (e.g., WorkOrderService) call
 * {@link #notifyAssignment} to create a notification when a work order
 * is assigned to a technician.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Creates an assignment notification for a technician.
     *
     * <p>Called when a dispatcher assigns a work order to a technician.
     * The notification is stored in the database and can be retrieved
     * by the frontend to show in the technician's notification list.</p>
     *
     * @param technician The technician being assigned
     * @param workOrder  The work order being assigned
     */
    @Transactional
    public void notifyAssignment(User technician, WorkOrder workOrder) {
        String message = String.format(
                "Work order %s \"%s\" has been assigned to you.",
                workOrder.getCode(), workOrder.getTitle());

        Notification notification = Notification.builder()
                .user(technician)
                .workOrder(workOrder)
                .type("ASSIGNMENT")
                .message(message)
                .build();

        notificationRepository.save(notification);

        log.info("Assignment notification created for technician {} — WO {}",
                technician.getUsername(), workOrder.getCode());
    }
}
