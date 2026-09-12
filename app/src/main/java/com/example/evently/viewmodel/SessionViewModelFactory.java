package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.domain.session.SessionRepository;

public final class SessionViewModelFactory implements ViewModelProvider.Factory {
    private final SessionRepository repository;
    public SessionViewModelFactory(@NonNull SessionRepository repository) { this.repository = repository; }
    @NonNull @Override @SuppressWarnings("unchecked") public <T extends ViewModel> T create(@NonNull Class<T> type) {
        if (type.isAssignableFrom(SessionViewModel.class)) return (T) new SessionViewModel(repository);
        throw new IllegalArgumentException("Unknown ViewModel: " + type.getName());
    }
}
