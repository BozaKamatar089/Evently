package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.domain.auth.AuthRepository;

public final class AccountSecurityViewModelFactory implements ViewModelProvider.Factory {
    private final AuthRepository repository;
    public AccountSecurityViewModelFactory(AuthRepository repository) { this.repository = repository; }
    @NonNull @Override @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AccountSecurityViewModel.class)) {
            return (T) new AccountSecurityViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
