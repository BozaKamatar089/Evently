package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.domain.user.UserRepository;
import com.example.evently.domain.verification.VerificationRepository;
import com.example.evently.domain.session.SessionRepository;

public class ProfileViewModelFactory implements ViewModelProvider.Factory {

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final SessionRepository sessionRepository;

    public ProfileViewModelFactory(@NonNull UserRepository userRepository,
                                   @NonNull VerificationRepository verificationRepository,
                                   @NonNull SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.sessionRepository = sessionRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(userRepository, verificationRepository, sessionRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
