package com.example.evently.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Mapira Firebase Auth greške na stabilne string kodove koje UI prikazuje
 * preko lokaliziranih resursa (ErrorMapper).
 */
public final class AuthErrorCodes {

    private AuthErrorCodes() {
    }

    /**
     * Vraća stabilni kod greške za Firebase Auth izuzetak.
     * UI zatim koristi ErrorMapper za pripisivanje lokaliziranog stringa.
     *
     * @param e Firebase Auth izuzetak (može biti null)
     * @return stabilni kod greške (npr. "AUTH_WRONG_PASSWORD") ili AUTH_GENERIC
     */
    @NonNull
    public static String from(@Nullable Exception e) {
        if (e == null) {
            return "AUTH_GENERIC";
        }

        // FirebaseAuthException specifics
        if (e instanceof FirebaseAuthException) {
            FirebaseAuthException authEx = (FirebaseAuthException) e;
            String code = authEx.getErrorCode();
            if (code != null) {
                if ("ERROR_INVALID_LOGIN_CREDENTIALS".equals(code)
                        || "ERROR_INVALID_CREDENTIAL".equals(code)) return "AUTH_INVALID_CREDENTIAL";
                // Format: "ERROR_USER_NOT_FOUND" -> "AUTH_USER_NOT_FOUND"
                return "AUTH_" + code.replace("ERROR_", "").toUpperCase(java.util.Locale.ROOT);
            }
        }

        // Common subclasses
        if (e instanceof FirebaseAuthInvalidUserException) {
            return "AUTH_USER_NOT_FOUND";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "AUTH_WRONG_PASSWORD";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "AUTH_EMAIL_ALREADY_IN_USE";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "AUTH_WEAK_PASSWORD";
        }

        // Network/generic
        String msg = e.getMessage();
        if (msg != null) {
            String m = msg.toLowerCase(java.util.Locale.ROOT);
            if (m.contains("network") || m.contains("connection") || m.contains("unavailable")) {
                return "AUTH_NETWORK";
            }
            if (m.contains("too many") || m.contains("quota")) {
                return "AUTH_TOO_MANY_REQUESTS";
            }
            if (m.contains("invalid") || m.contains("credential")) {
                return "AUTH_INVALID_CREDENTIAL";
            }
        }

        return "AUTH_GENERIC";
    }
}
