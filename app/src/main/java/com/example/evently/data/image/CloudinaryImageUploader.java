package com.example.evently.data.image;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.callback.ErrorInfo;
import com.example.evently.BuildConfig;

import java.util.Map;

/**
 * Cloudinary implementacija {@link ImageUploader} koja koristi unsigned upload preset.
 * Omota Cloudinary UploadCallback u naš callback interfejs.
 */
public class CloudinaryImageUploader implements ImageUploader {

    @Override
    public void cancelUpload(@NonNull String requestId) {
        MediaManager.get().cancelRequest(requestId);
    }

    @Nullable
    @Override
    public String uploadImage(@NonNull Uri imageUri, @NonNull OnUploadCallback callback) {
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isEmpty()
                || BuildConfig.CLOUDINARY_UPLOAD_PRESET.isEmpty()) {
            callback.onError(new ImageUploadException(
                    ImageUploadException.Kind.CONFIGURATION_MISSING,
                    "Cloudinary cloud name or unsigned upload preset is missing"));
            return null;
        }

        try {
            java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean();
            // Kreiramo Cloudinary UploadCallback internu
            UploadCallback cloudinaryCallback = new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    // nema op
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    if (!finished.get() && totalBytes > 0) {
                        int percent = (int) ((bytes * 100) / totalBytes);
                        callback.onUploadProgress(percent);
                    }
                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    if (!finished.compareAndSet(false, true)) return;
                    Object secureUrl = resultData.get("secure_url");
                    if (secureUrl == null || secureUrl.toString().isEmpty()) {
                        callback.onError(new ImageUploadException(
                                ImageUploadException.Kind.INVALID_RESPONSE,
                                "Cloudinary response did not include secure_url"));
                    } else callback.onSuccess(secureUrl.toString());
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    if (!finished.compareAndSet(false, true)) return;
                    callback.onError(new ImageUploadException(
                            ImageUploadException.Kind.REMOTE_FAILURE,
                            "Cloudinary upload rejected the request"));
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                    if (!finished.compareAndSet(false, true)) return;
                    MediaManager.get().cancelRequest(requestId);
                    callback.onError(new ImageUploadException(
                            ImageUploadException.Kind.REMOTE_FAILURE,
                            "Cloudinary upload was rescheduled"));
                }
            };

            return MediaManager.get().upload(imageUri)
                    .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                    .option("resource_type", "image")
                    .callback(cloudinaryCallback)
                    .dispatch();
        } catch (Exception e) {
            callback.onError(e);
            return null;
        }
    }
}
