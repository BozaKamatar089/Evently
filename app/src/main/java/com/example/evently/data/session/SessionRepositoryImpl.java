package com.example.evently.data.session;

import androidx.annotation.NonNull;

import com.example.evently.data.model.User;
import com.example.evently.domain.auth.AuthIdentity;
import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.common.Subscription;
import com.example.evently.domain.session.SessionRepository;
import com.example.evently.domain.session.SessionState;
import com.example.evently.domain.user.UserRepository;
import com.google.firebase.firestore.ListenerRegistration;

/** Firebase orchestration stays in data; missing canonical profiles are repaired once per session. */
public final class SessionRepositoryImpl implements SessionRepository {
    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    public SessionRepositoryImpl(@NonNull AuthRepository authRepository, @NonNull UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
    }

    @NonNull @Override public Subscription observe(@NonNull Listener listener) {
        final ListenerRegistration[] profileListener = new ListenerRegistration[1];
        final boolean[] repairInFlight = new boolean[1];
        final long[] generation = new long[1];
        final boolean[] cancelled = new boolean[1];
        Subscription authSubscription = authRepository.observeAuth(identity -> {
            if (cancelled[0]) return;
            final long currentGeneration = ++generation[0];
            if (profileListener[0] != null) { profileListener[0].remove(); profileListener[0] = null; }
            repairInFlight[0] = false;
            if (identity == null) { listener.onSessionChanged(SessionState.guest()); return; }
            listener.onSessionChanged(SessionState.loading(identity.getUid()));
            profileListener[0] = userRepository.listenUser(identity.getUid(), (profile, error) -> {
                if (cancelled[0] || currentGeneration != generation[0]) return;
                if (error != null) { listener.onSessionChanged(SessionState.recoveryFailed(identity.getUid(), "PROFILE_LOAD_ERROR")); return; }
                if (profile != null) { listener.onSessionChanged(SessionState.authenticated(profile)); return; }
                if (repairInFlight[0]) return;
                repairInFlight[0] = true;
                userRepository.ensureUserCreated(canonicalUser(identity))
                        .addOnFailureListener(e -> {
                            if (!cancelled[0] && currentGeneration == generation[0]) {
                                listener.onSessionChanged(SessionState.recoveryFailed(
                                        identity.getUid(), "PROFILE_BOOTSTRAP_FAILED"));
                            }
                        });
            });
        }, () -> {
            if (profileListener[0] != null) { profileListener[0].remove(); profileListener[0] = null; }
        });
        return () -> {
            cancelled[0] = true;
            generation[0]++;
            authSubscription.cancel();
            if (profileListener[0] != null) { profileListener[0].remove(); profileListener[0] = null; }
        };
    }

    private static User canonicalUser(@NonNull AuthIdentity identity) {
        User user = new User(identity.getUid());
        user.setName(identity.getDisplayName());
        user.setEmail(identity.getEmail());
        user.setPhotoUrl(identity.getPhotoUrl());
        user.setPhone(identity.getPhoneNumber());
        user.setRole(User.ROLE_PARTICIPANT);
        user.setVerified(false);
        user.setVerificationStatus("none");
        user.setMemberSince(System.currentTimeMillis());
        return user;
    }

    @Override public void logout() { authRepository.logout(); }
}
