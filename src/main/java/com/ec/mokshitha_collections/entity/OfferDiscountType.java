package com.ec.mokshitha_collections.entity;

/** How an offer's discount is calculated. */
public enum OfferDiscountType {
    PERCENTAGE,   // discountValue is a % (e.g. 20 = 20% off), capped by maxDiscountAmount
    FIXED         // discountValue is a flat ₹ amount off
}
