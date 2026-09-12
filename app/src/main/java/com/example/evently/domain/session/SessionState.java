package com.example.evently.domain.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.User;

/** Immutable application session state. UI never needs Firebase SDK state. */
public final class SessionState {
    public enum Status { GUEST, LOADING_PROFILE, AUTHENTICATED, PROFILE_RECOVERY_FAILED }

    @NonNull private final Status status;
    @Nullable private final String uid;
    @Nullable private final User user;
    @Nullable private final String errorCode;

    private SessionState(@NonNull Status status, @Nullable String uid, @Nullable User user,
                         @Nullable String errorCode) {
        this.status = status;
        this.uid = uid;
        this.user = user;
        this.errorCode = errorCode;
    }

    @NonNull public static SessionState guest() { return new SessionState(Status.GUEST, null, null, null); }
    @NonNull public static SessionState loading(@NonNull String uid) { return new SessionState(Status.LOADING_PROFILE, uid, null, null); }
    @NonNull public static SessionState authenticated(@NonNull User user) {
        return new SessionState(Status.AUTHENTICATED, user.getUid(), user, null);
    }
    @NonNull public static SessionState recoveryFailed(@NonNull String uid, @NonNull String errorCode) {
        return new SessionState(Status.PROFILE_RECOVERY_FAILED, uid, null, errorCode);
    }
    @NonNull public Status getStatus() { return status; }
    @Nullable public String getUid() { return uid; }
    @Nullable public User getUser() { return user; }
    @Nullable public String getErrorCode() { return errorCode; }
    public boolean isAuthenticated() { return status == Status.AUTHENTICATED && user != null; }
    public boolean isGuest() { return status == Status.GUEST; }
    public boolean isAdmin(@NonNull String adminUid) { return isAuthenticated() && adminUid.equals(uid); }
}
