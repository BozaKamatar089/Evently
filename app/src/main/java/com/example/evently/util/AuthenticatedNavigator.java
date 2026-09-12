package com.example.evently.util;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.example.evently.data.favorites.FavoriteRepositoryImpl;
import com.example.evently.data.model.Registration;
import com.example.evently.ui.activities.EventDetailsActivity;
import com.example.evently.ui.activities.HomeActivity;

/** Consumes one pending action only after SessionState has a canonical profile. */
public final class AuthenticatedNavigator {
    private AuthenticatedNavigator() { }
    public static void continueAfterProfileReady(@NonNull Activity activity) {
        PendingActionManager pending = new PendingActionManager(activity);
        if (!pending.beginConsumption()) { openHome(activity); return; }
        String eventId = pending.getPendingEventId();
        switch (pending.getPendingActionType()) {
            case FAVORITE_TOGGLE:
                if (eventId == null) { pending.finishConsumption(false); openHome(activity); return; }
                new FavoriteRepositoryImpl().addFavorite(eventId)
                        .addOnSuccessListener(v -> { pending.finishConsumption(true); openHome(activity); })
                        .addOnFailureListener(e -> { pending.finishConsumption(false); openHome(activity); });
                return;
            case PARTICIPANT_REGISTER:
            case VOLUNTEER_REGISTER:
                if (eventId != null) {
                    Intent intent = EventDetailsActivity.createIntent(activity, eventId)
                            .putExtra("AUTO_REGISTER", true)
                            .putExtra("AUTO_REGISTER_TYPE", pending.getPendingActionType()
                                    == PendingActionManager.ActionType.PARTICIPANT_REGISTER
                                    ? Registration.TYPE_PARTICIPANT : Registration.TYPE_VOLUNTEER)
                            .putExtra("PENDING_ACTION_REPLAY", true);
                    activity.startActivity(intent);
                    activity.finish();
                    return;
                }
                pending.finishConsumption(false);
                openHome(activity);
                return;
            default:
                pending.finishConsumption(true);
                openHome(activity);
        }
    }
    private static void openHome(Activity activity) {
        activity.startActivity(new Intent(activity, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        activity.finish();
    }
}
