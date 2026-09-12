package com.example.evently.data.registration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.Registration;
import com.example.evently.domain.registration.RegistrationRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore implementacija {@link RegistrationRepository}.
 * <p>
 * Pravilo #1 (CLAUDE.md): counteri se mijenjaju isključivo Firestore transakcijom.
 * Ovdje se unutar jedne transakcije čita Event, provjerava kapacitet, kreira Registration
 * i ažurira counter — atomično.
 */
public class RegistrationRepositoryImpl implements RegistrationRepository {

    private static final String COLLECTION = "Registrations";
    private static final String EVENTS = "Events";

    private final FirebaseFirestore db;

    public RegistrationRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Nullable
    private String getMyUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private Task<String> requireUid() {
        String uid = getMyUid();
        if (uid == null || uid.isEmpty()) {
            return Tasks.forException(new IllegalStateException("Korisnik nije prijavljen"));
        }
        return Tasks.forResult(uid);
    }

    @Override
    public Task<Void> register(@NonNull String eventId, @NonNull String type) {
        return requireUid().onSuccessTask(uid ->
                db.runTransaction(transaction -> {

                    DocumentReference regRef = db.collection(COLLECTION).document(uid + "_" + eventId);
                    DocumentSnapshot regSnap = transaction.get(regRef);
                    if (regSnap.exists()) {
                        throw new FirebaseFirestoreException(
                                "ALREADY_REGISTERED",
                                FirebaseFirestoreException.Code.ABORTED);
                    }

                    DocumentReference eventRef = db.collection(EVENTS).document(eventId);
                    DocumentSnapshot eventSnap = transaction.get(eventRef);
                    if (!eventSnap.exists()) {
                        throw new FirebaseFirestoreException(
                                "EVENT_NOT_FOUND",
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    Boolean regOpen = eventSnap.getBoolean("registrationsOpen");
                    if (regOpen != null && !regOpen) {
                        throw new FirebaseFirestoreException(
                                "REGISTRATIONS_CLOSED",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }
                    String status = eventSnap.getString("status");
                    if (!"active".equals(status)) {
                        throw new FirebaseFirestoreException(
                                "EVENT_NOT_ACTIVE",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }
                    Long dateLong = eventSnap.getLong("dateLong");
                    if (dateLong == null || dateLong <= System.currentTimeMillis() - 86_400_000L) {
                        throw new FirebaseFirestoreException(
                                "EVENT_PAST",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }

                    long maxParticipants = safeLong(eventSnap.get("maxParticipants"));
                    long currentParticipants = safeLong(eventSnap.get("currentParticipants"));
                    long maxVolunteers = safeLong(eventSnap.get("maxVolunteers"));
                    long currentVolunteers = safeLong(eventSnap.get("currentVolunteers"));

                    if (Registration.TYPE_PARTICIPANT.equals(type)) {
                        if (currentParticipants >= maxParticipants) {
                            throw new FirebaseFirestoreException(
                                    "CAPACITY_FULL",
                                    FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }
                        currentParticipants += 1;
                    } else if (Registration.TYPE_VOLUNTEER.equals(type)) {
                        if (currentVolunteers >= maxVolunteers) {
                            throw new FirebaseFirestoreException(
                                    "CAPACITY_FULL",
                                    FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }
                        currentVolunteers += 1;
                    } else {
                        throw new FirebaseFirestoreException(
                                "INVALID_TYPE",
                                FirebaseFirestoreException.Code.INVALID_ARGUMENT);
                    }

                    Map<String, Object> regData = new HashMap<>();
                    regData.put("eventId", eventId);
                    regData.put("userId", uid);
                    regData.put("type", type);
                    regData.put("status", Registration.STATUS_REGISTERED);
                    regData.put("registeredAt", System.currentTimeMillis());
                    regData.put("organizerId", eventSnap.getString("organizerId"));
                    transaction.set(regRef, regData);

                    Map<String, Object> counter = new HashMap<>();
                    counter.put("currentParticipants", currentParticipants);
                    counter.put("currentVolunteers", currentVolunteers);
                    transaction.update(eventRef, counter);

                    return null;
                }));
    }

    @Override
    public Task<Void> cancelRegistration(@NonNull String eventId) {
        return requireUid().onSuccessTask(uid ->
                db.runTransaction(transaction -> {

                    DocumentReference regRef = db.collection(COLLECTION).document(uid + "_" + eventId);
                    DocumentSnapshot regSnap = transaction.get(regRef);
                    if (!regSnap.exists()) {
                        throw new FirebaseFirestoreException(
                                "NOT_REGISTERED",
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }
                    String type = regSnap.getString("type");

                    DocumentReference eventRef = db.collection(EVENTS).document(eventId);
                    DocumentSnapshot eventSnap = transaction.get(eventRef);
                    if (eventSnap.exists()) {
                        long currentParticipants = safeLong(eventSnap.get("currentParticipants"));
                        long currentVolunteers = safeLong(eventSnap.get("currentVolunteers"));

                        Map<String, Object> counter = new HashMap<>();
                        if (Registration.TYPE_PARTICIPANT.equals(type)) {
                            counter.put("currentParticipants", Math.max(0, currentParticipants - 1));
                        } else {
                            counter.put("currentVolunteers", Math.max(0, currentVolunteers - 1));
                        }
                        transaction.update(eventRef, counter);
                    }

                    transaction.delete(regRef);
                    return null;
                }));
    }

    @Override
    public Task<Registration> getRegistration(@NonNull String eventId) {
        return requireUid().continueWithTask(task -> db.collection(COLLECTION)
                .document(task.getResult() + "_" + eventId).get())
                .continueWith(task -> {
                    DocumentSnapshot doc = task.getResult();
                    if (doc == null || !doc.exists()) {
                        return null;
                    }
                    return fromSnapshot(doc);
                });
    }

    @Override
    public ListenerRegistration listenMyRegistrations(@NonNull OnRegistrationsListener listener) {
        String uid = getMyUid();
        if (uid == null || uid.isEmpty()) {
            listener.onRegistrations(Collections.emptyList(), null);
            return () -> { };
        }
        return db.collection(COLLECTION)
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onRegistrations(null, error);
                        return;
                    }
                    if (snapshot == null) {
                        listener.onRegistrations(Collections.emptyList(), null);
                        return;
                    }
                    listener.onRegistrations(toRegistrations(snapshot), null);
                });
    }

    @Override
    public ListenerRegistration listenEventRegistrations(@NonNull String eventId,
                                                         @NonNull OnRegistrationsListener listener) {
        String uid = getMyUid();
        if (uid == null || uid.isEmpty()) {
            listener.onRegistrations(Collections.emptyList(),
                    new IllegalStateException("AUTH_REQUIRED"));
            return () -> { };
        }
        return db.collection(COLLECTION)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("organizerId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onRegistrations(null, error);
                        return;
                    }
                    if (snapshot == null) {
                        listener.onRegistrations(Collections.emptyList(), null);
                        return;
                    }
                    listener.onRegistrations(toRegistrations(snapshot), null);
                });
    }

    @Override
    public Task<Void> removeRegistration(@NonNull String eventId, @NonNull String userId) {
        return requireUid().onSuccessTask(uid -> {
            if (!uid.equals(userId)) {
                return Tasks.forException(new IllegalStateException(
                        "ORGANIZER_REMOVAL_REQUIRES_BACKEND"));
            }
            return cancelRegistration(eventId);
        });
    }

    private static List<Registration> toRegistrations(@NonNull QuerySnapshot snapshot) {
        if (snapshot.isEmpty()) {
            return Collections.emptyList();
        }
        List<Registration> list = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Registration registration = fromSnapshot(doc);
            if (registration != null) {
                list.add(registration);
            }
        }
        return list;
    }

    private static Registration fromSnapshot(@NonNull DocumentSnapshot doc) {
        Registration registration = new Registration(doc.getId());
        registration.setEventId(doc.getString("eventId"));
        registration.setUserId(doc.getString("userId"));
        registration.setType(doc.getString("type"));
        registration.setStatus(doc.getString("status"));
        Long registeredAt = doc.getLong("registeredAt");
        registration.setRegisteredAt(registeredAt != null ? registeredAt : 0L);
        registration.setOrganizerId(doc.getString("organizerId"));
        return registration;
    }

    private static long safeLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}
