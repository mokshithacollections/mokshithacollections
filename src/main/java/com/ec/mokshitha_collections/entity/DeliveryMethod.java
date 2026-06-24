package com.ec.mokshitha_collections.entity;

/**
 * How an order to this address is fulfilled. STORE_COLLECT (free) is only
 * offered when the address city is the store's city (Ongole); everything else
 * is HOME_DELIVERY with the normal shipping fee.
 */
public enum DeliveryMethod {
    HOME_DELIVERY,
    STORE_COLLECT
}
