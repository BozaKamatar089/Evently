package com.example.evently.domain.preferences;

import androidx.annotation.NonNull;

/** Persistent, device-local display preferences. */
public interface AppPreferencesRepository {
    enum Language {
        BOSNIAN("bs"), ENGLISH("en");
        private final String languageTag;
        Language(String languageTag) { this.languageTag = languageTag; }
        @NonNull public String languageTag() { return languageTag; }
        @NonNull public static Language fromTag(String value) {
            return ENGLISH.languageTag.equalsIgnoreCase(value) ? ENGLISH : BOSNIAN;
        }
    }

    enum Theme { SYSTEM, LIGHT, DARK }

    @NonNull Language getLanguage();
    void setLanguage(@NonNull Language language);
    @NonNull Theme getTheme();
    void setTheme(@NonNull Theme theme);
}
