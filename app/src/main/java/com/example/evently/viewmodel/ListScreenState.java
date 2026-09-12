package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.evently.data.model.Event;
import java.util.Collections;
import java.util.List;

/** Immutable state contract shared by event-list screens. */
public final class ListScreenState {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR, GUEST }
    @NonNull private final Status status;
    @NonNull private final List<Event> events;
    @Nullable private final String errorCode;
    private ListScreenState(@NonNull Status status, @NonNull List<Event> events, @Nullable String errorCode) {
        this.status = status; this.events = Collections.unmodifiableList(events); this.errorCode = errorCode;
    }
    public static ListScreenState loading() { return new ListScreenState(Status.LOADING, Collections.emptyList(), null); }
    public static ListScreenState guest() { return new ListScreenState(Status.GUEST, Collections.emptyList(), null); }
    public static ListScreenState error(@Nullable String code) { return new ListScreenState(Status.ERROR, Collections.emptyList(), code); }
    public static ListScreenState data(@NonNull List<Event> events) {
        return new ListScreenState(events.isEmpty() ? Status.EMPTY : Status.CONTENT, events, null);
    }
    @NonNull public Status getStatus() { return status; }
    @NonNull public List<Event> getEvents() { return events; }
    @Nullable public String getErrorCode() { return errorCode; }
}
