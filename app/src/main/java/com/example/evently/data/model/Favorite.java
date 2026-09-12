package com.example.evently.data.model;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Favorite model koji odgovara Firestore kolekciji {@code favorites/{id}}.
 * ID dokumenta je {@code userId_eventId} (konsistentno sa Registrations).
 * Šema je canonical i ne smije se mijenjati bez dozvole.
 */
public class Favorite {

    private final String id;
    private String userId;
    private String eventId;
    private long createdAt;

    public Favorite(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toCreateMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("eventId", eventId);
        map.put("createdAt", createdAt);
        return map;
    }
}