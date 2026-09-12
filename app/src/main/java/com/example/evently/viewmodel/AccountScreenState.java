package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Small immutable state contract for account screens. */
public final class AccountScreenState {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR, AUTH_REQUIRED }

    @NonNull private final Status status;
    @Nullable private final String errorCode;

    private AccountScreenState(@NonNull Status status, @Nullable String errorCode) {
        this.status = status;
        this.errorCode = errorCode;
    }

    public static AccountScreenState loading() { return new AccountScreenState(Status.LOADING, null); }
    public static AccountScreenState content() { return new AccountScreenState(Status.CONTENT, null); }
    public static AccountScreenState empty() { return new AccountScreenState(Status.EMPTY, null); }
    public static AccountScreenState authRequired() { return new AccountScreenState(Status.AUTH_REQUIRED, null); }
    public static AccountScreenState error(@NonNull String code) { return new AccountScreenState(Status.ERROR, code); }

    @NonNull public Status getStatus() { return status; }
    @Nullable public String getErrorCode() { return errorCode; }
}
