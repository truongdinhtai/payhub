package com.payhub.audit.domain;

/** The kinds of change recorded in the audit trail. */
public enum AuditAction {
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_PLAN_CHANGED,
    SUBSCRIPTION_CANCELED,
    SUBSCRIPTION_ACTIVATED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED
}
