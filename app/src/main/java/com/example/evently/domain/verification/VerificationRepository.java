package com.example.evently.domain.verification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.data.model.VerificationRequest;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Repository za zahtjeve verifikacije organizatora.
 * Sva VerificationRequests komunikacija ide kroz ovaj repository.
 * Request je u {@code VerificationRequests/{userId}}. Povezani
 * {@code Users.verificationStatus} se mijenja samo atomarno uz request.
 */
public interface VerificationRepository {

    /** Kreira zahtjev za verifikaciju (id = userId). */
    Task<Void> submitRequest(@NonNull String orgName, @NonNull String documentUrl);

    /** Briše (odustaje od) zahtjeva — dozvoljeno samo dok je pending. */
    Task<Void> cancelRequest(@NonNull String requestId);

    /** Sluša svoj (moj) zahtjev za verifikaciju. */
    ListenerRegistration listenMyRequest(@NonNull OnRequestListener listener);

    interface OnRequestListener {
        void onRequest(@Nullable VerificationRequest request, @Nullable Exception error);
    }
}
