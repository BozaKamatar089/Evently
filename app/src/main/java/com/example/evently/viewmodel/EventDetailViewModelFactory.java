package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.registration.RegistrationRepository;

public final class EventDetailViewModelFactory implements ViewModelProvider.Factory {
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public EventDetailViewModelFactory(@NonNull EventRepository eventRepository,
                                       @NonNull RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @NonNull @Override @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> type) {
        if (type.isAssignableFrom(EventDetailViewModel.class)) {
            return (T) new EventDetailViewModel(eventRepository, registrationRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + type.getName());
    }
}