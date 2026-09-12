package com.example.evently.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evently.R;
import com.example.evently.data.model.Event;
import com.example.evently.databinding.ItemAdminEventBinding;

import java.util.Date;
import java.util.Objects;

/** Read-only event summary rows for the operational Admin screen. */
public final class AdminEventAdapter extends ListAdapter<Event, AdminEventAdapter.ViewHolder> {
    private static final DiffUtil.ItemCallback<Event> DIFF = new DiffUtil.ItemCallback<Event>() {
        @Override public boolean areItemsTheSame(@NonNull Event left, @NonNull Event right) {
            return Objects.equals(left.getId(), right.getId());
        }
        @Override public boolean areContentsTheSame(@NonNull Event left, @NonNull Event right) {
            return Objects.equals(left.getTitle(), right.getTitle())
                    && Objects.equals(left.getOrganizerName(), right.getOrganizerName())
                    && Objects.equals(left.getStatus(), right.getStatus())
                    && left.getDateLong() == right.getDateLong()
                    && left.getCurrentParticipants() == right.getCurrentParticipants()
                    && left.getMaxParticipants() == right.getMaxParticipants();
        }
    };

    public AdminEventAdapter() { super(DIFF); }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAdminEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminEventBinding binding;
        ViewHolder(@NonNull ItemAdminEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(@NonNull Event event) {
            binding.tvTitle.setText(event.getTitle() == null ? "" : event.getTitle());
            binding.tvOrganizer.setText(itemView.getContext().getString(R.string.admin_organizer_label,
                    event.getOrganizerName() == null ? "" : event.getOrganizerName()));
            binding.tvDate.setText(event.getDateLong() > 0
                    ? android.text.format.DateFormat.getMediumDateFormat(itemView.getContext())
                    .format(new Date(event.getDateLong())) : "");
            String status = event.getStatus() == null ? Event.STATUS_ACTIVE : event.getStatus();
            binding.tvStatus.setText(Event.STATUS_CANCELLED.equals(status)
                    ? R.string.admin_event_status_cancelled
                    : Event.STATUS_DELETED.equals(status)
                    ? R.string.admin_event_status_deleted : R.string.admin_event_status_active);
            binding.tvParticipants.setText(itemView.getContext().getString(
                    R.string.admin_participant_capacity,
                    event.getCurrentParticipants(), event.getMaxParticipants()));
        }
    }
}
