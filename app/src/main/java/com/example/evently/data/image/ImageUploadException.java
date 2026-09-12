package com.example.evently.data.image;

import androidx.annotation.NonNull;

/** Stable upload failure category. Raw Cloudinary/network details never reach UI copy. */
public final class ImageUploadException extends Exception {
    public enum Kind { CONFIGURATION_MISSING, REMOTE_FAILURE, INVALID_RESPONSE }
    private final Kind kind;
    public ImageUploadException(@NonNull Kind kind, @NonNull String diagnostic) {
        super(diagnostic); this.kind = kind;
    }
    @NonNull public Kind getKind() { return kind; }
}
