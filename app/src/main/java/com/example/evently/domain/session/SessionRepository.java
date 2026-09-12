package com.example.evently.domain.session;

import androidx.annotation.NonNull;

import com.example.evently.domain.common.Subscription;

/** Owns authentication/profile bootstrap and exposes one application session stream. */
public interface SessionRepository {
    @NonNull Subscription observe(@NonNull Listener listener);
    void logout();
    interface Listener { void onSessionChanged(@NonNull SessionState state); }
}
