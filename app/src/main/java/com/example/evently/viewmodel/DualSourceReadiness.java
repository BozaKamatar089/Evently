package com.example.evently.viewmodel;

/** Pure readiness primitive: joined lists render only after both current sources respond. */
final class DualSourceReadiness {
    private boolean first;
    private boolean second;
    void reset() { first = false; second = false; }
    void firstReady() { first = true; }
    void secondReady() { second = true; }
    boolean isReady() { return first && second; }
}
