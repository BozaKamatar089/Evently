package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.domain.common.Subscription;
import com.example.evently.domain.session.SessionRepository;
import com.example.evently.domain.session.SessionState;

/** Activity-scoped owner for the shared application session. */
public final class SessionViewModel extends ViewModel {
    private final SessionRepository repository;
    // Auth has not resolved yet; publishing GUEST here prematurely closes protected screens.
    private final MutableLiveData<SessionState> state = new MutableLiveData<>(SessionState.loading(""));
    private Subscription subscription;
    public SessionViewModel(@NonNull SessionRepository repository) { this.repository = repository; }
    public LiveData<SessionState> getState() { return state; }
    public void start() { if (subscription == null) subscription = repository.observe(state::setValue); }
    public void retry() {
        if (subscription != null) subscription.cancel();
        subscription = null;
        state.setValue(SessionState.loading(""));
        start();
    }
    public void logout() { repository.logout(); }
    @Override protected void onCleared() { if (subscription != null) subscription.cancel(); super.onCleared(); }
}
