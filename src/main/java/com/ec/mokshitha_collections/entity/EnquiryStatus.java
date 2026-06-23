package com.ec.mokshitha_collections.entity;

/** Lifecycle of a contact-form enquiry as seen by the admin. */
public enum EnquiryStatus {
    NEW,   // just submitted, not yet reviewed
    SEEN   // an admin has marked it reviewed
}
