package com.example.evently.domain.auth;

import com.google.firebase.auth.FirebaseUser;

public interface AuthRepository {
    interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(Exception e);
    }

    void login(String email, String password, AuthCallback callback);
    void register(String email, String password, AuthCallback callback);
    void loginWithGoogle(String idToken, AuthCallback callback);
    FirebaseUser getCurrentUser();
    void logout();
}