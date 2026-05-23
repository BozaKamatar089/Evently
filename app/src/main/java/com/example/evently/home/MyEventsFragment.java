package com.example.evently.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evently.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Map<String, Object>> eventList;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvTitleMyEvents;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recyclerViewMyEvents);
        progressBar = view.findViewById(R.id.progressBarMyEvents);
        tvEmptyState = view.findViewById(R.id.tvEmptyStateMyEvents);
        tvTitleMyEvents = view.findViewById(R.id.tvTitleMyEvents);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventList = new ArrayList<>();

        adapter = new EventAdapter(eventList, documentId -> {
            Intent intent = new Intent(getActivity(), EventDetailsActivity.class);
            intent.putExtra("EVENT_ID", documentId);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        loadMyEvents();
    }

    private void loadMyEvents() {
        if (mAuth.getCurrentUser() == null) return;

        String myUserId = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isOrganizer = prefs.getBoolean("isOrganizer", false);

        if (isOrganizer) {

            if (tvTitleMyEvents != null) tvTitleMyEvents.setText("Događaji koje sam kreirao");

            db.collection("Events")
                    .whereEqualTo("organizerId", myUserId)
                    .addSnapshotListener((value, error) -> {
                        progressBar.setVisibility(View.GONE);
                        if (error != null) return;

                        if (value != null) {
                            eventList.clear();
                            for (QueryDocumentSnapshot document : value) {
                                Map<String, Object> eventData = document.getData();
                                eventData.put("documentId", document.getId());
                                eventList.add(eventData);
                            }
                            adapter.notifyDataSetChanged();

                            if (eventList.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                tvEmptyState.setText("Još niste kreirali nijedan događaj.");
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                            }
                        }
                    });

        } else {

            if (tvTitleMyEvents != null) tvTitleMyEvents.setText("Moje Prijave");


            db.collection("Registrations")
                    .whereEqualTo("userId", myUserId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        if (value != null) {
                            List<String> eventIds = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                eventIds.add(doc.getString("eventId"));
                            }

                            if (eventIds.isEmpty()) {
                                progressBar.setVisibility(View.GONE);
                                eventList.clear();
                                adapter.notifyDataSetChanged();
                                tvEmptyState.setVisibility(View.VISIBLE);
                                tvEmptyState.setText("Niste prijavljeni ni na jedan događaj.");
                                return;
                            }

                            db.collection("Events").addSnapshotListener((eventValue, eventError) -> {
                                progressBar.setVisibility(View.GONE);
                                if (eventError != null) return;

                                if (eventValue != null) {
                                    eventList.clear();
                                    for (QueryDocumentSnapshot eventDoc : eventValue) {
                                        if (eventIds.contains(eventDoc.getId())) {
                                            Map<String, Object> eventData = eventDoc.getData();
                                            eventData.put("documentId", eventDoc.getId());
                                            eventList.add(eventData);
                                        }
                                    }
                                    adapter.notifyDataSetChanged();

                                    if (eventList.isEmpty()) {
                                        tvEmptyState.setVisibility(View.VISIBLE);
                                        tvEmptyState.setText("Događaji na koje ste prijavljeni više ne postoje.");
                                    } else {
                                        tvEmptyState.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }
                    });
        }
    }
}