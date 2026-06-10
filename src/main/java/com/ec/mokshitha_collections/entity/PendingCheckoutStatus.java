package com.ec.mokshitha_collections.entity;

/** Lifecycle of a reserve-at-Pay checkout while a Razorpay payment is in flight. */
public enum PendingCheckoutStatus {
    /** Stock reserved, awaiting payment confirmation. */
    HELD,
    /** Payment succeeded and the real Order was created. */
    CONFIRMED,
    /** Payment failed/abandoned/expired — reserved stock was returned. */
    RELEASED
}
