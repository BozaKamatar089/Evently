package com.example.evently.ui.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.evently.R;
import com.example.evently.domain.event.EventCategory;

import java.util.function.Supplier;

/** Owns category popup labels and selected-row presentation for every form host. */
public final class EventCategoryAdapter extends android.widget.ArrayAdapter<String> {
    private final Supplier<EventCategory> selection;

    public EventCategoryAdapter(@NonNull Context context,
                                @NonNull Supplier<EventCategory> selection) {
        super(context, R.layout.item_category_option, R.id.tvCategoryOption,
                EventCategoryLabels.choices(context));
        this.selection = selection;
        setDropDownViewResource(R.layout.item_category_option);
    }

    @NonNull @Override public View getView(int position, @Nullable View convertView,
                                           @NonNull ViewGroup parent) {
        return applySelection(super.getView(position, convertView, parent), position);
    }

    @Override public View getDropDownView(int position, @Nullable View convertView,
                                         @NonNull ViewGroup parent) {
        return applySelection(super.getDropDownView(position, convertView, parent), position);
    }

    private View applySelection(@NonNull View row, int position) {
        EventCategory selected = selection.get();
        row.setActivated(selected != null && selected.ordinal() == position);
        return row;
    }
}
