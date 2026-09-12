package com.example.evently.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.evently.R;

/**
 * Mapira greške (Firebase kôdove, stabilne kôdove iz repozitorija) na
 * lokalizirane string resurse. UI sloj NIKADA ne prikazuje sirove Firebase
 * poruke korisniku — svaka greška prolazi kroz ovaj mapper.
 */
public final class ErrorMapper {

    private ErrorMapper() {
    }

    /**
     * Vraća resurs ID za datu grešku. Ako se greška ne prepozna, vraća
     * generički error_generic. Ako poruka sadrži poznati stabilni kôd,
     * mapira se na odgovarajuću poruku.
     *
     * @param message poruka greške (stabilni kôd ili Firebase poruka)
     * @return string resurs ID
     */
    @StringRes
    public static int toResId(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return R.string.error_generic;
        }
        String m = message.toUpperCase(java.util.Locale.ROOT);

        if (m.contains("AUTH_USER_NOT_FOUND")) return R.string.auth_error_user_not_found;
        if (m.contains("AUTH_WRONG_PASSWORD")) return R.string.auth_error_wrong_password;
        if (m.contains("AUTH_EMAIL_ALREADY_IN_USE")) return R.string.auth_error_email_already_in_use;
        if (m.contains("AUTH_WEAK_PASSWORD")) return R.string.auth_error_weak_password;
        if (m.contains("AUTH_INVALID_CREDENTIAL")) return R.string.auth_error_invalid_credential;
        if (m.contains("AUTH_TOO_MANY_REQUESTS")) return R.string.auth_error_too_many_requests;
        if (m.contains("AUTH_PASSWORD_PROVIDER_REQUIRED")) return R.string.password_provider_required;
        if (m.contains("AUTH_REQUIRED")
                || m.contains("NOT_LOGGED_IN")
                || m.contains("KORISNIK NIJE PRIJAVLJEN")) {
            return R.string.error_auth_required;
        }
        if (m.contains("ALREADY_REGISTERED")) {
            return R.string.registration_error_already;
        }
        if (m.contains("CAPACITY_FULL")) {
            return R.string.registration_error_full;
        }
        if (m.contains("REGISTRATIONS_CLOSED")) {
            return R.string.registration_error_closed;
        }
        if (m.contains("EVENT_NOT_FOUND")) {
            return R.string.event_not_found;
        }
        if (m.contains("EVENT_PAST") || m.contains("EVENT_NOT_ACTIVE")
                || m.contains("EVENT_HAS_REGISTRATIONS")) {
            return R.string.registration_error_closed;
        }
        if (m.contains("EVENT_CASCADE_TOO_LARGE") || m.contains("EVENT_DELETE_FAILED")) {
            return R.string.event_delete_error;
        }
        if (m.contains("ORGANIZER_REMOVAL_REQUIRES_BACKEND")) {
            return R.string.registration_error_generic;
        }
        if (m.contains("NOT_REGISTERED")) {
            return R.string.registration_error_generic;
        }
        if (m.contains("ADMIN_LOAD_ERROR")) {
            return R.string.admin_load_error;
        }
        if (m.contains("ADMIN_ACTION_FAILED")) {
            return R.string.admin_action_failed;
        }
        if (m.contains("PROFILE_LOAD_ERROR")) {
            return R.string.profile_load_error;
        }
        if (m.contains("PROFILE_SAVE_ERROR")) {
            return R.string.profile_save_error;
        }
        if (m.contains("AUTH_NETWORK")
                || m.contains("UNAVAILABLE")
                || m.contains("UNAUTHENTICATED")
                || m.contains("NETWORK")
                || m.contains("PERMISSION_DENIED")
                || m.contains("OFFLINE")) {
            return R.string.error_network;
        }
        return R.string.error_generic;
    }

    /**
     * Mapira grešku na lokalizirani string.
     *
     * @param context Android kontekst
     * @param message poruka greške
     * @return lokalizirani tekst greške
     */
    @NonNull
    public static String toString(android.content.Context context, @Nullable String message) {
        return context.getString(toResId(message));
    }

    /**
     * Mapira Throwable (npr. FirebaseFirestoreException) na lokalizirani string.
     *
     * @param context Android kontekst
     * @param throwable izuzetak
     * @return lokalizirani tekst greške
     */
    @NonNull
    public static String toString(android.content.Context context, @Nullable Throwable throwable) {
        if (throwable == null) {
            return context.getString(R.string.error_generic);
        }
        return toString(context, throwable.getMessage());
    }
}
