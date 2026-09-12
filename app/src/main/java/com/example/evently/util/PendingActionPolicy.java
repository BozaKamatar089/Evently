package com.example.evently.util;

/** Pure policy used by persistence and unit tests for exactly-once replay. */
public final class PendingActionPolicy {
    private PendingActionPolicy() { }
    public static boolean canBegin(PendingActionManager.ActionType action, boolean consuming) {
        return action != PendingActionManager.ActionType.NONE && !consuming;
    }
    public static boolean clearsAfter(boolean replaySucceeded) { return replaySucceeded; }
}
