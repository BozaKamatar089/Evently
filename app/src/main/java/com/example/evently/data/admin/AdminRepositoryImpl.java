package com.example.evently.data.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.AdminStats;
import com.example.evently.data.model.Event;
import com.example.evently.data.model.User;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.domain.admin.AdminRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore implementacija {@link AdminRepository}.
 * Admin operacije: stats, verifikacije, eventi, approve/reject.
 */
public class AdminRepositoryImpl implements AdminRepository {

    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_EVENTS = "Events";
    private static final String COLLECTION_REGISTRATIONS = "Registrations";
    private static final String COLLECTION_VERIFICATIONS = "VerificationRequests";

    private final FirebaseFirestore db;

    public AdminRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public Task<AdminStats> getStats() {
        Task<QuerySnapshot> usersTask = db.collection(COLLECTION_USERS).get();
        Task<QuerySnapshot> eventsTask = db.collection(COLLECTION_EVENTS).get();
        Task<QuerySnapshot> regsTask = db.collection(COLLECTION_REGISTRATIONS).get();

        return Tasks.whenAll(usersTask, eventsTask, regsTask)
                .continueWith(v -> {
                    int userCount = usersTask.getResult() != null ? usersTask.getResult().size() : 0;
                    int eventCount = 0;
                    if (eventsTask.getResult() != null) {
                        for (DocumentSnapshot event : eventsTask.getResult().getDocuments()) {
                            if (!"deleted".equals(event.getString("status"))) eventCount++;
                        }
                    }
                    int registrationCount = regsTask.getResult() != null ? regsTask.getResult().size() : 0;
                    return new AdminStats(userCount, eventCount, registrationCount);
                });
    }

    @Override
    public Task<List<VerificationRequest>> getPendingVerifications() {
        return db.collection(COLLECTION_VERIFICATIONS)
                .whereEqualTo("status", VerificationRequest.STATUS_PENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<VerificationRequest> values = toVerifications(task.getResult());
                    values.sort((left, right) -> Long.compare(right.getCreatedAt(), left.getCreatedAt()));
                    return values;
                });
    }

    @Override
    public Task<List<Event>> getAllEvents() {
        return db.collection(COLLECTION_EVENTS)
                .orderBy("dateLong", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Event> events = new ArrayList<>();
                    if (task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            Event event = fromEventSnapshot(doc);
                            if (event != null && !"deleted".equals(event.getStatus())) events.add(event);
                        }
                    }
                    return events;
                });
    }

    @Override
    public Task<Void> approveVerification(@NonNull String userId, @NonNull String orgName) {
        return requireAdminUid().onSuccessTask(adminUid -> db.runTransaction(transaction -> {
            // Update VerificationRequest
            DocumentReference vrRef = db.collection(COLLECTION_VERIFICATIONS).document(userId);
            Map<String, Object> verificationUpdate = new HashMap<>();
            verificationUpdate.put("status", VerificationRequest.STATUS_APPROVED);
            verificationUpdate.put("adminId", adminUid);
            verificationUpdate.put("rejectionReason", FieldValue.delete());
            transaction.update(vrRef, verificationUpdate);

            // Keep every verification projection in Users synchronized atomically.
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("role", User.ROLE_ORGANIZER);
            userUpdate.put("isVerified", true);
            userUpdate.put("verificationStatus", VerificationRequest.STATUS_APPROVED);
            userUpdate.put("rejectionReason", FieldValue.delete());
            transaction.update(userRef, userUpdate);

            return null;
        }));
    }

    @Override
    public Task<Void> rejectVerification(@NonNull String userId, @NonNull String orgName,
                                         @NonNull String reason) {
        return requireAdminUid().onSuccessTask(adminUid -> db.runTransaction(transaction -> {
            DocumentReference vrRef = db.collection(COLLECTION_VERIFICATIONS).document(userId);
            transaction.update(vrRef, "status", VerificationRequest.STATUS_REJECTED,
                    "rejectionReason", reason,
                    "adminId", adminUid);

            DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
            transaction.update(userRef, "role", User.ROLE_PARTICIPANT,
                    "isVerified", false,
                    "verificationStatus", VerificationRequest.STATUS_REJECTED,
                    "rejectionReason", reason);

            return null;
        }));
    }

    private Task<String> requireAdminUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getUid() == null || user.getUid().isEmpty()) {
            return Tasks.forException(new IllegalStateException("AUTH_REQUIRED"));
        }
        return Tasks.forResult(user.getUid());
    }

    private static List<VerificationRequest> toVerifications(@Nullable QuerySnapshot snapshot) {
        List<VerificationRequest> list = new ArrayList<>();
        if (snapshot == null || snapshot.isEmpty()) return list;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            VerificationRequest vr = new VerificationRequest(doc.getId());
            vr.setUserId(doc.getString("userId"));
            vr.setOrgName(doc.getString("orgName"));
            vr.setDocumentUrl(doc.getString("documentUrl"));
            vr.setStatus(doc.getString("status"));
            vr.setRejectionReason(doc.getString("rejectionReason"));
            Long createdAt = doc.getLong("createdAt");
            vr.setCreatedAt(createdAt != null ? createdAt : 0L);
            list.add(vr);
        }
        return list;
    }

    private static Event fromEventSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Event event = new Event(doc.getId());
        event.setTitle(doc.getString("title"));
        event.setDescription(doc.getString("description"));
        event.setCategory(doc.getString("category"));
        event.setDateLong(safeLong(doc.get("dateLong")));
        event.setDisplayDate(doc.getString("displayDate"));
        event.setLocation(doc.getString("location"));
        event.setImageUrl(doc.getString("imageUrl"));
        event.setOrganizerId(doc.getString("organizerId"));
        event.setOrganizerName(doc.getString("organizerName"));
        event.setOrganizerVerified(Boolean.TRUE.equals(doc.getBoolean("organizerVerified")));
        event.setMaxParticipants(safeLong(doc.get("maxParticipants")));
        event.setCurrentParticipants(safeLong(doc.get("currentParticipants")));
        event.setMaxVolunteers(safeLong(doc.get("maxVolunteers")));
        event.setCurrentVolunteers(safeLong(doc.get("currentVolunteers")));
        event.setRegistrationsOpen(doc.getBoolean("registrationsOpen") == null
                || doc.getBoolean("registrationsOpen"));
        event.setStatus(doc.getString("status"));
        event.setCreatedAt(safeLong(doc.get("createdAt")));
        return event;
    }

    private static long safeLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }
}
