package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.domain.favorites.FavoriteRepository;

public final class FavoriteActionViewModelFactory implements ViewModelProvider.Factory {
    private final FavoriteRepository repository;
    public FavoriteActionViewModelFactory(@NonNull FavoriteRepository repository) { this.repository = repository; }
    @NonNull @Override @SuppressWarnings("unchecked") public <T extends ViewModel> T create(@NonNull Class<T> type) {
        if (type.isAssignableFrom(FavoriteActionViewModel.class)) return (T) new FavoriteActionViewModel(repository);
        throw new IllegalArgumentException("Unknown ViewModel: " + type.getName());
    }
}
