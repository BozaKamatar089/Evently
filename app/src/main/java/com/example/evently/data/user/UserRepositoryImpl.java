package com.example.evently.data.user;

import androidx.annotation.NonNull;

import com.example.evently.data.model.User;
import com.example.evently.domain.user.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Firestore implementacija {@link UserRepository}.
 * Sva Firestore komunikacija za korisnike prolazi kroz ovaj repository.
 */
public class UserRepositoryImpl implements UserRepository {

    private static final String COLLECTION = "Users";
    private static final Set<String> EDITABLE_FIELDS = new HashSet<>(Arrays.asList(
            "name", "bio", "phone", "photoUrl"
    ));

    private final FirebaseFirestore db;

    public UserRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public Task<Void> updateUser(@NonNull String uid, @NonNull Map<String, Object> editableFields) {
        Map<String, Object> safeFields = new HashMap<>();
        for (Map.Entry<String, Object> entry : editableFields.entrySet()) {
            if (!EDITABLE_FIELDS.contains(entry.getKey())) {
                return Tasks.forException(new IllegalArgumentException("PROFILE_FIELD_NOT_EDITABLE"));
            }
            safeFields.put(entry.getKey(),
                    entry.getValue() != null ? entry.getValue() : FieldValue.delete());
        }
        if (safeFields.isEmpty()) {
            return Tasks.forResult(null);
        }
        return db.collection(COLLECTION).document(uid)
                .update(safeFields);
    }

    @Override
    public Task<Void> ensureUserCreated(@NonNull User user) {
        String uid = user.getUid();

        com.google.firebase.firestore.DocumentReference ref = db.collection(COLLECTION).document(uid);
        // Startup recovery and interactive sign-in may race. Only the missing profile is created.
        return db.runTransaction(transaction -> {
            if (!transaction.get(ref).exists()) transaction.set(ref, user.toCreateMap());
            return null;
        });
    }

    @Override
    public Task<User> getUser(@NonNull String uid) {
        return db.collection(COLLECTION).document(uid).get()
                .continueWith(task -> {
                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot == null || !snapshot.exists()) {
                        return null;
                    }
                    User user = new User(uid);
                    user.setName(snapshot.getString("name"));
                    user.setEmail(snapshot.getString("email"));
                    user.setPhotoUrl(snapshot.getString("photoUrl"));
                    user.setBio(snapshot.getString("bio"));
                    user.setPhone(snapshot.getString("phone"));
                    user.setRole(snapshot.getString("role"));
                    Object verified = snapshot.get("isVerified");
                    user.setVerified(Boolean.TRUE.equals(verified));
                    user.setVerificationStatus(snapshot.getString("verificationStatus"));
                    user.setRejectionReason(snapshot.getString("rejectionReason"));
                    Long memberSince = snapshot.getLong("memberSince");
                    user.setMemberSince(memberSince != null ? memberSince : 0L);
                    return user;
                });
    }

    @Override
    public ListenerRegistration listenUser(@NonNull String uid,
                                           @NonNull OnUserListener listener) {
        return db.collection(COLLECTION).document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onUser(null, error);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        User user = new User(uid);
                        user.setName(snapshot.getString("name"));
                        user.setEmail(snapshot.getString("email"));
                        user.setPhotoUrl(snapshot.getString("photoUrl"));
                        user.setBio(snapshot.getString("bio"));
                        user.setPhone(snapshot.getString("phone"));
                        user.setRole(snapshot.getString("role"));
                        Object verified = snapshot.get("isVerified");
                        user.setVerified(Boolean.TRUE.equals(verified));
                        user.setVerificationStatus(snapshot.getString("verificationStatus"));
                        user.setRejectionReason(snapshot.getString("rejectionReason"));
                        Long memberSince = snapshot.getLong("memberSince");
                        user.setMemberSince(memberSince != null ? memberSince : 0L);
                        listener.onUser(user, null);
                    } else {
                        listener.onUser(null, null);
                    }
                });
    }
}
