package com.example.evently.domain.user;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;

/**
 * Repository za korisnike. Users dokument se kreira isključivo kroz ovaj repository.
 */
public interface UserRepository {

    /** Kreira (akko ne postoji) {@code Users/{uid}} dokument sa podacima iz {@link User}. */
    Task<Void> ensureUserCreated(@NonNull User user);

    /** Dohvata {@code Users/{uid}} dokument; vraća null ako ne postoji. */
    Task<User> getUser(@NonNull String uid);

    /**
     * Real-time listener za korisnički profil.
     */
    ListenerRegistration listenUser(@NonNull String uid, @NonNull OnUserListener listener);

    /**
     * Ažurira SAMO editabilna polja (name, bio, phone, photoUrl) preko mape.
     * Nikada ne serijalizira cijeli POJO; role/verificationStatus/rejectionReason
     * su zaključana Firestore pravilima i korisnik ih ne smije mijenjati.
     */
    Task<Void> updateUser(@NonNull String uid, @NonNull Map<String, Object> editableFields);

    interface OnUserListener {
        void onUser(@Nullable User user, @Nullable Exception error);
    }
}
