package com.example.evently.domain.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Firebase-independent identity exposed to application layers. */
public final class AuthIdentity {
    private final String uid;
    @Nullable private final String displayName;
    @Nullable private final String email;
    @Nullable private final String photoUrl;
    @Nullable private final String phoneNumber;
    private final boolean passwordProvider;
    private final boolean googleProvider;

    public AuthIdentity(@NonNull String uid, @Nullable String displayName, @Nullable String email,
                        @Nullable String photoUrl, @Nullable String phoneNumber) {
        this(uid, displayName, email, photoUrl, phoneNumber, false, false);
    }

    public AuthIdentity(@NonNull String uid, @Nullable String displayName, @Nullable String email,
                        @Nullable String photoUrl, @Nullable String phoneNumber,
                        boolean passwordProvider, boolean googleProvider) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.phoneNumber = phoneNumber;
        this.passwordProvider = passwordProvider;
        this.googleProvider = googleProvider;
    }

    @NonNull public String getUid() { return uid; }
    @Nullable public String getDisplayName() { return displayName; }
    @Nullable public String getEmail() { return email; }
    @Nullable public String getPhotoUrl() { return photoUrl; }
    @Nullable public String getPhoneNumber() { return phoneNumber; }
    public boolean hasPasswordProvider() { return passwordProvider; }
    public boolean hasGoogleProvider() { return googleProvider; }
    public boolean hasMultipleProviders() { return passwordProvider && googleProvider; }
}
