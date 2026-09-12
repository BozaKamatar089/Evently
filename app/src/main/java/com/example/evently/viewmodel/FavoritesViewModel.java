package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.Event;
import com.example.evently.data.model.Favorite;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.favorites.FavoriteRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewModel za FavoritesFragment.
 * Kombinira favorites sa EventRepository da dohvati puni Event objekat.
 */
public class FavoritesViewModel extends ViewModel {

    private final EventRepository eventRepository;
    private final FavoriteRepository favoriteRepository;

    private final MutableLiveData<List<Event>> favoriteEvents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<ListScreenState> state = new MutableLiveData<>(ListScreenState.guest());

    private final List<Event> allEvents = new ArrayList<>();
    private final Set<String> myFavoriteEventIds = new HashSet<>();

    private ListenerRegistration eventsRegistration;
    private ListenerRegistration favoritesRegistration;
    private final DualSourceReadiness readiness = new DualSourceReadiness();
    private int generation;
    private String activeUid = "";

    public FavoritesViewModel(@NonNull EventRepository eventRepository,
                              @NonNull FavoriteRepository favoriteRepository) {
        this.eventRepository = eventRepository;
        this.favoriteRepository = favoriteRepository;
    }

    public void startListening(String myUid) {
        String uid = myUid != null ? myUid : "";
        if ((eventsRegistration != null || favoritesRegistration != null) && uid.equals(activeUid)) {
            return;
        }
        stopListening(); activeUid = uid;

        if (uid.isEmpty()) {
            state.setValue(ListScreenState.guest());
            loading.setValue(false);
            return;
        }
        final int requestGeneration = ++generation;
        readiness.reset(); loading.setValue(true); error.setValue(null); state.setValue(ListScreenState.loading());

        favoritesRegistration = favoriteRepository.listenMyFavorites((favorites, err) -> {
            if (requestGeneration != generation) return;
            if (err != null) {
                cancelSubscriptions(); generation++;
                error.setValue(err.getMessage());
                loading.setValue(false);
                state.setValue(ListScreenState.error(err.getMessage()));
                return;
            }
            myFavoriteEventIds.clear();
            if (favorites != null) {
                for (Favorite favorite : favorites) {
                    if (favorite.getEventId() != null) {
                        myFavoriteEventIds.add(favorite.getEventId());
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
        myFavoriteEventIds.clear();
        favoriteEvents.setValue(new ArrayList<>());
        generation++;
    }
    private void cancelSubscriptions() {
        if (favoritesRegistration != null) { favoritesRegistration.remove(); favoritesRegistration = null; }
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
            if (myFavoriteEventIds.contains(event.getId()) && !isDeleted(event)) {
                result.add(event);
            }
        }
        favoriteEvents.setValue(result);
        loading.setValue(false);
        state.setValue(ListScreenState.data(result));
    }

    private boolean isDeleted(Event event) {
        return Event.STATUS_DELETED.equals(event.getStatus());
    }

    public LiveData<List<Event>> getFavoriteEvents() {
        return favoriteEvents;
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
