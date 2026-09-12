package com.example.evently.domain.admin;

import androidx.annotation.NonNull;

/** App-side capability for navigation/UX. Firestore rules remain the authority. */
public final class AdminCapability {
    private static final String ADMIN_UID = "60pGCwRrSwXwVQtqZzALPJzwBka2";
    private AdminCapability() { }
    @NonNull public static String configuredUid() { return ADMIN_UID; }
    public static boolean isConfiguredAdmin(String uid) { return ADMIN_UID.equals(uid); }
}
