package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.evently.domain.auth.AccountSecurityPolicy;
import com.example.evently.domain.auth.AuthIdentity;
import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.util.AuthErrorCodes;

/** Retains the reauthentication + password-change operation across recreation. */
public final class AccountSecurityViewModel extends ViewModel {
    public enum Status { IDLE, LOADING, SUCCESS, ERROR }
    private final AuthRepository repository;
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AccountSecurityViewModel(@NonNull AuthRepository repository) { this.repository = repository; }
    public AuthIdentity getIdentity() { return repository.getCurrentUser(); }
    public LiveData<Status> getStatus() { return status; }
    public LiveData<String> getError() { return error; }

    @NonNull public AccountSecurityPolicy.PasswordResult changePassword(
            String current, String next, String confirmation) {
        AccountSecurityPolicy.PasswordResult validation =
                AccountSecurityPolicy.validatePasswordChange(current, next, confirmation);
        if (validation != AccountSecurityPolicy.PasswordResult.VALID) return validation;
        if (status.getValue() == Status.LOADING) return validation;
        status.setValue(Status.LOADING); error.setValue(null);
        repository.changePassword(current, next).addOnCompleteListener(task -> {
            if (task.isSuccessful()) status.setValue(Status.SUCCESS);
            else { error.setValue(AuthErrorCodes.from(task.getException())); status.setValue(Status.ERROR); }
        });
        return validation;
    }
}
