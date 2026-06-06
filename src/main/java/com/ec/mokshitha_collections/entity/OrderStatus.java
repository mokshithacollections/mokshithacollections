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
}
