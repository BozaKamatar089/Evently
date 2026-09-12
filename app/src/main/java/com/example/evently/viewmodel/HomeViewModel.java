package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.Event;
import com.example.evently.domain.event.EventRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends ViewModel {

    private final EventRepository eventRepository;

    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    private final MutableLiveData<ListScreenState> state = new MutableLiveData<>(ListScreenState.loading());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final List<Event> allEvents = new ArrayList<>();
    private ListenerRegistration registration;
    private int generation;
    private boolean hasSuccessfulData;

    private String query = "";
    private String category = "";

    public HomeViewModel(@NonNull EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void startListening() {
        if (registration != null) {
            return;
        }
        final int requestGeneration = ++generation;
        loading.setValue(true); error.setValue(null); state.setValue(ListScreenState.loading());
        hasSuccessfulData = false;
        registration = eventRepository.listenAllEvents((list, err) -> {
            if (requestGeneration != generation) return;
            loading.setValue(false);
            if (err != null) {
                error.setValue(err.getMessage());
                state.setValue(ListScreenState.error(err.getMessage()));
                return;
            }
            allEvents.clear();
            if (list != null) {
                allEvents.addAll(list);
            }
            error.setValue(null); hasSuccessfulData = true;
            applyFilter();
        });
    }

    public void stopListening() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        generation++;
    }

    public void refresh() { stopListening(); startListening(); }
    public void showAuxiliaryError(@NonNull String code) { stopListening(); loading.setValue(false); error.setValue(code); state.setValue(ListScreenState.error(code)); }

    public void setSearchQuery(@Nullable String text) {
        query = text != null ? text.trim().toLowerCase(Locale.ROOT) : "";
        applyFilter();
    }

    public void setCategoryFilter(@Nullable String category) {
        this.category = category != null ? category : "";
        applyFilter();
    }

    private void applyFilter() {
        if (!hasSuccessfulData) return;
        List<Event> filtered = new ArrayList<>();
        for (Event event : allEvents) {
            boolean categoryMatch = category.isEmpty() || com.example.evently.domain.event.EventCategory.matches(category, event.getCategory());
            boolean queryMatch = query.isEmpty()
                    || (event.getTitle() != null && event.getTitle().toLowerCase(Locale.ROOT).contains(query))
                    || (event.getLocation() != null && event.getLocation().toLowerCase(Locale.ROOT).contains(query));
            if (categoryMatch && queryMatch && !isDeleted(event)) {
                filtered.add(event);
            }
        }
        events.setValue(filtered);
        state.setValue(ListScreenState.data(filtered));
    }

    private boolean isDeleted(Event event) {
        return Event.STATUS_DELETED.equals(event.getStatus());
    }

    public LiveData<List<Event>> getEvents() {
        return events;
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
