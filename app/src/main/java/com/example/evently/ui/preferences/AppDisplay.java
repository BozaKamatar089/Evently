package com.example.evently.ui.preferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.evently.domain.preferences.AppPreferencesRepository;

/** Applies persisted display choices through AndroidX's supported runtime APIs. */
public final class AppDisplay {
    private AppDisplay() { }

    public static void applyLanguage(@NonNull AppPreferencesRepository.Language language) {
        LocaleListCompat locales = LocaleListCompat.forLanguageTags(language.languageTag());
        if (!AppCompatDelegate.getApplicationLocales().toLanguageTags().equals(locales.toLanguageTags())) {
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }

    public static void applyTheme(@NonNull AppPreferencesRepository.Theme theme) {
        int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (theme == AppPreferencesRepository.Theme.LIGHT) mode = AppCompatDelegate.MODE_NIGHT_NO;
        if (theme == AppPreferencesRepository.Theme.DARK) mode = AppCompatDelegate.MODE_NIGHT_YES;
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }
}
