package com.example.evently.viewmodel;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.data.image.ImageUploader;
import com.example.evently.domain.event.EventRepository;
public final class EventFormViewModelFactory implements ViewModelProvider.Factory {
 private final EventRepository events; private final ImageUploader images;
 public EventFormViewModelFactory(EventRepository events,ImageUploader images){this.events=events;this.images=images;}
 @NonNull public <T extends ViewModel> T create(@NonNull Class<T> type){if(type.isAssignableFrom(EventFormViewModel.class)) return (T)new EventFormViewModel(events,images); throw new IllegalArgumentException(type.getName());}
}
