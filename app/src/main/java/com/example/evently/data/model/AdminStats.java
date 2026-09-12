package com.example.evently.data.model;

/**
 * Model za admin statistike — broj korisnika, događaja i prijava.
 */
public class AdminStats {

    private final int userCount;
    private final int eventCount;
    private final int registrationCount;

    public AdminStats(int userCount, int eventCount, int registrationCount) {
        this.userCount = userCount;
        this.eventCount = eventCount;
        this.registrationCount = registrationCount;
    }

    public int getUserCount() { return userCount; }
    public int getEventCount() { return eventCount; }
    public int getRegistrationCount() { return registrationCount; }
}
