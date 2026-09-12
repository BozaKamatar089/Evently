package com.example.evently.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.user.UserRepository;

public class AuthViewModelFactory implements ViewModelProvider.Factory {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    public AuthViewModelFactory(@NonNull AuthRepository authRepository,
                                @NonNull UserRepository userRepository) {
        this.authRepository = authRepository;
        this.userRepository = userRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(authRepository, userRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
