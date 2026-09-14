package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.Event;
import com.example.evently.domain.event.EventCategory;
import com.example.evently.domain.event.EventDiscoveryPolicy;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.event.HomeDiscoveryState;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class HomeViewModel extends ViewModel {

    public enum EmptyReason {
        NO_EVENTS,
        NO_UPCOMING,
        NO_SEARCH_RESULTS,
        NO_FILTER_RESULTS
    }

    interface TimeProvider {
        long now();
    }

    private final EventRepository eventRepository;
    private final TimeProvider timeProvider;
    private final TimeZone timeZone;
    private final Locale locale;

    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    private final MutableLiveData<ListScreenState> state = new MutableLiveData<>(ListScreenState.loading());
    private final MutableLiveData<HomeDiscoveryState> discoveryState =
            new MutableLiveData<>(HomeDiscoveryState.defaults());
    private final MutableLiveData<EmptyReason> emptyReason = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final List<Event> allEvents = new ArrayList<>();
    private ListenerRegistration registration;
    private int generation;
    private boolean hasSuccessfulData;

    public HomeViewModel(@NonNull EventRepository eventRepository) {
        this(eventRepository, System::currentTimeMillis, TimeZone.getDefault(), Locale.getDefault());
    }

    HomeViewModel(@NonNull EventRepository eventRepository, @NonNull TimeProvider timeProvider,
                  @NonNull TimeZone timeZone, @NonNull Locale locale) {
        this.eventRepository = eventRepository;
        this.timeProvider = timeProvider;
        this.timeZone = timeZone;
        this.locale = locale;
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
        HomeDiscoveryState current = requireDiscoveryState();
        discoveryState.setValue(current.withQuery(text));
        applyFilter();
    }

    public void setCategoryFilter(@Nullable EventCategory category) {
        discoveryState.setValue(requireDiscoveryState().withCategory(category));
        applyFilter();
    }

    public void setDateFilter(@NonNull HomeDiscoveryState.DateFilter dateFilter) {
        discoveryState.setValue(requireDiscoveryState().withDateFilter(dateFilter));
        applyFilter();
    }

    public void setSortOrder(@NonNull HomeDiscoveryState.SortOrder sortOrder) {
        discoveryState.setValue(requireDiscoveryState().withSortOrder(sortOrder));
        applyFilter();
    }

    public void resetDiscovery() {
        discoveryState.setValue(HomeDiscoveryState.defaults());
        applyFilter();
    }

    private void applyFilter() {
        if (!hasSuccessfulData) return;
        HomeDiscoveryState current = requireDiscoveryState();
        List<Event> filtered = EventDiscoveryPolicy.apply(
                allEvents, current, timeProvider.now(), timeZone, locale);
        events.setValue(filtered);
        emptyReason.setValue(filtered.isEmpty() ? determineEmptyReason(current) : null);
        state.setValue(ListScreenState.data(filtered));
    }

    @NonNull
    private HomeDiscoveryState requireDiscoveryState() {
        HomeDiscoveryState current = discoveryState.getValue();
        return current != null ? current : HomeDiscoveryState.defaults();
    }

    @NonNull
    private EmptyReason determineEmptyReason(@NonNull HomeDiscoveryState current) {
        if (allEvents.isEmpty()) return EmptyReason.NO_EVENTS;
        if (!EventDiscoveryPolicy.normalize(current.getQuery()).isEmpty()) {
            return EmptyReason.NO_SEARCH_RESULTS;
        }
        if (current.getCategory() == null
                && current.getDateFilter() == HomeDiscoveryState.DateFilter.UPCOMING) {
            return EmptyReason.NO_UPCOMING;
        }
        return EmptyReason.NO_FILTER_RESULTS;
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
    public LiveData<HomeDiscoveryState> getDiscoveryState() { return discoveryState; }
    public LiveData<EmptyReason> getEmptyReason() { return emptyReason; }

    @Override
    protected void onCleared() {
        stopListening();
        super.onCleared();
    }
}
