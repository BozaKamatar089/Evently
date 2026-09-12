package com.example.evently.data.favorites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.Favorite;
import com.example.evently.domain.favorites.FavoriteRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore implementacija {@link FavoriteRepository}.
 * Kreira/briše dokumente u {@code favorites/{userId_eventId}}.
 */
public class FavoriteRepositoryImpl implements FavoriteRepository {

    private static final String COLLECTION = "Favorites";

    private final FirebaseFirestore db;

    public FavoriteRepositoryImpl() {
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
    public Task<Void> addFavorite(@NonNull String eventId) {
        return requireUid().onSuccessTask(uid -> {
            String favId = uid + "_" + eventId;
            Favorite favorite = new Favorite(favId);
            favorite.setUserId(uid);
            favorite.setEventId(eventId);
            favorite.setCreatedAt(System.currentTimeMillis());
            return db.collection(COLLECTION).document(favId).set(favorite.toCreateMap());
        });
    }

    @Override
    public Task<Void> removeFavorite(@NonNull String eventId) {
        return requireUid().onSuccessTask(uid -> {
            String favId = uid + "_" + eventId;
            return db.collection(COLLECTION).document(favId).delete();
        });
    }

    @Override
    public Task<Boolean> isFavorite(@NonNull String eventId) {
        return requireUid().onSuccessTask(uid -> {
            String favId = uid + "_" + eventId;
            return db.collection(COLLECTION).document(favId).get()
                    .continueWith(task -> {
                        DocumentSnapshot doc = task.getResult();
                        return doc != null && doc.exists();
                    });
        });
    }

    @Override
    public ListenerRegistration listenMyFavorites(@NonNull OnFavoritesListener listener) {
        String uid = getMyUid();
        if (uid == null || uid.isEmpty()) {
            listener.onFavorites(Collections.emptyList(), null);
            return () -> { };
        }
        return db.collection(COLLECTION)
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onFavorites(null, error);
                        return;
                    }
                    if (snapshot == null) {
                        listener.onFavorites(Collections.emptyList(), null);
                        return;
                    }
                    listener.onFavorites(toFavorites(snapshot), null);
                });
    }

    private static List<Favorite> toFavorites(@NonNull com.google.firebase.firestore.QuerySnapshot snapshot) {
        if (snapshot.isEmpty()) {
            return Collections.emptyList();
        }
        List<Favorite> list = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Favorite favorite = new Favorite(doc.getId());
            favorite.setUserId(doc.getString("userId"));
            favorite.setEventId(doc.getString("eventId"));
            Long createdAt = doc.getLong("createdAt");
            favorite.setCreatedAt(createdAt != null ? createdAt : 0L);
            list.add(favorite);
        }
        return list;
    }
}