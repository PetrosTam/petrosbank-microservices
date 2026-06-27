package com.backendev.userservice.audit;

public enum AuditEventType {

    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    REGISTRATION,
    PROFILE_UPDATED,
    INVALID_TOKEN,
    TOKEN_EXPIRED
}
