package com.example.evently.domain.registration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.Registration;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * Repository za registracije (prijave na događaje).
 * Sva Registrations komunikacija prolazi kroz ovaj repository.
 * <p>
 * Registracija i otkazivanje mijenjaju countere (currentParticipants/currentVolunteers)
 * isključivo kroz Firestore transakciju — nikada read→increment→update izvan transakcije.
 */
public interface RegistrationRepository {

    /** Prijavljuje trenutnog korisnika na događaj (participant|volunteer) transakcijski. */
    Task<Void> register(@NonNull String eventId, @NonNull String type);

    /** Otkazuje prijavu trenutnog korisnika na događaj transakcijski (dekrementira counter). */
    Task<Void> cancelRegistration(@NonNull String eventId);

    /** Vraća prijavu trenutnog korisnika za događaj; null ako nije prijavljen. */
    Task<Registration> getRegistration(@NonNull String eventId);

    /** Sluša sve prijave trenutnog korisnika. */
    ListenerRegistration listenMyRegistrations(@NonNull OnRegistrationsListener listener);

    /** Sluša sve prijave za jedan događaj (organizator). */
    ListenerRegistration listenEventRegistrations(@NonNull String eventId,
                                                  @NonNull OnRegistrationsListener listener);

    /**
     * Organizator uklanja prijavu (transakcijski: dekrementira counter, briše Registration).
     * @param eventId ID događaja
     * @param userId  ID korisnika čija se prijava briše
     */
    Task<Void> removeRegistration(@NonNull String eventId, @NonNull String userId);

    interface OnRegistrationsListener {
        void onRegistrations(@Nullable List<Registration> registrations, @Nullable Exception error);
    }
}
