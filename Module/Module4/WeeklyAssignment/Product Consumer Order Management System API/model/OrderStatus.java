package org.nbfc.productwa.model;

/**
 * Lifecycle states of an {@link Order}.
 */
public enum OrderStatus {
    PLACED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
