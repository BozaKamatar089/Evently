package com.example.evently.domain.favorites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.Favorite;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Repository za omiljene događaje (favorites).
 * Sva favorites komunikacija ide kroz ovaj repository.
 */
public interface FavoriteRepository {

    /** Dodaje događaj u omiljene. */
    Task<Void> addFavorite(@NonNull String eventId);

    /** Uklanja događaj iz omiljenih. */
    Task<Void> removeFavorite(@NonNull String eventId);

    /** Provjerava da li je događaj u omiljenima. */
    Task<Boolean> isFavorite(@NonNull String eventId);

    /** Sluša sve omiljene događaje trenutnog korisnika. */
    ListenerRegistration listenMyFavorites(@NonNull OnFavoritesListener listener);

    interface OnFavoritesListener {
        void onFavorites(@Nullable List<Favorite> favorites, @Nullable Exception error);
    }
}