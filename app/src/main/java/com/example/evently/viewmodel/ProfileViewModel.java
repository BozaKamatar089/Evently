package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.evently.data.model.User;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.domain.user.UserRepository;
import com.example.evently.domain.verification.VerificationRepository;
import com.example.evently.domain.session.SessionRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel za ProfileFragment.
 * Učitava korisnika i status verifikacije; podržava update profila i logout.
 */
public class ProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final SessionRepository sessionRepository;

    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<VerificationRequest> verificationRequest = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> logoutDone = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> profileUpdated = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<AccountScreenState> screenState =
            new MutableLiveData<>(AccountScreenState.loading());

    private ListenerRegistration userListener;
    private ListenerRegistration verificationListener;
    private String currentUid;
    private boolean userResolved;
    private boolean verificationResolved;
    private int accountGeneration;
    private boolean loadFailed;

    public ProfileViewModel(@NonNull UserRepository userRepository,
                            @NonNull VerificationRepository verificationRepository,
                            @NonNull SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.sessionRepository = sessionRepository;
    }

    public void startListening(String uid) {
        if (uid == null || uid.isEmpty()) return;
        if (uid.equals(currentUid) && userListener != null) return;
        stopListening();
        currentUid = uid;
        final int generation = accountGeneration;
        userResolved = false;
        verificationResolved = false;
        loadFailed = false;
        error.setValue(null);
        profileUpdated.setValue(false);

        loading.setValue(true);
        screenState.setValue(AccountScreenState.loading());
        userListener = userRepository.listenUser(uid, (u, err) -> {
            if (generation != accountGeneration) return;
            userResolved = true;
            if (err != null) {
                loadFailed = true;
                error.setValue("PROFILE_LOAD_ERROR");
                loading.setValue(false);
                screenState.setValue(AccountScreenState.error("PROFILE_LOAD_ERROR"));
                return;
            }
            user.setValue(u);
            resolveInitialLoad();
        });

        verificationListener = verificationRepository.listenMyRequest((request, err) -> {
            if (generation != accountGeneration) return;
            verificationResolved = true;
            if (err != null) {
                loadFailed = true;
                error.setValue("PROFILE_LOAD_ERROR");
                loading.setValue(false);
                screenState.setValue(AccountScreenState.error("PROFILE_LOAD_ERROR"));
                return;
            }
            verificationRequest.setValue(request);
            resolveInitialLoad();
        });
    }

    private void resolveInitialLoad() {
        if (loadFailed || !userResolved || !verificationResolved) return;
        loading.setValue(false);
        screenState.setValue(user.getValue() == null
                ? AccountScreenState.empty() : AccountScreenState.content());
    }

    public void retry() {
        String uid = currentUid;
        stopListenersOnly();
        currentUid = null;
        if (uid != null && !uid.isEmpty()) startListening(uid);
    }

    private void stopListenersOnly() {
        if (userListener != null) { userListener.remove(); userListener = null; }
        if (verificationListener != null) { verificationListener.remove(); verificationListener = null; }
    }

    public void stopListening() {
        accountGeneration++;
        stopListenersOnly();
        currentUid = null;
        user.setValue(null);
        verificationRequest.setValue(null);
        loading.setValue(false);
        screenState.setValue(AccountScreenState.authRequired());
        saving.setValue(false);
    }

    public void updateProfile(String name, String bio, String phone, String photoUrl) {
        if (Boolean.TRUE.equals(saving.getValue())) return;
        String uid = currentUid;
        if (uid == null || uid.isEmpty()) {
            error.setValue("AUTH_REQUIRED");
            return;
        }
        saving.setValue(true);
        error.setValue(null);
        final int generation = accountGeneration;
        Map<String, Object> fields = new HashMap<>();
        if (name != null) fields.put("name", name);
        if (bio != null) fields.put("bio", bio);
        if (phone != null) fields.put("phone", phone);
        if (photoUrl != null) fields.put("photoUrl", photoUrl);

        userRepository.updateUser(uid, fields)
                .addOnSuccessListener(v -> {
                    if (generation != accountGeneration) return;
                    saving.setValue(false);
                    profileUpdated.setValue(true);
                })
                .addOnFailureListener(e -> {
                    if (generation != accountGeneration) return;
                    saving.setValue(false);
                    error.setValue("PROFILE_SAVE_ERROR");
                });
    }

    public void logout() {
        stopListening();
        sessionRepository.logout();
        logoutDone.setValue(true);
    }

    public LiveData<User> getUser() { return user; }
    public LiveData<VerificationRequest> getVerificationRequest() { return verificationRequest; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getLogoutDone() { return logoutDone; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getProfileUpdated() { return profileUpdated; }
    public LiveData<Boolean> getSaving() { return saving; }
    public LiveData<AccountScreenState> getScreenState() { return screenState; }
    public ProfileScreenState snapshot(com.example.evently.domain.session.SessionState session, String adminUid) {
        return ProfileScreenState.resolve(session, currentUid, screenState.getValue(),
                user.getValue(), verificationRequest.getValue(), adminUid);
    }

    @Override
    protected void onCleared() {
        stopListening();
        super.onCleared();
    }
}
