package com.example.evently.domain.common;

import androidx.annotation.Nullable;
import com.example.evently.data.model.VerificationRequest;

/** Pure validation shared by account and admin UI actions. */
public final class AccountActionPolicy {
    private AccountActionPolicy() { }

    public static boolean hasRejectReason(@Nullable String reason) {
        return reason != null && !reason.trim().isEmpty();
    }

    public static boolean canCancelVerification(@Nullable String status) {
        return VerificationRequest.STATUS_PENDING.equals(status);
    }

    public static boolean canResubmitVerification(@Nullable String status) {
        return VerificationRequest.STATUS_REJECTED.equals(status);
    }
}
