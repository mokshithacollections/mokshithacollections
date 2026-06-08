package com.ec.mokshitha_collections.entity;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean isCancellable() {
        return this == PLACED || this == CONFIRMED;
    }

    /** The next status in the normal fulfilment flow, or null if terminal. */
    public OrderStatus nextStep() {
        return switch (this) {
            case PLACED     -> CONFIRMED;
            case CONFIRMED  -> PROCESSING;
            case PROCESSING -> SHIPPED;
            case SHIPPED    -> DELIVERED;
            default         -> null; // DELIVERED, CANCELLED are terminal
        };
    }

    /**
     * Whether an admin may move this order to {@code target}. Allowed:
     *  - no change (re-saving to edit tracking info / payment status),
     *  - exactly the next step in the flow (no skipping), or
     *  - cancelling, while the order is still cancellable.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == this) return true;
        if (target == CANCELLED) return isCancellable();
        return target == nextStep();
    }
}
