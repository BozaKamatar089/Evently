package com.example.evently.data.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Event model koji odgovara Firestore kolekciji {@code Events/{eventId}}.
 * Šema je canonical i ne smije se mijenjati bez dozvole.
 * {@code dateLong} je epoch millis; {@code displayDate} služi samo za prikaz.
 */
public class Event {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String STATUS_DELETED = "deleted";

    private final String id;
    private String title;
    private String description;
    private String category;
    private long dateLong;
    private String displayDate;
    private String location;
    private String imageUrl;
    private String organizerId;
    private String organizerName;
    private boolean organizerVerified;
    private long maxParticipants;
    private long currentParticipants;
    private long maxVolunteers;
    private long currentVolunteers;
    private boolean registrationsOpen;
    private String status;
    private long createdAt;

    public Event(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getDateLong() {
        return dateLong;
    }

    public void setDateLong(long dateLong) {
        this.dateLong = dateLong;
    }

    public String getDisplayDate() {
        return displayDate;
    }

    public void setDisplayDate(String displayDate) {
        this.displayDate = displayDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public boolean isOrganizerVerified() {
        return organizerVerified;
    }

    public void setOrganizerVerified(boolean organizerVerified) {
        this.organizerVerified = organizerVerified;
    }

    public long getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(long maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public long getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(long currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public long getMaxVolunteers() {
        return maxVolunteers;
    }

    public void setMaxVolunteers(long maxVolunteers) {
        this.maxVolunteers = maxVolunteers;
    }

    public long getCurrentVolunteers() {
        return currentVolunteers;
    }

    public void setCurrentVolunteers(long currentVolunteers) {
        this.currentVolunteers = currentVolunteers;
    }

    public boolean isRegistrationsOpen() {
        return registrationsOpen;
    }

    public void setRegistrationsOpen(boolean registrationsOpen) {
        this.registrationsOpen = registrationsOpen;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getSeatsLeft() {
        return (int) Math.max(0, maxParticipants - currentParticipants);
    }

    /** Mapa samo za CREATE — sadrži sva polja potrebna pri kreiranju. */
    public Map<String, Object> toCreateMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("category", category);
        map.put("dateLong", dateLong);
        map.put("displayDate", displayDate);
        map.put("location", location);
        if (imageUrl != null) {
            map.put("imageUrl", imageUrl);
        }
        map.put("organizerId", organizerId);
        map.put("organizerName", organizerName);
        map.put("organizerVerified", organizerVerified);
        map.put("maxParticipants", maxParticipants);
        map.put("currentParticipants", 0L);
        map.put("maxVolunteers", maxVolunteers);
        map.put("currentVolunteers", 0L);
        map.put("registrationsOpen", registrationsOpen);
        map.put("status", status);
        map.put("createdAt", createdAt);
        return map;
    }

    /**
     * Mapa samo sa EDITABILNIM poljima za UPDATE.
     * Nikada ne sadrži sistemske/zaključane vrijednosti (counters, organizerId, createdAt).
     */
    public Map<String, Object> toUpdateMap() {
        Map<String, Object> map = new HashMap<>();
        putUpdateValue(map, "title", title);
        putUpdateValue(map, "description", description);
        putUpdateValue(map, "category", category);
        map.put("dateLong", dateLong);
        putUpdateValue(map, "displayDate", displayDate);
        putUpdateValue(map, "location", location);
        putUpdateValue(map, "imageUrl", imageUrl);
        map.put("maxParticipants", maxParticipants);
        map.put("maxVolunteers", maxVolunteers);
        return map;
    }

    private static void putUpdateValue(Map<String, Object> map, String key, String value) {
        map.put(key, value != null ? value : FieldValue.delete());
    }
}
