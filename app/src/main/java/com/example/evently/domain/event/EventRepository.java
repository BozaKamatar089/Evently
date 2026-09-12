package com.example.evently.domain.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

/**
 * Repository za događaje. Sva Events komunikacija prolazi kroz ovaj repository.
 */
public interface EventRepository {

    ListenerRegistration listenAllEvents(@NonNull OnEventsListener listener);

    Task<List<Event>> getEvents();

    Task<Event> getEvent(@NonNull String eventId);

    /** Real-time listener za pojedinačni događaj. */
    ListenerRegistration listenEvent(@NonNull String eventId, @NonNull OnEventListener listener);

    Task<String> createEvent(@NonNull Event event);

    /** Ažurira samo editabilna polja (preko {@link Event#toUpdateMap()}). */
    Task<Void> updateEvent(@NonNull String eventId, @NonNull Event event);

    /** Brisanje eventa sa kaskadom na Registrations, Favorites, Notifications. */
    Task<Void> deleteEvent(@NonNull String eventId);

    interface OnEventsListener {
        void onEvents(@Nullable List<Event> events, @Nullable Exception error);
    }

    interface OnEventListener {
        void onEvent(@Nullable Event event, @Nullable Exception error);
    }
}
