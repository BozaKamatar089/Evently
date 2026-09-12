package com.example.evently.viewmodel;

import com.example.evently.data.model.User;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.domain.session.SessionState;

/** One UID-scoped snapshot decides all profile content and capability visibility. */
public final class ProfileScreenState {
    public enum Status { GUEST, AUTH_LOADING, AUTHENTICATED, ERROR }
    private final Status status;
    private final User user;
    private final VerificationRequest request;
    private final boolean admin;
    private final boolean missing;

    private ProfileScreenState(Status status, User user, VerificationRequest request,
                               boolean admin, boolean missing) {
        this.status = status; this.user = user; this.request = request;
        this.admin = admin; this.missing = missing;
    }

    public static ProfileScreenState resolve(SessionState session, String loadedUid,
            AccountScreenState account, User user, VerificationRequest request, String adminUid) {
        Status status = Status.AUTH_LOADING;
        boolean missing = false;
        if (session != null && session.isGuest()) status = Status.GUEST;
        else if (session != null && session.getStatus() == SessionState.Status.PROFILE_RECOVERY_FAILED)
            status = Status.ERROR;
        else if (session != null && session.isAuthenticated() && session.getUid().equals(loadedUid)) {
            if (account != null && (account.getStatus() == AccountScreenState.Status.ERROR
                    || account.getStatus() == AccountScreenState.Status.EMPTY)) {
                status = Status.ERROR;
                missing = account.getStatus() == AccountScreenState.Status.EMPTY;
            } else if (account != null && account.getStatus() == AccountScreenState.Status.CONTENT
                    && user != null && session.getUid().equals(user.getUid())) {
                status = Status.AUTHENTICATED;
            }
        }
        boolean content = status == Status.AUTHENTICATED;
        VerificationRequest scoped = content && request != null
                && session.getUid().equals(request.getUserId())
                && request.getStatus() != null
                && request.getStatus().equals(user.getVerificationStatus()) ? request : null;
        return new ProfileScreenState(status, content ? user : null, scoped,
                content && session.isAdmin(adminUid), missing);
    }
    public Status getStatus() { return status; }
    public User getUser() { return user; }
    public VerificationRequest getRequest() { return request; }
    public boolean isAdmin() { return admin; }
    public boolean isMissing() { return missing; }
}
