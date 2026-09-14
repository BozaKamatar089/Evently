package com.example.evently.domain.event;

import androidx.annotation.NonNull;

import com.example.evently.data.model.Event;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/** Pure, deterministic discovery pipeline over events already loaded by the repository. */
public final class EventDiscoveryPolicy {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private EventDiscoveryPolicy() {
    }

    @NonNull
    public static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("");
        return WHITESPACE.matcher(withoutDiacritics.trim())
                .replaceAll(" ")
                .toLowerCase(Locale.ROOT);
    }

    @NonNull
    public static List<Event> apply(List<Event> source, @NonNull HomeDiscoveryState state,
                                    long nowMillis, @NonNull TimeZone timeZone,
                                    @NonNull Locale locale) {
        if (source == null || source.isEmpty()) return Collections.emptyList();

        String normalizedQuery = normalize(state.getQuery());
        String[] queryTokens = normalizedQuery.isEmpty()
                ? new String[0]
                : normalizedQuery.split(" ");
        DateRange dateRange = dateRange(state.getDateFilter(), nowMillis, timeZone, locale);

        List<Event> result = new ArrayList<>();
        for (Event event : source) {
            if (event == null || Event.STATUS_DELETED.equalsIgnoreCase(event.getStatus())) continue;
            if (!matchesCategory(event, state.getCategory())) continue;
            if (!dateRange.contains(event.getDateLong())) continue;
            if (!matchesQuery(event, queryTokens)) continue;
            result.add(event);
        }

        result.sort(comparator(state.getSortOrder()));
        return result;
    }

    private static boolean matchesCategory(Event event, EventCategory selectedCategory) {
        return selectedCategory == null
                || EventCategory.matches(selectedCategory.storedValue(), event.getCategory());
    }

    private static boolean matchesQuery(Event event, String[] tokens) {
        if (tokens.length == 0) return true;
        String searchable = normalize(join(
                event.getTitle(),
                event.getLocation(),
                event.getDescription(),
                EventCategory.searchText(event.getCategory())));
        for (String token : tokens) {
            if (!searchable.contains(token)) return false;
        }
        return true;
    }

    private static String join(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(value);
        }
        return result.toString();
    }

    private static Comparator<Event> comparator(HomeDiscoveryState.SortOrder sortOrder) {
        Comparator<Event> fallback = Comparator
                .comparingLong(EventDiscoveryPolicy::sortableDate)
                .thenComparing(event -> normalize(event.getTitle()))
                .thenComparing(Event::getId);

        if (sortOrder == HomeDiscoveryState.SortOrder.NEWEST) {
            return (left, right) -> {
                boolean leftHasCreatedAt = left.getCreatedAt() > 0L;
                boolean rightHasCreatedAt = right.getCreatedAt() > 0L;
                if (leftHasCreatedAt != rightHasCreatedAt) return leftHasCreatedAt ? -1 : 1;
                if (leftHasCreatedAt) {
                    int createdComparison = Long.compare(right.getCreatedAt(), left.getCreatedAt());
                    if (createdComparison != 0) return createdComparison;
                }
                return fallback.compare(left, right);
            };
        }
        return fallback;
    }

    private static long sortableDate(Event event) {
        return event.getDateLong() > 0L ? event.getDateLong() : Long.MAX_VALUE;
    }

    private static DateRange dateRange(HomeDiscoveryState.DateFilter filter, long nowMillis,
                                       TimeZone timeZone, Locale locale) {
        if (filter == HomeDiscoveryState.DateFilter.ALL) {
            return new DateRange(Long.MIN_VALUE, Long.MAX_VALUE);
        }

        Calendar start = Calendar.getInstance(timeZone, locale);
        start.setTimeInMillis(nowMillis);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        long startMarker = utcDateMarker(start, locale);

        if (filter == HomeDiscoveryState.DateFilter.UPCOMING) {
            return new DateRange(startMarker, Long.MAX_VALUE);
        }

        Calendar end = (Calendar) start.clone();
        if (filter == HomeDiscoveryState.DateFilter.TODAY) {
            end.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            int daysUntilNextWeek = (start.getFirstDayOfWeek()
                    - start.get(Calendar.DAY_OF_WEEK) + 7) % 7;
            end.add(Calendar.DAY_OF_MONTH, daysUntilNextWeek == 0 ? 7 : daysUntilNextWeek);
        }
        return new DateRange(startMarker, utcDateMarker(end, locale));
    }

    private static long utcDateMarker(Calendar localDate, Locale locale) {
        Calendar marker = Calendar.getInstance(TimeZone.getTimeZone("UTC"), locale);
        marker.clear();
        marker.set(
                localDate.get(Calendar.YEAR),
                localDate.get(Calendar.MONTH),
                localDate.get(Calendar.DAY_OF_MONTH),
                0, 0, 0);
        return marker.getTimeInMillis();
    }

    private static final class DateRange {
        private final long startInclusive;
        private final long endExclusive;

        private DateRange(long startInclusive, long endExclusive) {
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
        }

        private boolean contains(long value) {
            return value >= startInclusive && value < endExclusive;
        }
    }
}
