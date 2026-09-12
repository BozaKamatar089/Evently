package com.example.evently.ui.activities;

/** Pure state holder which prevents realtime snapshots from replacing an active draft. */
final class EditProfileDraftGuard {
    private String uid;
    private boolean initialized;
    private boolean dirty;

    boolean shouldApplySnapshot(String nextUid) {
        if (nextUid == null || nextUid.isEmpty()) return false;
        if (!nextUid.equals(uid)) {
            uid = nextUid;
            initialized = false;
            dirty = false;
        }
        if (!initialized || !dirty) {
            initialized = true;
            return true;
        }
        return false;
    }

    void markDirty() { if (initialized) dirty = true; }
    void markSaved() { dirty = false; }
    void clear() { uid = null; initialized = false; dirty = false; }
    void restore(String restoredUid) {
        uid = restoredUid;
        initialized = restoredUid != null && !restoredUid.isEmpty();
        dirty = initialized;
    }
    boolean isDirty() { return dirty; }
}
