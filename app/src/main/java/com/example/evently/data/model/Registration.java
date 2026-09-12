package com.example.evently.data.model;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Registration model koji odgovara Firestore kolekciji {@code Registrations/{id}}.
 * ID dokumenta je {@code userId_eventId} (vidjeti Firestore rules).
 * Šema je canonical i ne smije se mijenjati bez dozvole.
 */
public class Registration {

    public static final String TYPE_PARTICIPANT = "participant";
    public static final String TYPE_VOLUNTEER = "volunteer";

    public static final String STATUS_REGISTERED = "registered";

    private final String id;
    private String eventId;
    private String userId;
    private String type;
    private String status;
    private long registeredAt;
    private String organizerId;

    public Registration(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(long registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }
}
