package com.keystone.keystone_backend.exception;

/**
 * Thrown when a request violates a business rule.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>Attempting to edit a COMPLETED or CANCELLED work order</li>
 *   <li>Assigning a site that does not belong to the work order's customer</li>
 *   <li>Invalid status transition</li>
 * </ul>
 *
 * <p>Handled by {@code GlobalExceptionHandler} → returns HTTP 400 Bad Request
 * (or 409 Conflict depending on the specific case).</p>
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
