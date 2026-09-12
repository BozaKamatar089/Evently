package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.domain.event.EventRepository;

public class HomeViewModelFactory implements ViewModelProvider.Factory {

    private final EventRepository eventRepository;

    public HomeViewModelFactory(@NonNull EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            return (T) new HomeViewModel(eventRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}