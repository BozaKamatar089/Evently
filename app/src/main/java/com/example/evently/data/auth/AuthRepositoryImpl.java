package com.example.evently.data.auth;

import androidx.annotation.NonNull;

import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.auth.AuthIdentity;
import com.example.evently.domain.common.Subscription;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.UserInfo;

public class AuthRepositoryImpl implements AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public AuthRepositoryImpl() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public Task<AuthIdentity> login(@NonNull String email, @NonNull String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return toIdentity(task.getResult().getUser());
                    }
                    throw task.getException();
                });
    }

    @Override
    public Task<AuthIdentity> register(@NonNull String email, @NonNull String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return toIdentity(task.getResult().getUser());
                    }
                    throw task.getException();
                });
    }

    @Override
    public Task<AuthIdentity> loginWithGoogle(@NonNull String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        return firebaseAuth.signInWithCredential(credential)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return toIdentity(task.getResult().getUser());
                    }
                    throw task.getException();
                });
    }

    @Override
    public Task<Void> sendPasswordResetEmail(@NonNull String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    @Override
    public Task<Void> changePassword(@NonNull String currentPassword,
                                     @NonNull String newPassword) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            return Tasks.forException(new IllegalStateException("AUTH_REQUIRED"));
        }
        boolean hasPassword = false;
        for (UserInfo provider : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(provider.getProviderId())) {
                hasPassword = true;
                break;
            }
        }
        if (!hasPassword) {
            return Tasks.forException(new IllegalStateException("AUTH_PASSWORD_PROVIDER_REQUIRED"));
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        return user.reauthenticate(credential).onSuccessTask(ignored -> user.updatePassword(newPassword));
    }

    @Override
    public AuthIdentity getCurrentUser() {
        return toIdentity(firebaseAuth.getCurrentUser());
    }

    @NonNull
    @Override
    public Subscription observeAuth(@NonNull AuthListener listener, @NonNull Runnable onCancelled) {
        FirebaseAuth.AuthStateListener authListener = auth -> listener.onAuthChanged(toIdentity(auth.getCurrentUser()));
        firebaseAuth.addAuthStateListener(authListener);
        return () -> {
            firebaseAuth.removeAuthStateListener(authListener);
            onCancelled.run();
        };
    }

    @Override
    public void logout() {
        firebaseAuth.signOut();
    }

    private static AuthIdentity toIdentity(FirebaseUser user) {
        if (user == null) return null;
        boolean passwordProvider = false;
        boolean googleProvider = false;
        for (UserInfo provider : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(provider.getProviderId())) passwordProvider = true;
            if (GoogleAuthProvider.PROVIDER_ID.equals(provider.getProviderId())) googleProvider = true;
        }
        return new AuthIdentity(user.getUid(), user.getDisplayName(), user.getEmail(),
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                user.getPhoneNumber(), passwordProvider, googleProvider);
    }
}
