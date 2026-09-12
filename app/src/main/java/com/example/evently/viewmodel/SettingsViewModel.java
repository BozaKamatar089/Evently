package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.domain.auth.AuthIdentity;
import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.preferences.AppPreferencesRepository;

/** Account capability and persistent display-preference state for Settings. */
public final class SettingsViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final AppPreferencesRepository preferences;
    private final MutableLiveData<AuthIdentity> identity = new MutableLiveData<>();
    private final MutableLiveData<AppPreferencesRepository.Language> language = new MutableLiveData<>();
    private final MutableLiveData<AppPreferencesRepository.Theme> theme = new MutableLiveData<>();

    public SettingsViewModel(@NonNull AuthRepository authRepository,
                             @NonNull AppPreferencesRepository preferences) {
        this.authRepository = authRepository;
        this.preferences = preferences;
        refresh();
    }

    public void refresh() {
        identity.setValue(authRepository.getCurrentUser());
        language.setValue(preferences.getLanguage());
        theme.setValue(preferences.getTheme());
    }

    public void selectLanguage(@NonNull AppPreferencesRepository.Language value) {
        preferences.setLanguage(value);
        language.setValue(value);
    }

    public void selectTheme(@NonNull AppPreferencesRepository.Theme value) {
        preferences.setTheme(value);
        theme.setValue(value);
    }

    public LiveData<AuthIdentity> getIdentity() { return identity; }
    public LiveData<AppPreferencesRepository.Language> getLanguage() { return language; }
    public LiveData<AppPreferencesRepository.Theme> getTheme() { return theme; }
}
