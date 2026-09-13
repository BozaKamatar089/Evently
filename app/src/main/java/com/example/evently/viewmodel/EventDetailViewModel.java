package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.Event;
import com.example.evently.data.model.Registration;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.registration.RegistrationRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Posrednik između EventDetailsActivity i Firebase repositorija.
 * <p>
 * UI sloj nikada ne poziva repositorije direktno (UI -> ViewModel -> Repository -> Firebase).
 * Osim delegiranja operacija, ViewModel je vlasnik generacijskih brojača koji
 * sprječavaju zastarjele callbackove nakon lifecycle/sesije promjene:
 * <ul>
 *   <li>{@link #stopEventListener()} invalidira i event snapshot i sve u letu
 *       pokrenute registration čitanje (ponašanje identično starom Activity kodu),</li>
 *   <li>{@link #invalidateRegistrationReads()} poziva se pri promjeni sesije.</li>
 * </ul>
 * Sirovi kod greške se prosljeđuje callbacku; mapiranje na UI poruke ostaje u UI sloju.
 */
public class EventDetailViewModel extends ViewModel {

    /** Snapshot događaja ili greška učitavanja. */
    public interface EventSnapshotCallback {
        void onEvent(@Nullable Event event, @Nullable Exception error);
    }

    /** Jednokratno čitanje vlastite prijave; errorCode != null znači neuspjeh. */
    public interface RegistrationCallback {
        void onRegistration(@Nullable Registration registration, @Nullable String errorCode);
    }

    /** Rezultat mutacije (register/cancel/delete/remove). */
    public interface OperationCallback {
        void onSuccess();
        void onError(@Nullable String errorCode);
    }

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    private ListenerRegistration eventListener;
    private ListenerRegistration eventRegistrationsListener;
    private int eventGeneration;
    private int registrationReadGeneration;

    public EventDetailViewModel(@NonNull EventRepository eventRepository,
                                @NonNull RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }
/**
     * Pokreće real-time event listener. Ponovni poziv dok je listener aktivan
     * (ili s null eventId) je no-op — ista zaštita kao i ranije u Activity-u.
     */
    public void startEventListener(@Nullable String eventId, @NonNull EventSnapshotCallback callback) {
        if (eventListener != null || eventId == null) return;
        final int generation = ++eventGeneration;
        eventListener = eventRepository.listenEvent(eventId, (event, error) -> {
            if (generation != eventGeneration) return;
            callback.onEvent(event, error);
        });
    }

    /**
     * Uklanja event listener. Bumpa i registration generaciju tako da u letu
     * pokrenuto jednostruko čitanje prijave ne može dodijeliti zastarjeli rezultat.
     */
    public void stopEventListener() {
        eventGeneration++;
        registrationReadGeneration++;
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    /** Invalidira u letu pokrenuta registration čitanja (npr. pri promjeni sesije). */
    public void invalidateRegistrationReads() {
        registrationReadGeneration++;
    }

    /** Jednokratno čitanje vlastite prijave sa zastarjelošću zaštićenim callbackom. */
    public void fetchMyRegistration(@NonNull String eventId, @NonNull RegistrationCallback callback) {
        final int generation = ++registrationReadGeneration;
        registrationRepository.getRegistration(eventId)
                .addOnSuccessListener(registration -> {
                    if (generation != registrationReadGeneration) return;
                    callback.onRegistration(registration, null);
                })
                .addOnFailureListener(e -> {
                    if (generation != registrationReadGeneration) return;
                    callback.onRegistration(null, e.getMessage());
                });
    }
public void register(@NonNull String eventId, @NonNull String type, @NonNull OperationCallback callback) {
        attach(registrationRepository.register(eventId, type), callback);
    }

    public void cancelRegistration(@NonNull String eventId, @NonNull OperationCallback callback) {
        attach(registrationRepository.cancelRegistration(eventId), callback);
    }

    /** Soft-delete praznog eventa sa kaskadom — samo kroz repository. */
    public void deleteEvent(@NonNull String eventId, @NonNull OperationCallback callback) {
        attach(eventRepository.deleteEvent(eventId), callback);
    }

    /** Organizatorsko uklanjanje prijave (transakcijski dekrement countera). */
    public void removeRegistration(@NonNull String eventId, @NonNull String userId,
                                   @NonNull OperationCallback callback) {
        attach(registrationRepository.removeRegistration(eventId, userId), callback);
    }

    /**
     * Pokreće listener prijava za organizatorski dialog. Prethodno uklanja
     * eventualno aktivan listener — tačno jedan listener je aktivan u svakom trenutku.
     */
    public void startEventRegistrationsListener(@NonNull String eventId,
                                                @NonNull RegistrationRepository.OnRegistrationsListener listener) {
        stopEventRegistrationsListener();
        eventRegistrationsListener = registrationRepository.listenEventRegistrations(eventId, listener);
    }

    public void stopEventRegistrationsListener() {
        if (eventRegistrationsListener != null) {
            eventRegistrationsListener.remove();
            eventRegistrationsListener = null;
        }
    }

    private void attach(@NonNull Task<Void> task, @NonNull OperationCallback callback) {
        task.addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    @Override
    protected void onCleared() {
        stopEventListener();
        stopEventRegistrationsListener();
        super.onCleared();
    }
}