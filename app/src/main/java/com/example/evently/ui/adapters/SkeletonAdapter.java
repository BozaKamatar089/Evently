package com.example.evently.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evently.databinding.ItemEventSkeletonBinding;

/**
 * Adapter za prikaz skeleton loading kartica.
 * Koristi se dok se pravi podaci učitavaju.
 */
public class SkeletonAdapter extends RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder> {

    private static final int SKELETON_COUNT = 6; // Broj skeleton kartica za prikaz

    @NonNull
    @Override
    public SkeletonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventSkeletonBinding binding = ItemEventSkeletonBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SkeletonViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SkeletonViewHolder holder, int position) {
        // Skeleton kartice ne treba podatke - samo se prikazuju
        // ViewBinding doesn't have executePendingBindings, that's DataBinding
    }

    @Override
    public int getItemCount() {
        return SKELETON_COUNT;
    }

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        final ItemEventSkeletonBinding binding;

        SkeletonViewHolder(@NonNull ItemEventSkeletonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}