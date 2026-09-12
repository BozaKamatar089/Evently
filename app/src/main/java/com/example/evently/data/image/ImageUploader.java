package com.example.evently.data.image;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Upload slika na Cloudinary (unsigned preset). Vraća Cloudinary URL (secure_url)
 * koji se potom čuva u {@code Events/{id}.imageUrl}.
 */
public interface ImageUploader {

    /**
     * Uploaduje sliku sa {@link Uri}a.
     *
     * @return request ID za praćenje, ili null ako upload nije moguće započeti
     */
    @Nullable
    String uploadImage(@NonNull Uri imageUri, @NonNull OnUploadCallback callback);

    /** Cancels an in-flight upload owned by this screen. */
    void cancelUpload(@NonNull String requestId);

    interface OnUploadCallback {
        void onSuccess(@NonNull String secureUrl);
        void onError(@NonNull Exception error);
        void onUploadProgress(int percent);
    }
}
