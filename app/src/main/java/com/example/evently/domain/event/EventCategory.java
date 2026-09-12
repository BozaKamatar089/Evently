package com.example.evently.domain.event;

import java.util.Locale;

/** Stable stored identities. Aliases only interpret existing records; they never migrate them. */
public enum EventCategory {
    MUSIC("muzika", "glazba"), SPORT("sports"), EDUCATION("edukacija", "education", "radionica", "workshop"),
    CULTURE("kultura"), CHARITY("humanitarno"), VOLUNTEERING("volontiranje"),
    SOCIAL("druženje", "druzenje"), OTHER("ostalo");

    private final String[] aliases;
    EventCategory(String... aliases) { this.aliases = aliases; }
    public String storedValue() { return name(); }
    public static EventCategory fromStored(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (EventCategory category : values()) {
            if (category.name().toLowerCase(Locale.ROOT).equals(normalized)) return category;
            for (String alias : category.aliases) if (alias.equals(normalized)) return category;
        }
        return null;
    }
    public static boolean matches(String filter, String stored) {
        EventCategory category = fromStored(filter);
        EventCategory storedCategory = fromStored(stored);
        if (category != null) {
            // Unknown legacy identities are presented as Other, so the Other filter must
            // include them without replacing the raw value in Firestore.
            return category == storedCategory
                    || (category == OTHER && storedCategory == null
                    && stored != null && !stored.trim().isEmpty());
        }
        return filter != null && stored != null
                && filter.trim().equalsIgnoreCase(stored.trim());
    }

    /** Keeps an existing legacy identity unless the organizer explicitly picks a category. */
    public static String valueForEdit(EventCategory selection, String existingStoredValue) {
        if (selection != null) return selection.storedValue();
        return existingStoredValue == null ? "" : existingStoredValue;
    }
}
