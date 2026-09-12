package com.example.evently.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evently.R;
import com.example.evently.data.model.Registration;
import com.example.evently.databinding.ItemRegistrationBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/** Queue rows for registration work; the Activity owns navigation and decisions. */
public final class RegistrationAdapter extends ListAdapter<Registration, RegistrationAdapter.RegViewHolder> {

    public interface OnRemoveClickListener {
        void onRemove(@NonNull Registration registration);
    }

    private static final DiffUtil.ItemCallback<Registration> DIFF = new DiffUtil.ItemCallback<Registration>() {
        @Override public boolean areItemsTheSame(@NonNull Registration left, @NonNull Registration right) {
            return Objects.equals(left.getId(), right.getId());
        }
        @Override public boolean areContentsTheSame(@NonNull Registration left, @NonNull Registration right) {
            return Objects.equals(left.getType(), right.getType())
                    && left.getRegisteredAt() == right.getRegisteredAt();
        }
    };

    private final OnRemoveClickListener onRemoveClickListener;

    public RegistrationAdapter(@NonNull OnRemoveClickListener onRemoveClickListener) {
        super(DIFF);
        this.onRemoveClickListener = onRemoveClickListener;
    }

    @NonNull
    @Override
    public RegViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRegistrationBinding binding = ItemRegistrationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RegViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RegViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class RegViewHolder extends RecyclerView.ViewHolder {
        private final ItemRegistrationBinding binding;

        RegViewHolder(@NonNull ItemRegistrationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Registration registration) {
            binding.tvRegistrationType.setText(
                    Registration.TYPE_PARTICIPANT.equals(registration.getType())
                            ? itemView.getContext().getString(R.string.registration_type_participant)
                            : itemView.getContext().getString(R.string.registration_type_volunteer));

            binding.tvRegistrationUser.setText(R.string.d1_attendee);

            if (registration.getRegisteredAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                binding.tvRegistrationDate.setText(sdf.format(new Date(registration.getRegisteredAt())));
            } else {
                binding.tvRegistrationDate.setText("");
            }

            if (Registration.TYPE_PARTICIPANT.equals(registration.getType())) {
                binding.chipRegistrationType.setText(R.string.registration_type_participant);
            } else {
                binding.chipRegistrationType.setText(R.string.registration_type_volunteer);
            }

            // The deployed integrity model permits only self-cancellation; organizer removal
            // requires a trusted backend and must not be offered as a working client action.
            binding.btnRemoveRegistration.setVisibility(View.GONE);
            binding.btnRemoveRegistration.setOnClickListener(v -> {
                if (onRemoveClickListener != null) {
                    onRemoveClickListener.onRemove(registration);
                }
            });
        }
    }
}