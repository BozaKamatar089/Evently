package com.example.evently.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;

import com.example.evently.R;
import com.example.evently.data.model.Event;

/** Thin platform handoff for Event Details. It never writes app or backend data. */
public final class EventPlatformActions {

    public enum Result {
        STARTED,
        MISSING_DATE,
        MISSING_LOCATION,
        NO_HANDLER
    }

    private EventPlatformActions() {
    }

    @NonNull
    public static Result share(@NonNull Context context, @NonNull Event event,
                               @NonNull String formattedDate) {
        String title = EventActionContent.calendarTitle(event);
        if (title.isEmpty()) title = context.getString(R.string.app_name);
        String contextLine = context.getString(R.string.event_action_share_intro, title);
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT,
                        EventActionContent.shareText(event, formattedDate, contextLine));
        Intent chooser = Intent.createChooser(send, context.getString(R.string.event_action_share_chooser));
        return start(context, chooser);
    }

    @NonNull
    public static Result addToCalendar(@NonNull Context context, @NonNull Event event) {
        if (event.getDateLong() <= 0L) return Result.MISSING_DATE;
        String title = EventActionContent.calendarTitle(event);
        if (title.isEmpty()) title = context.getString(R.string.app_name);
        Intent insert = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION,
                        EventActionContent.mapQuery(event))
                .putExtra(CalendarContract.Events.DESCRIPTION,
                        event.getDescription() == null ? "" : event.getDescription().trim())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDateLong())
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "UTC");
        return start(context, insert);
    }

    @NonNull
    public static Result openInMaps(@NonNull Context context, @NonNull Event event) {
        String location = EventActionContent.mapQuery(event);
        if (location.isEmpty()) return Result.MISSING_LOCATION;
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
        return start(context, new Intent(Intent.ACTION_VIEW, uri));
    }

    private static Result start(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return Result.STARTED;
        } catch (ActivityNotFoundException | SecurityException error) {
            return Result.NO_HANDLER;
        }
    }
}
