package com.example.evently.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.User;
import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.auth.AuthIdentity;
import com.example.evently.domain.user.UserRepository;
import com.example.evently.util.AuthErrorCodes;

/**
 * ViewModel za autentifikaciju. Nakon uspješnog logina/registracije
 * osigurava da {@code Users/{uid}} dokument postoji preko {@link UserRepository}.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<AuthIdentity> user = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Void> resetSent = new MutableLiveData<>();

    public AuthViewModel(@NonNull AuthRepository authRepository,
                         @NonNull UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<AuthIdentity> getUser() {
        return user;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Void> getResetSent() {
        return resetSent;
    }

    public void login(String email, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        clearError();
        authRepository.login(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ensureUserDoc(task.getResult(), null);
                    } else {
                        handleFail(task.getException());
                    }
                });
    }

    public void register(String firstName, String lastName, String email, String password) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        clearError();
        authRepository.register(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String fullName = (firstName + " " + lastName).trim();
                        ensureUserDoc(task.getResult(), fullName);
                    } else {
                        error.setValue(AuthErrorCodes.from(task.getException()));
                        loading.setValue(false);
                    }
                });
    }

    public void loginWithGoogle(String idToken, String displayName) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        clearError();
        authRepository.loginWithGoogle(idToken)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ensureUserDoc(task.getResult(), displayName);
                    } else {
                        handleFail(task.getException());
                    }
                });
    }

    public void resetPassword(String email) {
        authRepository.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        resetSent.setValue(null);
                    } else {
                        error.setValue(AuthErrorCodes.from(task.getException()));
                    }
                });
    }

    private void ensureUserDoc(AuthIdentity identity, String fallbackName) {
        User user = new User(identity.getUid());
        user.setName(fallbackName != null
                ? fallbackName
                : identity.getDisplayName());
        user.setEmail(identity.getEmail());
        user.setPhotoUrl(identity.getPhotoUrl());
        user.setPhone(identity.getPhoneNumber());
        user.setRole(User.ROLE_PARTICIPANT);
        user.setMemberSince(System.currentTimeMillis());

        userRepository.ensureUserCreated(user)
                .addOnCompleteListener(task -> {
                    loading.setValue(false);
                    if (task.isSuccessful()) {
                        this.user.setValue(identity);
                    } else {
                        handleFail(task.getException());
                    }
                });
    }

    private void handleFail(Exception e) {
        error.setValue(AuthErrorCodes.from(e));
        loading.setValue(false);
    }

    private void clearError() {
        error.setValue(null);
    }

    public void clearErrorState() {
        clearError();
    }

    public AuthIdentity getCurrentUser() {
        return authRepository.getCurrentUser();
    }
}
