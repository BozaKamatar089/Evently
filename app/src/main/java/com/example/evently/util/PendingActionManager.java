package com.example.evently.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Upravlja "pending action" za gost koji pokušava zaštićenu akciju.
 * Pohranjuje akciju prije logina, replay-uje nakon uspješnog logina.
 */
public class PendingActionManager {

    private static final String PREFS_NAME = "evently_pending_action";
    private static final String KEY_ACTION_TYPE = "action_type";
    private static final String KEY_EVENT_ID = "event_id";
    private static final String KEY_CONSUMING = "consuming";

    public enum ActionType {
        NONE,
        FAVORITE_TOGGLE,
        PARTICIPANT_REGISTER,
        VOLUNTEER_REGISTER
    }

    private final SharedPreferences prefs;

    public PendingActionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setPendingAction(ActionType type, @Nullable String eventId) {
        prefs.edit()
                .putString(KEY_ACTION_TYPE, type.name())
                .putString(KEY_EVENT_ID, eventId != null ? eventId : "")
                .putBoolean(KEY_CONSUMING, false)
                .apply();
    }

    @NonNull
    public ActionType getPendingActionType() {
        String typeStr = prefs.getString(KEY_ACTION_TYPE, ActionType.NONE.name());
        try {
            return ActionType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return ActionType.NONE;
        }
    }

    @Nullable
    public String getPendingEventId() {
        String eventId = prefs.getString(KEY_EVENT_ID, "");
        return eventId.isEmpty() ? null : eventId;
    }

    public void clearPendingAction() {
        prefs.edit().remove(KEY_ACTION_TYPE).remove(KEY_EVENT_ID).remove(KEY_CONSUMING).apply();
    }

    public boolean hasPendingAction() {
        return getPendingActionType() != ActionType.NONE;
    }

    /** Atomically marks an action as replaying. A duplicate completion cannot execute it twice. */
    public boolean beginConsumption() {
        if (!PendingActionPolicy.canBegin(getPendingActionType(), prefs.getBoolean(KEY_CONSUMING, false))) return false;
        prefs.edit().putBoolean(KEY_CONSUMING, true).commit();
        return true;
    }

    /** Successful replay removes the action; failures make one explicit retry possible. */
    public void finishConsumption(boolean success) {
        if (PendingActionPolicy.clearsAfter(success)) clearPendingAction();
        else prefs.edit().putBoolean(KEY_CONSUMING, false).apply();
    }
}
