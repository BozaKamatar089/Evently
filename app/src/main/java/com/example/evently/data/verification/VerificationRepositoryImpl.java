package com.example.evently.data.verification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.VerificationRequest;
import com.example.evently.domain.verification.VerificationRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Firestore implementacija {@link VerificationRepository}.
 * VerificationRequest je izvor zahtjeva, dok se verificationStatus u Users drži kao
 * atomarna projekcija za brz prikaz i autorizaciju.
 */
public class VerificationRepositoryImpl implements VerificationRepository {

    private static final String COLLECTION = "VerificationRequests";
    private static final String COLLECTION_USERS = "Users";

    private final FirebaseFirestore db;

    public VerificationRepositoryImpl() {
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
    public Task<Void> submitRequest(@NonNull String orgName, @NonNull String documentUrl) {
        return requireUid().onSuccessTask(uid -> {
            VerificationRequest request = new VerificationRequest(uid);
            request.setUserId(uid);
            request.setOrgName(orgName);
            request.setDocumentUrl(documentUrl);
            DocumentReference requestRef = db.collection(COLLECTION).document(uid);
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(uid);

            return db.runTransaction(transaction -> {
                DocumentSnapshot requestSnapshot = transaction.get(requestRef);
                DocumentSnapshot userSnapshot = transaction.get(userRef);
                if (!userSnapshot.exists()) {
                    throw new IllegalStateException("USER_NOT_FOUND");
                }

                String verificationStatus = userSnapshot.getString("verificationStatus");
                if (requestSnapshot.exists()) {
                    if (!VerificationRequest.STATUS_REJECTED.equals(requestSnapshot.getString("status"))
                            || !VerificationRequest.STATUS_REJECTED.equals(verificationStatus)) {
                        throw new IllegalStateException("VERIFICATION_REQUEST_NOT_RESUBMITTABLE");
                    }
                } else if (!"none".equals(verificationStatus)) {
                    throw new IllegalStateException("VERIFICATION_REQUEST_NOT_SUBMITTABLE");
                }

                transaction.set(requestRef, request.toCreateMap());
                transaction.update(userRef,
                        "verificationStatus", VerificationRequest.STATUS_PENDING,
                        "rejectionReason", FieldValue.delete());
                return null;
            });
        });
    }

    @Override
    public Task<Void> cancelRequest(@NonNull String requestId) {
        return requireUid().onSuccessTask(uid -> {
            if (!uid.equals(requestId)) {
                return Tasks.forException(new IllegalArgumentException("VERIFICATION_NOT_OWNER"));
            }
            DocumentReference requestRef = db.collection(COLLECTION).document(requestId);
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(uid);
            return db.runTransaction(transaction -> {
                DocumentSnapshot requestSnapshot = transaction.get(requestRef);
                if (!requestSnapshot.exists()
                        || !VerificationRequest.STATUS_PENDING.equals(requestSnapshot.getString("status"))) {
                    throw new IllegalStateException("VERIFICATION_REQUEST_NOT_CANCELLABLE");
                }
                transaction.delete(requestRef);
                transaction.update(userRef,
                        "verificationStatus", "none",
                        "rejectionReason", FieldValue.delete());
                return null;
            });
        });
    }

    @Override
    public ListenerRegistration listenMyRequest(@NonNull OnRequestListener listener) {
        String uid = getMyUid();
        if (uid == null || uid.isEmpty()) {
            listener.onRequest(null, null);
            return () -> { };
        }
        return db.collection(COLLECTION).document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onRequest(null, error);
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        listener.onRequest(null, null);
                        return;
                    }
                    listener.onRequest(fromSnapshot(snapshot), null);
                });
    }

    private static VerificationRequest fromSnapshot(@NonNull DocumentSnapshot doc) {
        VerificationRequest request = new VerificationRequest(doc.getId());
        request.setUserId(doc.getString("userId"));
        request.setOrgName(doc.getString("orgName"));
        request.setDocumentUrl(doc.getString("documentUrl"));
        request.setStatus(doc.getString("status"));
        request.setRejectionReason(doc.getString("rejectionReason"));
        Long createdAt = doc.getLong("createdAt");
        request.setCreatedAt(createdAt != null ? createdAt : 0L);
        request.setAdminId(doc.getString("adminId"));
        return request;
    }
}
