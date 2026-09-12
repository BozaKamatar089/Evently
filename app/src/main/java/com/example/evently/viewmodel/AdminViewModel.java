package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.AdminStats;
import com.example.evently.data.model.Event;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.domain.admin.AdminRepository;
import com.example.evently.domain.common.AccountActionPolicy;

import java.util.List;

/**
 * ViewModel za AdminDashboardActivity.
 * Drži state za statistike, verifikacije i listu događaja.
 */
public class AdminViewModel extends ViewModel {

    private final AdminRepository adminRepository;
    private final java.util.concurrent.Executor callbacks;

    private final MutableLiveData<AdminStats> stats = new MutableLiveData<>();
    private final MutableLiveData<List<VerificationRequest>> verifications = new MutableLiveData<>();
    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccess = new MutableLiveData<>();
    private final MutableLiveData<AccountScreenState> screenState =
            new MutableLiveData<>(AccountScreenState.loading());
    private final MutableLiveData<AccountScreenState> statsState =
            new MutableLiveData<>(AccountScreenState.loading());
    private final MutableLiveData<AccountScreenState> verificationState =
            new MutableLiveData<>(AccountScreenState.loading());
    private final MutableLiveData<AccountScreenState> eventsState =
            new MutableLiveData<>(AccountScreenState.loading());
    private int loadGeneration;
    private int pendingLoads;
    private int successfulLoads;
    private boolean actionRunning;

    public AdminViewModel(@NonNull AdminRepository adminRepository) {
        this(adminRepository, com.google.android.gms.tasks.TaskExecutors.MAIN_THREAD);
    }

    public AdminViewModel(@NonNull AdminRepository adminRepository,
                          @NonNull java.util.concurrent.Executor callbacks) {
        this.adminRepository = adminRepository;
        this.callbacks = callbacks;
    }

    public void loadAll() {
        if (actionRunning) return;
        final int generation = ++loadGeneration;
        pendingLoads = 3;
        successfulLoads = 0;
        loading.setValue(true);
        error.setValue(null);
        screenState.setValue(AccountScreenState.loading());
        statsState.setValue(AccountScreenState.loading());
        verificationState.setValue(AccountScreenState.loading());
        eventsState.setValue(AccountScreenState.loading());
        adminRepository.getStats()
                .addOnCompleteListener(callbacks, task -> {
                    if (generation != loadGeneration) return;
                    if (task.isSuccessful()) {
                        stats.setValue(task.getResult()); statsState.setValue(AccountScreenState.content());
                    } else statsState.setValue(AccountScreenState.error("ADMIN_STATS_ERROR"));
                    completeLoad(generation, task.isSuccessful());
                });
        adminRepository.getPendingVerifications()
                .addOnCompleteListener(callbacks, task -> {
                    if (generation != loadGeneration) return;
                    if (task.isSuccessful()) {
                        List<VerificationRequest> value=task.getResult(); verifications.setValue(value);
                        verificationState.setValue(value==null||value.isEmpty()?AccountScreenState.empty():AccountScreenState.content());
                    } else verificationState.setValue(AccountScreenState.error("ADMIN_VERIFICATIONS_ERROR"));
                    completeLoad(generation, task.isSuccessful());
                });
        adminRepository.getAllEvents()
                .addOnCompleteListener(callbacks, task -> {
                    if (generation != loadGeneration) return;
                    if (task.isSuccessful()) {
                        List<Event> value=task.getResult(); events.setValue(value);
                        eventsState.setValue(value==null||value.isEmpty()?AccountScreenState.empty():AccountScreenState.content());
                    } else eventsState.setValue(AccountScreenState.error("ADMIN_EVENTS_ERROR"));
                    completeLoad(generation, task.isSuccessful());
                });
    }

    private void completeLoad(int generation, boolean success) {
        if (generation != loadGeneration) return;
        if (success) successfulLoads++;
        if (--pendingLoads > 0) return;
        loading.setValue(false);
        if (successfulLoads == 0) {
            error.setValue("ADMIN_LOAD_ERROR");
            screenState.setValue(AccountScreenState.error("ADMIN_LOAD_ERROR"));
            return;
        }
        if (successfulLoads < 3) error.setValue("ADMIN_PARTIAL_LOAD_ERROR");
        screenState.setValue(AccountScreenState.content());
    }

    public void approveVerification(@NonNull String userId, @NonNull String orgName) {
        if (actionRunning) return;
        actionRunning = true;
        final int generation = loadGeneration;
        loading.setValue(true);
        adminRepository.approveVerification(userId, orgName)
                .addOnSuccessListener(aVoid -> {
                    if (generation != loadGeneration) return;
                    actionRunning = false;
                    loading.setValue(false);
                    actionSuccess.setValue(VerificationRequest.STATUS_APPROVED);
                    loadAll();
                })
                .addOnFailureListener(e -> {
                    if (generation != loadGeneration) return;
                    actionRunning = false;
                    loading.setValue(false);
                    error.setValue("ADMIN_ACTION_FAILED");
                    screenState.setValue(AccountScreenState.content());
                });
    }

    public void rejectVerification(@NonNull String userId, @NonNull String orgName,
                                   @NonNull String reason) {
        if (!AccountActionPolicy.hasRejectReason(reason)) {
            error.setValue("ADMIN_REJECT_REASON_REQUIRED");
            return;
        }
        if (actionRunning) return;
        actionRunning = true;
        final int generation = loadGeneration;
        loading.setValue(true);
        adminRepository.rejectVerification(userId, orgName, reason)
                .addOnSuccessListener(aVoid -> {
                    if (generation != loadGeneration) return;
                    actionRunning = false;
                    loading.setValue(false);
                    actionSuccess.setValue(VerificationRequest.STATUS_REJECTED);
                    loadAll();
                })
                .addOnFailureListener(e -> {
                    if (generation != loadGeneration) return;
                    actionRunning = false;
                    loading.setValue(false);
                    error.setValue("ADMIN_ACTION_FAILED");
                    screenState.setValue(AccountScreenState.content());
                });
    }

    public LiveData<AdminStats> getStats() { return stats; }
    public void clearSession() {
        loadGeneration++; actionRunning = false;
        stats.setValue(null); verifications.setValue(java.util.Collections.emptyList());
        events.setValue(java.util.Collections.emptyList()); loading.setValue(false);
        screenState.setValue(AccountScreenState.loading());
        statsState.setValue(AccountScreenState.loading()); verificationState.setValue(AccountScreenState.loading());
        eventsState.setValue(AccountScreenState.loading());
    }
    @Override protected void onCleared() { clearSession(); super.onCleared(); }
    public LiveData<List<VerificationRequest>> getVerifications() { return verifications; }
    public LiveData<List<Event>> getEvents() { return events; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getActionSuccess() { return actionSuccess; }
    public LiveData<AccountScreenState> getScreenState() { return screenState; }
    public LiveData<AccountScreenState> getStatsState() { return statsState; }
    public LiveData<AccountScreenState> getVerificationState() { return verificationState; }
    public LiveData<AccountScreenState> getEventsState() { return eventsState; }
}
