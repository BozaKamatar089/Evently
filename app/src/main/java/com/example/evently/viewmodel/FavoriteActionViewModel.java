package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.domain.favorites.FavoriteRepository;
import com.example.evently.data.model.Favorite;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Owns favorite mutations; adapters only report a user intent. */
public final class FavoriteActionViewModel extends ViewModel {
    public static final class Result {
        private final String eventId; private final boolean favorite; private final String errorCode;
        private Result(String eventId, boolean favorite, String errorCode) { this.eventId = eventId; this.favorite = favorite; this.errorCode = errorCode; }
        public static Result success(String eventId, boolean favorite) { return new Result(eventId, favorite, null); }
        public static Result failure(String eventId) { return new Result(eventId, false, "FAVORITE_ERROR"); }
        public String getEventId() { return eventId; } public boolean isFavorite() { return favorite; } public String getErrorCode() { return errorCode; }
    }
    private final FavoriteRepository repository;
    private final MutableLiveData<Result> result = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> favoriteIds = new MutableLiveData<>(Collections.emptySet());
    private final MutableLiveData<Boolean> streamError = new MutableLiveData<>(false);
    private final Set<String> pending = new HashSet<>();
    private ListenerRegistration registration;
    private String activeUid = "";
    private int generation;
    public FavoriteActionViewModel(@NonNull FavoriteRepository repository) { this.repository = repository; }
    public LiveData<Result> getResult() { return result; }
    public LiveData<Set<String>> getFavoriteIds() { return favoriteIds; }
    public LiveData<Boolean> getStreamError() { return streamError; }
    public void observeForUser(String uid) {
        String requested = uid != null ? uid : "";
        if (requested.equals(activeUid) && registration != null) return;
        stopObserving(); activeUid = requested;
        if (requested.isEmpty()) { favoriteIds.setValue(Collections.emptySet()); return; }
        final int requestGeneration = ++generation;
        registration = repository.listenMyFavorites((favorites, error) -> {
            if (requestGeneration != generation) return;
            if (error != null) {
                if (registration != null) registration.remove(); registration = null;
                streamError.setValue(true); return;
            }
            Set<String> ids = new HashSet<>();
            if (favorites != null) for (Favorite favorite : favorites) {
                if (favorite.getEventId() != null) ids.add(favorite.getEventId());
            }
            favoriteIds.setValue(Collections.unmodifiableSet(ids));
            streamError.setValue(false);
        });
    }
    public void stopObserving() {
        generation++; pending.clear(); activeUid = ""; result.setValue(null);
        if (registration != null) { registration.remove(); registration = null; }
        favoriteIds.setValue(Collections.emptySet()); streamError.setValue(false);
    }
    /**
     * Applies the user's intended favorite state for {@code eventId}.
     * <p>
     * The desired state is computed once from the authoritative favorite-listener
     * state ({@link #favoriteIds}); no extra Firestore {@code isFavorite()} read
     * is performed and no delete-first semantics are used. The mutation therefore
     * targets a deterministic state, while the realtime listener remains the
     * authority afterwards.
     */
    public void toggle(@NonNull String eventId) {
        if (activeUid.isEmpty()) { result.setValue(Result.failure(eventId)); return; }
        if (!pending.add(eventId)) return;
        final int mutationGeneration = generation;

        Set<String> knownFavorites = favoriteIds.getValue();
        boolean currentlyFavorite = knownFavorites != null && knownFavorites.contains(eventId);
        boolean desiredFavorite = !currentlyFavorite;

        Task<Void> mutation = currentlyFavorite
                ? repository.removeFavorite(eventId)
                : repository.addFavorite(eventId);

        mutation
                .addOnSuccessListener(v -> {
                    if (mutationGeneration != generation) return;
                    pending.remove(eventId);
                    result.setValue(Result.success(eventId, desiredFavorite));
                })
                .addOnFailureListener(e -> {
                    if (mutationGeneration != generation) return;
                    pending.remove(eventId);
                    result.setValue(Result.failure(eventId));
                });
    }
    @Override protected void onCleared() { stopObserving(); super.onCleared(); }
}