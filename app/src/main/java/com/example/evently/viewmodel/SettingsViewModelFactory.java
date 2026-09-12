package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.preferences.AppPreferencesRepository;

public final class SettingsViewModelFactory implements ViewModelProvider.Factory {
    private final AuthRepository auth;
    private final AppPreferencesRepository preferences;
    public SettingsViewModelFactory(AuthRepository auth, AppPreferencesRepository preferences) {
        this.auth = auth; this.preferences = preferences;
    }
    @NonNull @Override @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(auth, preferences);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
    }
}
