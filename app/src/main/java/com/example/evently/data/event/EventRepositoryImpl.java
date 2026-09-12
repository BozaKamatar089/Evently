package com.example.evently.data.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.Event;
import com.example.evently.domain.event.EventRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore implementacija {@link EventRepository}.
 * Counteri (currentParticipants/currentVolunteers) se mijenjaju isključivo
 * transakcijama koje kreiraju drugi repository slojevi (FAZA 3).
 */
public class EventRepositoryImpl implements EventRepository {

    private static final String COLLECTION = "Events";
    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_REGISTRATIONS = "Registrations";
    private static final String COLLECTION_FAVORITES = "Favorites";
    private static final String COLLECTION_NOTIFICATIONS = "Notifications";

    private final FirebaseFirestore db;

    public EventRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public ListenerRegistration listenAllEvents(@NonNull OnEventsListener listener) {
        return db.collection(COLLECTION)
                .orderBy("dateLong", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        listener.onEvents(null, error);
                        return;
                    }
                    if (querySnapshot != null) {
                        listener.onEvents(toEvents(querySnapshot), null);
                    }
                });
    }

    @Override
    public Task<List<Event>> getEvents() {
        return db.collection(COLLECTION)
                .orderBy("dateLong", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> toEvents(task.getResult()));
    }

    @Override
    public Task<Event> getEvent(@NonNull String eventId) {
        return db.collection(COLLECTION).document(eventId).get()
                .continueWith(task -> {
                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot == null || !snapshot.exists()) {
                        return null;
                    }
                    return fromSnapshot(snapshot);
                });
    }

    @Override
    public ListenerRegistration listenEvent(@NonNull String eventId,
                                            @NonNull OnEventListener listener) {
        return db.collection(COLLECTION).document(eventId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onEvent(null, error);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        listener.onEvent(fromSnapshot(snapshot), null);
                    } else {
                        listener.onEvent(null, null);
                    }
                });
    }

    @Override
    public Task<String> createEvent(@NonNull Event event) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.getUid().equals(event.getOrganizerId())) {
            return Tasks.forException(new IllegalStateException("AUTH_REQUIRED"));
        }

        return db.collection(COLLECTION_USERS).document(currentUser.getUid()).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        return Tasks.forException(new IllegalStateException("ORGANIZER_NOT_VERIFIED"));
                    }
                    Boolean verified = task.getResult().getBoolean("isVerified");
                    String role = task.getResult().getString("role");
                    if (!Boolean.TRUE.equals(verified) || !"Organizer".equals(role)) {
                        return Tasks.forException(new IllegalStateException("ORGANIZER_NOT_VERIFIED"));
                    }

                    Map<String, Object> createMap = new HashMap<>(event.toCreateMap());
                    createMap.put("organizerVerified", true);
                    return db.collection(COLLECTION).add(createMap)
                            .continueWith(createTask -> {
                                if (createTask.isSuccessful() && createTask.getResult() != null) {
                                    return createTask.getResult().getId();
                                }
                                throw createTask.getException();
                            });
                });
    }

    @Override
    public Task<Void> updateEvent(@NonNull String eventId, @NonNull Event event) {
        return db.collection(COLLECTION).document(eventId)
                .update(event.toUpdateMap());
    }

    @Override
    public Task<Void> deleteEvent(@NonNull String eventId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid().isEmpty()) {
            return Tasks.forException(new IllegalStateException("AUTH_REQUIRED"));
        }
        String uid = user.getUid();

        Task<QuerySnapshot> registrations = db.collection(COLLECTION_REGISTRATIONS)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("organizerId", uid)
                .get();
        Task<QuerySnapshot> favorites = db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("eventId", eventId)
                .get();
        Task<QuerySnapshot> notifications = db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("eventId", eventId)
                .get();

        // A Firestore batch is all-or-nothing. If a write is denied or a new
        // registration wins the race, the event is not retired and no child
        // collection is partially cleaned up.
        return Tasks.whenAll(registrations, favorites, notifications)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        return Tasks.forException(exception != null
                                ? exception
                                : new IllegalStateException("EVENT_DELETE_FAILED"));
                    }
                    if (registrations.getResult() != null
                            && !registrations.getResult().isEmpty()) {
                        return Tasks.forException(
                                new IllegalStateException("EVENT_HAS_REGISTRATIONS"));
                    }

                    int childWriteCount = snapshotSize(favorites.getResult())
                            + snapshotSize(notifications.getResult());
                    // One write is reserved for the Events status update.
                    if (childWriteCount > 499) {
                        return Tasks.forException(
                                new IllegalStateException("EVENT_CASCADE_TOO_LARGE"));
                    }

                    WriteBatch batch = db.batch();
                    Map<String, Object> eventUpdate = new HashMap<>();
                    eventUpdate.put("status", Event.STATUS_DELETED);
                    eventUpdate.put("registrationsOpen", false);
                    batch.update(db.collection(COLLECTION).document(eventId), eventUpdate);
                    addDeletes(batch, favorites.getResult());
                    addDeletes(batch, notifications.getResult());
                    return batch.commit();
                });
    }

    private static int snapshotSize(@Nullable QuerySnapshot snapshot) {
        return snapshot == null ? 0 : snapshot.size();
    }

    private static void addDeletes(@NonNull WriteBatch batch, @Nullable QuerySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            batch.delete(document.getReference());
        }
    }

    private List<Event> toEvents(@Nullable QuerySnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return Collections.emptyList();
        }
        List<Event> events = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = fromSnapshot(doc);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private static Event fromSnapshot(DocumentSnapshot doc) {
        Event event = new Event(doc.getId());
        event.setTitle(doc.getString("title"));
        event.setDescription(doc.getString("description"));
        event.setCategory(doc.getString("category"));
        Long dateLong = doc.getLong("dateLong");
        event.setDateLong(dateLong != null ? dateLong : 0L);
        event.setDisplayDate(doc.getString("displayDate"));
        event.setLocation(doc.getString("location"));
        event.setImageUrl(doc.getString("imageUrl"));
        event.setOrganizerId(doc.getString("organizerId"));
        event.setOrganizerName(doc.getString("organizerName"));
        Boolean orgVerified = doc.getBoolean("organizerVerified");
        event.setOrganizerVerified(Boolean.TRUE.equals(orgVerified));
        event.setMaxParticipants(safeLong(doc.get("maxParticipants")));
        event.setCurrentParticipants(safeLong(doc.get("currentParticipants")));
        event.setMaxVolunteers(safeLong(doc.get("maxVolunteers")));
        event.setCurrentVolunteers(safeLong(doc.get("currentVolunteers")));
        Boolean regOpen = doc.getBoolean("registrationsOpen");
        event.setRegistrationsOpen(regOpen == null || regOpen);
        event.setStatus(doc.getString("status"));
        Long createdAt = doc.getLong("createdAt");
        event.setCreatedAt(createdAt != null ? createdAt : 0L);
        return event;
    }

    private static long safeLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
