package com.example.evently.domain.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** Immutable owner-independent state for Home event discovery controls. */
public final class HomeDiscoveryState {

    public enum DateFilter {
        UPCOMING,
        TODAY,
        THIS_WEEK,
        ALL
    }

    public enum SortOrder {
        SOONEST,
        NEWEST
    }

    private final String query;
    private final EventCategory category;
    private final DateFilter dateFilter;
    private final SortOrder sortOrder;

    private HomeDiscoveryState(@NonNull String query, @Nullable EventCategory category,
                               @NonNull DateFilter dateFilter, @NonNull SortOrder sortOrder) {
        this.query = query;
        this.category = category;
        this.dateFilter = dateFilter;
        this.sortOrder = sortOrder;
    }

    @NonNull
    public static HomeDiscoveryState defaults() {
        return new HomeDiscoveryState("", null, DateFilter.UPCOMING, SortOrder.SOONEST);
    }

    @NonNull
    public HomeDiscoveryState withQuery(@Nullable String value) {
        return new HomeDiscoveryState(value == null ? "" : value, category, dateFilter, sortOrder);
    }

    @NonNull
    public HomeDiscoveryState withCategory(@Nullable EventCategory value) {
        return new HomeDiscoveryState(query, value, dateFilter, sortOrder);
    }

    @NonNull
    public HomeDiscoveryState withDateFilter(@NonNull DateFilter value) {
        return new HomeDiscoveryState(query, category, value, sortOrder);
    }

    @NonNull
    public HomeDiscoveryState withSortOrder(@NonNull SortOrder value) {
        return new HomeDiscoveryState(query, category, dateFilter, value);
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    @Nullable
    public EventCategory getCategory() {
        return category;
    }

    @NonNull
    public DateFilter getDateFilter() {
        return dateFilter;
    }

    @NonNull
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public boolean isDefault() {
        return EventDiscoveryPolicy.normalize(query).isEmpty()
                && category == null
                && dateFilter == DateFilter.UPCOMING
                && sortOrder == SortOrder.SOONEST;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HomeDiscoveryState)) return false;
        HomeDiscoveryState that = (HomeDiscoveryState) other;
        return query.equals(that.query)
                && category == that.category
                && dateFilter == that.dateFilter
                && sortOrder == that.sortOrder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, category, dateFilter, sortOrder);
    }
}
