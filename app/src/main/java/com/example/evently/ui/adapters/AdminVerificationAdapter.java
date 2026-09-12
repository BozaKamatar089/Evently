package com.example.evently.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evently.data.model.VerificationRequest;
import com.example.evently.databinding.ItemAdminVerificationBinding;

import java.util.Date;
import java.util.Objects;

/** Queue rows for verification work; the Activity owns navigation and decisions. */
public final class AdminVerificationAdapter
        extends ListAdapter<VerificationRequest, AdminVerificationAdapter.ViewHolder> {
    public interface Listener {
        void onViewDocument(@NonNull VerificationRequest request);
        void onApprove(@NonNull VerificationRequest request);
        void onReject(@NonNull VerificationRequest request);
    }

    private static final DiffUtil.ItemCallback<VerificationRequest> DIFF =
            new DiffUtil.ItemCallback<VerificationRequest>() {
                @Override public boolean areItemsTheSame(@NonNull VerificationRequest left,
                                                         @NonNull VerificationRequest right) {
                    return Objects.equals(left.getId(), right.getId());
                }
                @Override public boolean areContentsTheSame(@NonNull VerificationRequest left,
                                                            @NonNull VerificationRequest right) {
                    return Objects.equals(left.getStatus(), right.getStatus())
                            && Objects.equals(left.getOrgName(), right.getOrgName())
                            && Objects.equals(left.getDocumentUrl(), right.getDocumentUrl())
                            && left.getCreatedAt() == right.getCreatedAt();
                }
            };

    private final Listener listener;

    public AdminVerificationAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAdminVerificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminVerificationBinding binding;
        ViewHolder(@NonNull ItemAdminVerificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(@NonNull VerificationRequest request) {
            binding.tvOrgName.setText(request.getOrgName() == null ? "" : request.getOrgName());
            binding.tvUserId.setVisibility(View.GONE);
            binding.tvDate.setText(request.getCreatedAt() > 0
                    ? android.text.format.DateFormat.getMediumDateFormat(itemView.getContext())
                    .format(new Date(request.getCreatedAt())) : "");
            boolean hasDocument = request.getDocumentUrl() != null && !request.getDocumentUrl().isEmpty();
            binding.tvViewDocument.setVisibility(hasDocument ? View.VISIBLE : View.GONE);
            binding.tvViewDocument.setOnClickListener(hasDocument
                    ? view -> listener.onViewDocument(request) : null);
            boolean actionable = request.getUserId() != null && !request.getUserId().isEmpty();
            binding.btnApprove.setEnabled(actionable);
            binding.btnReject.setEnabled(actionable);
            binding.btnApprove.setOnClickListener(actionable
                    ? view -> listener.onApprove(request) : null);
            binding.btnReject.setOnClickListener(actionable
                    ? view -> listener.onReject(request) : null);
        }
    }
}
