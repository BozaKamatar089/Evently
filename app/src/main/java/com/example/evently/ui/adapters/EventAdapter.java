package com.example.evently.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.evently.R;
import com.example.evently.data.model.Event;
import com.example.evently.databinding.ItemEventBinding;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.text.DateFormat;
import java.util.TimeZone;

public class EventAdapter extends ListAdapter<Event, EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(@NonNull Event event);
    }
    public interface OnFavoriteClickListener { void onFavoriteClick(@NonNull Event event); }

    private final OnEventClickListener listener;
    private final OnFavoriteClickListener favoriteListener;
    private final Set<String> favoriteEventIds = new HashSet<>();

    public EventAdapter(@NonNull OnEventClickListener listener) {
        this(listener, null);
    }
    public EventAdapter(@NonNull OnEventClickListener listener, OnFavoriteClickListener favoriteListener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.favoriteListener = favoriteListener;
    }

    static final DiffUtil.ItemCallback<Event> DIFF_CALLBACK = new DiffUtil.ItemCallback<Event>() {
        @Override
        public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return !rowFieldsChanged(oldItem, newItem) && !capacityFieldsChanged(oldItem, newItem);
        }

        @Override
        @Nullable
        public Object getChangePayload(@NonNull Event oldItem, @NonNull Event newItem) {
            // A targeted payload is safe only when the only changed fields are covered by the
            // capacity/status bind block. If any row field differs, DiffUtil gets null and the
            // adapter performs a full rebind instead.
            if (rowFieldsChanged(oldItem, newItem)) {
                return null;
            }
            if (capacityFieldsChanged(oldItem, newItem)) {
                return "CAPACITY_STATUS_CHANGED";
            }
            return null;
        }

        /**
         * True when any field that renders outside the capacity/status block differs
         * (identity, context, image or organizer/verification data). Such a change
         * requires a full row rebind.
         */
        private boolean rowFieldsChanged(@NonNull Event oldItem, @NonNull Event newItem) {
            return !sameNullableText(oldItem.getTitle(), newItem.getTitle())
                    || !sameNullableText(oldItem.getDescription(), newItem.getDescription())
                    || !sameNullableText(oldItem.getCategory(), newItem.getCategory())
                    || oldItem.getDateLong() != newItem.getDateLong()
                    || !sameNullableText(oldItem.getDisplayDate(), newItem.getDisplayDate())
                    || !sameNullableText(oldItem.getLocation(), newItem.getLocation())
                    || !sameNullableText(oldItem.getImageUrl(), newItem.getImageUrl())
                    || !sameNullableText(oldItem.getOrganizerId(), newItem.getOrganizerId())
                    || !sameNullableText(oldItem.getOrganizerName(), newItem.getOrganizerName())
                    || oldItem.isOrganizerVerified() != newItem.isOrganizerVerified()
                    || !sameNullableText(oldItem.getStatus(), newItem.getStatus())
                    || oldItem.getCreatedAt() != newItem.getCreatedAt();
        }

        /**
         * True when a difference is confined to capacity-relevant fields that the
         * targeted {@code "CAPACITY_STATUS_CHANGED"} bind block re-renders.
         */
        private boolean capacityFieldsChanged(@NonNull Event oldItem, @NonNull Event newItem) {
            return oldItem.getMaxParticipants() != newItem.getMaxParticipants()
                    || oldItem.getCurrentParticipants() != newItem.getCurrentParticipants()
                    || oldItem.getMaxVolunteers() != newItem.getMaxVolunteers()
                    || oldItem.getCurrentVolunteers() != newItem.getCurrentVolunteers()
                    || oldItem.isRegistrationsOpen() != newItem.isRegistrationsOpen();
        }

        private boolean sameNullableText(@Nullable String left, @Nullable String right) {
            return left == null ? right == null : left.equals(right);
        }
    };

    public void setFavorite(@NonNull String eventId, boolean favorite) {
        if (favorite) favoriteEventIds.add(eventId); else favoriteEventIds.remove(eventId);
        // Find position and notify only that item
        int position = findPositionById(eventId);
        if (position != -1) {
            notifyItemChanged(position, "FAVORITE_CHANGED");
        }
    }

    public void setFavoriteIds(@NonNull Set<String> ids) {
        // Diff the previous and new favorite sets against the currently displayed list and
        // rebind only rows whose favorite membership actually flipped. This avoids a full
        // notifyDataSetChanged() (and with it a Glide reload of every visible image) on each
        // favorites snapshot, while still keeping the realtime listener authoritative for
        // the stored set. O(n) in list size; no mutation of the DiffUtil-managed list.
        List<String> orderedIds = new ArrayList<>();
        for (Event event : getCurrentList()) {
            orderedIds.add(event.getId());
        }
        Set<String> changed = changedFavoriteIds(favoriteEventIds, ids, orderedIds);
        favoriteEventIds.clear();
        favoriteEventIds.addAll(ids);
        if (changed.isEmpty()) {
            return;
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            if (changed.contains(orderedIds.get(i))) {
                notifyItemChanged(i, "FAVORITE_CHANGED");
            }
        }
    }

    /**
     * Pure helper: returns the subset of {@code orderedIds} whose favorite membership
     * differs between {@code previous} and {@code next}. Ids not present in
     * {@code orderedIds} are ignored (they are not rendered by this adapter instance).
     */
    static Set<String> changedFavoriteIds(@NonNull Set<String> previous,
                                          @NonNull Set<String> next,
                                          @NonNull List<String> orderedIds) {
        Set<String> changed = new HashSet<>();
        for (String id : orderedIds) {
            if (id == null) {
                continue;
            }
            if (previous.contains(id) != next.contains(id)) {
                changed.add(id);
            }
        }
        return changed;
    }

    private int findPositionById(@NonNull String eventId) {
        for (int i = 0; i < getCurrentList().size(); i++) {
            if (getCurrentList().get(i).getId().equals(eventId)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventBinding binding = ItemEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = getItem(position);
        holder.bind(event, null);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position, @NonNull List<Object> payloads) {
        Event event = getItem(position);
        if (!payloads.isEmpty()) {
            holder.bind(event, payloads);
        } else {
            onBindViewHolder(holder, position);
        }
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final ItemEventBinding binding;

        EventViewHolder(@NonNull ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Event event, @Nullable List<Object> payloads) {
            boolean fullRebind = payloads == null || payloads.isEmpty();
            boolean favoriteOnly = fullRebind == false && payloads.size() == 1 && "FAVORITE_CHANGED".equals(payloads.get(0));
            boolean capacityStatus = fullRebind == false && payloads.size() == 1 && "CAPACITY_STATUS_CHANGED".equals(payloads.get(0));

            if (fullRebind) {
                Glide.with(itemView).clear(binding.ivEventImage);
                binding.tvEventTitle.setText(event.getTitle() != null ? event.getTitle() : "");
                binding.getRoot().setContentDescription(itemView.getContext().getString(
                        R.string.e_event_open_description,
                        event.getTitle() != null ? event.getTitle() : ""));

                if (event.getDateLong() > 0) {
                    DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    binding.tvEventDate.setText(format.format(new java.util.Date(event.getDateLong())));
                } else {
                    binding.tvEventDate.setText("");
                }
                if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                    binding.tvEventLocation.setText(event.getLocation());
                } else {
                    binding.tvEventLocation.setText("");
                }

                if (event.getCategory() != null && !event.getCategory().isEmpty()) {
                    binding.chipCategory.setText(com.example.evently.ui.views.EventCategoryLabels.display(binding.getRoot().getContext(), event.getCategory()));
                    binding.chipCategory.setVisibility(View.VISIBLE);
                } else {
                    binding.chipCategory.setText("");
                    binding.chipCategory.setVisibility(View.GONE);
                }
            }

            if (fullRebind || capacityStatus) {
                long max = event.getMaxParticipants();
                long current = event.getCurrentParticipants();
                long seatsLeft = Math.max(0, max - current);
                binding.tvEventSpots.setText(
                itemView.getResources().getQuantityString(
                        R.plurals.event_available_short,
                        (int) Math.min(seatsLeft, Integer.MAX_VALUE),
                        seatsLeft));

                LinearProgressIndicator progress = binding.progressCapacity;
                if (max > 0) {
                    int progressValue = (int) Math.max(0, Math.min(100, (current * 100.0) / max));
                    progress.setProgressCompat(progressValue, true);
                    if (seatsLeft <= 0) {
                        binding.tvEventCapacityLabel.setText(R.string.event_capacity_full);
                    } else {
                        binding.tvEventCapacityLabel.setText("");
                    }
                } else {
                    progress.setProgressCompat(0, false);
                    binding.tvEventCapacityLabel.setText("");
                }
            }

            if (fullRebind) {
                if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(event.getImageUrl())
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .placeholder(R.drawable.event_placeholder)
                            .error(R.drawable.event_placeholder)
                            .centerCrop()
                            .into(binding.ivEventImage);
                } else {
                    binding.ivEventImage.setImageResource(R.drawable.event_placeholder);
                }

                setupFavoriteButton(event);
            }

            if (favoriteOnly || fullRebind) {
                updateFavoriteButton(event);
            }

            if (fullRebind) {
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEventClick(event);
                    }
                });
            }
        }

        private void setupFavoriteButton(@NonNull Event event) {
            ImageView favIcon = binding.ivFavorite;
            if (favIcon == null) return;
            favIcon.setVisibility(favoriteListener != null ? View.VISIBLE : View.GONE);
            updateFavoriteButton(event);
            favIcon.setOnClickListener(v -> { if (favoriteListener != null) favoriteListener.onFavoriteClick(event); });
        }

        private void updateFavoriteButton(@NonNull Event event) {
            ImageView favIcon = binding.ivFavorite;
            if (favIcon == null) return;
            boolean isFavorite = favoriteEventIds.contains(event.getId());
            favIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
            favIcon.setContentDescription(itemView.getContext().getString(isFavorite
                    ? R.string.event_remove_favorite : R.string.event_add_favorite));
        }
    }

    @Override public void onViewRecycled(@NonNull EventViewHolder holder) {
        Glide.with(holder.itemView).clear(holder.binding.ivEventImage);
        holder.binding.ivEventImage.setImageDrawable(null);
        holder.binding.getRoot().setOnClickListener(null);
        holder.binding.ivFavorite.setOnClickListener(null);
        super.onViewRecycled(holder);
    }

}