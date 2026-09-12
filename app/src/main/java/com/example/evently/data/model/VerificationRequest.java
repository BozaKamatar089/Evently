package com.example.evently.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Model za zahtjev verifikacije organizatora.
 * Firestore kolekcija: {@code VerificationRequests/{id}}.
 * ID dokumenta = userId (jedan zahtjev po korisniku).
 */
public class VerificationRequest {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private final String id;
    private String userId;
    private String orgName;
    private String documentUrl;
    private String status;
    private String rejectionReason;
    private long createdAt;
    private String adminId;

    public VerificationRequest(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() { return id; }

    @Nullable
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Nullable
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }

    @Nullable
    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    @Nullable
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Nullable
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Nullable
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    @NonNull
    public Map<String, Object> toCreateMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("orgName", orgName);
        map.put("documentUrl", documentUrl);
        map.put("status", STATUS_PENDING);
        map.put("createdAt", System.currentTimeMillis());
        return map;
    }
}
