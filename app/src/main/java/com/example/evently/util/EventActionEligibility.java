package com.example.evently.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.evently.data.model.Event;
import com.example.evently.data.model.Registration;
import java.util.Calendar;

public final class EventActionEligibility {
    public enum Reason { ELIGIBLE, CLOSED, CANCELLED, DELETED, PAST, FULL, NO_VOLUNTEERS, ALREADY_REGISTERED }
    private EventActionEligibility() { }
    public static Reason forType(@NonNull Event event, @Nullable Registration existing,
                                 @NonNull String type, long now) {
        if (existing != null) return Reason.ALREADY_REGISTERED;
        if (Event.STATUS_CANCELLED.equals(event.getStatus())) return Reason.CANCELLED;
        if (Event.STATUS_DELETED.equals(event.getStatus())) return Reason.DELETED;
        if (!event.isRegistrationsOpen()) return Reason.CLOSED;
        if (!Event.STATUS_ACTIVE.equals(event.getStatus())) return Reason.CLOSED;
        // Events are day based: use the same 24-hour boundary as deployed Firestore rules.
        if (event.getDateLong() <= 0 || event.getDateLong() <= now - 86_400_000L) return Reason.PAST;
        if (!Registration.TYPE_PARTICIPANT.equals(type) && !Registration.TYPE_VOLUNTEER.equals(type))
            return Reason.CLOSED;
        if (Registration.TYPE_VOLUNTEER.equals(type)) {
            if (event.getMaxVolunteers() <= 0) return Reason.NO_VOLUNTEERS;
            return event.getCurrentVolunteers() >= event.getMaxVolunteers() ? Reason.FULL : Reason.ELIGIBLE;
        }
        return event.getCurrentParticipants() >= event.getMaxParticipants() ? Reason.FULL : Reason.ELIGIBLE;
    }

    /** Returns the event-wide registration state without treating an unused volunteer pool as capacity. */
    public static Reason overall(@NonNull Event event, long now) {
        Reason participant = forType(event, null, Registration.TYPE_PARTICIPANT, now);
        if (participant != Reason.ELIGIBLE && participant != Reason.FULL) return participant;

        Reason volunteer = forType(event, null, Registration.TYPE_VOLUNTEER, now);
        if (participant == Reason.ELIGIBLE || volunteer == Reason.ELIGIBLE) return Reason.ELIGIBLE;
        return Reason.FULL;
    }
}
