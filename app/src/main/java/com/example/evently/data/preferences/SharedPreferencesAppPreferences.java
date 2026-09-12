package com.example.evently.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.evently.domain.preferences.AppPreferencesRepository;

/** SharedPreferences implementation; no account or Firebase data is stored here. */
public final class SharedPreferencesAppPreferences implements AppPreferencesRepository {
    private static final String FILE = "evently_display_preferences";
    private static final String LANGUAGE = "language";
    private static final String THEME = "theme";
    private final SharedPreferences preferences;

    public SharedPreferencesAppPreferences(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @NonNull @Override public Language getLanguage() {
        return Language.fromTag(preferences.getString(LANGUAGE, Language.BOSNIAN.languageTag()));
    }

    @Override public void setLanguage(@NonNull Language language) {
        preferences.edit().putString(LANGUAGE, language.languageTag()).apply();
    }

    @NonNull @Override public Theme getTheme() {
        String value = preferences.getString(THEME, Theme.SYSTEM.name());
        try { return Theme.valueOf(value); }
        catch (IllegalArgumentException ignored) { return Theme.SYSTEM; }
    }

    @Override public void setTheme(@NonNull Theme theme) {
        preferences.edit().putString(THEME, theme.name()).apply();
    }
}
