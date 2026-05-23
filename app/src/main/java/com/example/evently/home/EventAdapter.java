package com.example.evently.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.evently.R;
import java.util.List;
import java.util.Map;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Map<String, Object>> eventList;
    private final OnEventClickListener listener;
    public interface OnEventClickListener {
        void onEventClick(String documentId);
    }

    public EventAdapter(List<Map<String, Object>> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Map<String, Object> event = eventList.get(position);

        holder.tvName.setText(event.get("name").toString());
        holder.tvDate.setText("Datum: " + event.get("date").toString());
        holder.tvLocation.setText("Grad: " + event.get("city").toString());

        String base64Image = (String) event.get("imageBase64");
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivImage.setImageBitmap(decodedByte);
            } catch (Exception e) {
                holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }


        holder.itemView.setOnClickListener(v -> {
            String documentId = (String) event.get("documentId"); // ID iz baze
            if (listener != null && documentId != null) {
                listener.onEventClick(documentId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvDate, tvLocation;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivEventItemImage);
            tvName = itemView.findViewById(R.id.tvEventItemName);
            tvDate = itemView.findViewById(R.id.tvEventItemDate);
            tvLocation = itemView.findViewById(R.id.tvEventItemLocation);
        }
    }
}