package com.example.evently.util;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.example.evently.ui.auth.LoginActivity;

/** Single entry point for guest-only actions. */
public final class AuthGate {
    private AuthGate() { }
    public static void requireSignIn(@NonNull Activity activity,
                                     @NonNull PendingActionManager.ActionType action,
                                     String eventId) {
        new PendingActionManager(activity).setPendingAction(action, eventId);
        activity.startActivity(new Intent(activity, LoginActivity.class));
    }
}
