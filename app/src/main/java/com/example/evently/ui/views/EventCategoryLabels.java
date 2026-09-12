package com.example.evently.ui.views;

import android.content.Context;
import com.example.evently.R;
import com.example.evently.domain.event.EventCategory;

/** Shared presentation for selector, card and details; storage never receives these labels. */
public final class EventCategoryLabels {
    private EventCategoryLabels() { }
    public static String label(Context context, EventCategory category) {
        switch (category) {
            case MUSIC: return context.getString(R.string.category_music);
            case SPORT: return context.getString(R.string.category_sport);
            case EDUCATION: return context.getString(R.string.category_education);
            case CULTURE: return context.getString(R.string.category_culture);
            case CHARITY: return context.getString(R.string.category_charity);
            case VOLUNTEERING: return context.getString(R.string.category_volunteering);
            case SOCIAL: return context.getString(R.string.category_social);
            default: return context.getString(R.string.category_other);
        }
    }
    public static String display(Context context, String stored) {
        EventCategory category = EventCategory.fromStored(stored);
        return category == null ? label(context, EventCategory.OTHER) : label(context, category);
    }
    public static String[] choices(Context context) {
        EventCategory[] values = EventCategory.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = label(context, values[i]);
        return labels;
    }
}
