package com.keystone.keystone_backend.exception;

/**
 * Thrown when a requested resource does not exist in the database.
 *
 * <p>Handled by {@code GlobalExceptionHandler} → returns HTTP 404 Not Found.</p>
 *
 * <p><strong>Usage examples (future phases):</strong></p>
 * <ul>
 *   <li>{@code throw new ResourceNotFoundException("Customer", "id", 42);}
 *       → "Customer not found with id: '42'"</li>
 *   <li>{@code throw new ResourceNotFoundException("Work Order not found");}
 *       → Custom message</li>
 * </ul>
 *
 * <p><strong>INTERVIEW TIP:</strong> "Why extend RuntimeException, not Exception?"
 * <br>→ RuntimeException is an <strong>unchecked</strong> exception — methods don't
 * need to declare it with {@code throws}. Spring's {@code @ExceptionHandler} can
 * catch both checked and unchecked exceptions, but unchecked is more convenient
 * because every method in the call chain doesn't need a throws clause.
 * Business exceptions (like "not found") are typically unchecked in Spring apps.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor for the common pattern:
     * "Customer not found with id: '42'"
     *
     * @param resourceName The type of resource (e.g., "Customer", "WorkOrder")
     * @param fieldName    The field used for lookup (e.g., "id", "code")
     * @param fieldValue   The value that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
