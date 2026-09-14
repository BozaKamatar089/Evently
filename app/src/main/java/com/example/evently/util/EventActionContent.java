package com.example.evently.util;

import androidx.annotation.NonNull;

import com.example.evently.data.model.Event;

/** Produces safe, presentation-only content for platform event actions. */
public final class EventActionContent {

    private EventActionContent() {
    }

    @NonNull
    public static String shareText(@NonNull Event event, String formattedDate) {
        return shareText(event, formattedDate, "");
    }

    @NonNull
    public static String shareText(@NonNull Event event, String formattedDate, String contextLine) {
        StringBuilder text = new StringBuilder();
        String cleanContext = clean(contextLine);
        appendLine(text, cleanContext.isEmpty() ? event.getTitle() : cleanContext);
        appendLine(text, formattedDate);
        appendLine(text, event.getLocation());
        appendLine(text, event.getDescription());
        return text.toString();
    }

    @NonNull
    public static String calendarTitle(@NonNull Event event) {
        return clean(event.getTitle());
    }

    @NonNull
    public static String mapQuery(@NonNull Event event) {
        return clean(event.getLocation());
    }

    private static void appendLine(StringBuilder text, String value) {
        String clean = clean(value);
        if (clean.isEmpty()) return;
        if (text.length() > 0) text.append('\n');
        text.append(clean);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
