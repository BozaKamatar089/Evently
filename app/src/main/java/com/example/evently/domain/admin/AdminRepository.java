package com.example.evently.domain.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.AdminStats;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.data.model.Event;
import com.google.android.gms.tasks.Task;

import java.util.List;

/**
 * Repository za Admin Dashboard funkcionalnosti.
 * Sva admin Firebase komunikacija prolazi kroz ovaj repository.
 */
public interface AdminRepository {

    Task<AdminStats> getStats();

    Task<List<VerificationRequest>> getPendingVerifications();

    Task<Void> approveVerification(@NonNull String userId, @NonNull String orgName);

    Task<Void> rejectVerification(@NonNull String userId, @NonNull String orgName, @NonNull String reason);

    Task<List<Event>> getAllEvents();

}
