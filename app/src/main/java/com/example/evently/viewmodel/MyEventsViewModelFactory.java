package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.registration.RegistrationRepository;

public class MyEventsViewModelFactory implements ViewModelProvider.Factory {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public MyEventsViewModelFactory(@NonNull EventRepository eventRepository,
                                    @NonNull RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MyEventsViewModel.class)) {
            return (T) new MyEventsViewModel(eventRepository, registrationRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
