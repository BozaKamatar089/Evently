package com.example.evently.domain.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.domain.common.Subscription;
import com.google.android.gms.tasks.Task;

/**
 * Apstrakcija nad Firebase Authentication.
 */
public interface AuthRepository {

    Task<AuthIdentity> login(@NonNull String email, @NonNull String password);

    Task<AuthIdentity> register(@NonNull String email, @NonNull String password);

    Task<AuthIdentity> loginWithGoogle(@NonNull String idToken);

    Task<Void> sendPasswordResetEmail(@NonNull String email);

    /** Reauthenticates a password account and changes its password atomically from the UI's perspective. */
    Task<Void> changePassword(@NonNull String currentPassword, @NonNull String newPassword);

    @Nullable AuthIdentity getCurrentUser();

    /** Observes Auth without leaking FirebaseAuth/AuthStateListener to callers. */
    @NonNull Subscription observeAuth(@NonNull AuthListener listener, @NonNull Runnable onCancelled);

    interface AuthListener { void onAuthChanged(@Nullable AuthIdentity identity); }

    void logout();
}
