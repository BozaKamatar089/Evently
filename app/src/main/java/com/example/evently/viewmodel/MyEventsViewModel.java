package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.Event;
import com.example.evently.data.model.Registration;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.registration.RegistrationRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewModel za "Moji eventi" tab.
 * Kombinira: događaje koje korisnik organizira + događaje na koje je prijavljen.
 */
public class MyEventsViewModel extends ViewModel {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    private final MutableLiveData<List<Event>> myEvents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<ListScreenState> state = new MutableLiveData<>(ListScreenState.guest());

    private final List<Event> allEvents = new ArrayList<>();
    private final Set<String> myRegisteredEventIds = new HashSet<>();

    private ListenerRegistration eventsRegistration;
    private ListenerRegistration regsRegistration;

    private String myUid;
    private final DualSourceReadiness readiness = new DualSourceReadiness();
    private int generation;

    public MyEventsViewModel(@NonNull EventRepository eventRepository,
                             @NonNull RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    public void startListening(String uid) {
        String requestedUid = uid != null ? uid : "";
        if ((eventsRegistration != null || regsRegistration != null) && requestedUid.equals(myUid)) {
            return;
        }
        stopListening(); myUid = requestedUid;
        if (myUid.isEmpty()) {
            myEvents.setValue(new ArrayList<>());
            loading.setValue(false);
            state.setValue(ListScreenState.guest());
            return;
        }
        final int requestGeneration = ++generation;
        readiness.reset(); loading.setValue(true); error.setValue(null); state.setValue(ListScreenState.loading());

        regsRegistration = registrationRepository.listenMyRegistrations((registrations, err) -> {
            if (requestGeneration != generation) return;
            if (err != null) {
                cancelSubscriptions(); generation++;
                error.setValue(err.getMessage());
                loading.setValue(false);
                state.setValue(ListScreenState.error(err.getMessage()));
                return;
            }
            myRegisteredEventIds.clear();
            if (registrations != null) {
                for (Registration registration : registrations) {
                    if (registration.getEventId() != null) {
                        myRegisteredEventIds.add(registration.getEventId());
                    }
                }
            }
            readiness.firstReady();
            combine();
        });

        eventsRegistration = eventRepository.listenAllEvents((list, err) -> {
            if (requestGeneration != generation) return;
            if (err != null) {
                cancelSubscriptions(); generation++;
                error.setValue(err.getMessage());
                loading.setValue(false);
                state.setValue(ListScreenState.error(err.getMessage()));
                return;
            }
            allEvents.clear();
            if (list != null) {
                allEvents.addAll(list);
            }
            readiness.secondReady();
            combine();
        });
    }

    public void stopListening() {
        cancelSubscriptions();
        allEvents.clear();
        myRegisteredEventIds.clear();
        myEvents.setValue(new ArrayList<>());
        generation++;
    }
    private void cancelSubscriptions() {
        if (regsRegistration != null) { regsRegistration.remove(); regsRegistration = null; }
        if (eventsRegistration != null) { eventsRegistration.remove(); eventsRegistration = null; }
    }

    public void refresh(@NonNull String uid) { stopListening(); startListening(uid); }
    public void showSessionLoading() { stopListening(); loading.setValue(true); state.setValue(ListScreenState.loading()); }
    public void showSessionError(@NonNull String code) { stopListening(); loading.setValue(false); state.setValue(ListScreenState.error(code)); }
    public void showAuxiliaryError(@NonNull String code) { stopListening(); loading.setValue(false); error.setValue(code); state.setValue(ListScreenState.error(code)); }

    private void combine() {
        if (!readiness.isReady()) return;
        List<Event> result = new ArrayList<>();
        for (Event event : allEvents) {
            if (isMine(event)) {
                result.add(event);
            }
        }
        myEvents.setValue(result);
        loading.setValue(false);
        state.setValue(ListScreenState.data(result));
    }

    private boolean isMine(@NonNull Event event) {
        String organizerId = event.getOrganizerId();
        if (myUid != null && !myUid.isEmpty() && myUid.equals(organizerId)) {
            return true;
        }
        return myRegisteredEventIds.contains(event.getId());
    }

    public LiveData<List<Event>> getMyEvents() {
        return myEvents;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }
    public LiveData<ListScreenState> getState() { return state; }

    @Override
    protected void onCleared() {
        stopListening();
        super.onCleared();
    }
}
