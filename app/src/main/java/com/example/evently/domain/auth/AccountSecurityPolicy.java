package com.example.evently.domain.auth;

import androidx.annotation.NonNull;

/** Pure validation for sensitive password-account operations. */
public final class AccountSecurityPolicy {
    public enum PasswordResult { VALID, CURRENT_REQUIRED, NEW_TOO_SHORT, CONFIRMATION_MISMATCH, UNCHANGED }
    private AccountSecurityPolicy() { }

    @NonNull public static PasswordResult validatePasswordChange(
            String currentPassword, String newPassword, String confirmation) {
        if (currentPassword == null || currentPassword.isEmpty()) return PasswordResult.CURRENT_REQUIRED;
        if (newPassword == null || newPassword.length() < 6) return PasswordResult.NEW_TOO_SHORT;
        if (!newPassword.equals(confirmation)) return PasswordResult.CONFIRMATION_MISMATCH;
        if (newPassword.equals(currentPassword)) return PasswordResult.UNCHANGED;
        return PasswordResult.VALID;
    }
}
