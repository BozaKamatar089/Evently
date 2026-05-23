package com.example.evently.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.example.evently.domain.auth.AuthRepository;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<FirebaseUser> getUser() {
        return user;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void login(String email, String password) {
        loading.setValue(true);
        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                loading.setValue(false);
                user.setValue(firebaseUser);
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    public void register(String email, String password) {
        loading.setValue(true);
        authRepository.register(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                loading.setValue(false);
                user.setValue(firebaseUser);
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    public void loginWithGoogle(String idToken) {
        loading.setValue(true);
        authRepository.loginWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                loading.setValue(false);
                user.setValue(firebaseUser);
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public void logout() {
        authRepository.logout();
        user.setValue(null);
    }
}