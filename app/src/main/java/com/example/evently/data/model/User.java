package com.example.evently.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Korisnički model koji odgovara Firestore kolekciji {@code Users/{uid}}.
 * Polja prate canonical šemu i ne smiju se mijenjati bez dozvole.
 */
public class User {

    public static final String ROLE_PARTICIPANT = "Participant";
    public static final String ROLE_ORGANIZER = "Organizer";

    private final String uid;
    private String name;
    private String email;
    private String photoUrl;
    private String bio;
    private String phone;
    private String role;
    private boolean isVerified;
    private String verificationStatus;
    private String rejectionReason;
    private long memberSince;

    public User(@NonNull String uid) {
        this.uid = uid;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public long getMemberSince() {
        return memberSince;
    }

    public void setMemberSince(long memberSince) {
        this.memberSince = memberSince;
    }

    /** Beznačajna polja (name, email) omogućavaju kreiranje dokumenta preko Google ulaska. */
    public Map<String, Object> toCreateMap() {
        Map<String, Object> map = new HashMap<>();
        putNonNull(map, "name", name);
        putNonNull(map, "email", email);
        putNonNull(map, "photoUrl", photoUrl);
        map.put("phone", phone != null ? phone : "");
        map.put("role", role != null ? role : ROLE_PARTICIPANT);
        map.put("isVerified", isVerified);
        map.put("verificationStatus", verificationStatus != null ? verificationStatus : "none");
        map.put("memberSince", memberSince);
        return map;
    }

    private static void putNonNull(Map<String, Object> map, String key, @Nullable String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
