package com.example.evently.domain.common;

/** A lifecycle-owned, cancellable observation. */
public interface Subscription {
    void cancel();
}
