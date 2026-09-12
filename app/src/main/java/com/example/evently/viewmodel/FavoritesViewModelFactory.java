package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.favorites.FavoriteRepository;

public class FavoritesViewModelFactory implements ViewModelProvider.Factory {

    private final EventRepository eventRepository;
    private final FavoriteRepository favoriteRepository;

    public FavoritesViewModelFactory(@NonNull EventRepository eventRepository,
                                     @NonNull FavoriteRepository favoriteRepository) {
        this.eventRepository = eventRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FavoritesViewModel.class)) {
            return (T) new FavoritesViewModel(eventRepository, favoriteRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}