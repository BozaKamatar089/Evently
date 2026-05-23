package com.example.evently.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evently.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;


    private List<Map<String, Object>> eventList;
    private List<Map<String, Object>> eventListFull;

    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private ExtendedFloatingActionButton fabAddEvent;
    private LinearLayout layoutUserHeader;
    private EditText etSearch;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recyclerViewEvents);
        progressBar = view.findViewById(R.id.progressBarHome);
        tvEmptyState = view.findViewById(R.id.tvEmptyStateHome);
        fabAddEvent = view.findViewById(R.id.fabAddEvent);
        layoutUserHeader = view.findViewById(R.id.layoutUserHeader);
        etSearch = view.findViewById(R.id.etSearch);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventList = new ArrayList<>();
        eventListFull = new ArrayList<>();

        adapter = new EventAdapter(eventList, documentId -> {
            Intent intent = new Intent(getActivity(), EventDetailsActivity.class);
            intent.putExtra("EVENT_ID", documentId);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        checkUserRole();
        loadEvents();
        setupSearch();
    }

    private void checkUserRole() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isOrganizer = prefs.getBoolean("isOrganizer", false);

        if (isOrganizer) {
            fabAddEvent.setVisibility(View.VISIBLE);
            layoutUserHeader.setVisibility(View.GONE);
            fabAddEvent.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), EventManagerActivity.class);
                startActivity(intent);
            });
        } else {
            fabAddEvent.setVisibility(View.GONE);
            layoutUserHeader.setVisibility(View.VISIBLE);
        }
    }

    private void loadEvents() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);


        db.collection("Events").addSnapshotListener((value, error) -> {
            progressBar.setVisibility(View.GONE);

            if (error != null) {
                Toast.makeText(getContext(), "Greška pri učitavanju događaja.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                eventList.clear();
                eventListFull.clear();

                for (QueryDocumentSnapshot document : value) {
                    Map<String, Object> eventData = document.getData();
                    eventData.put("documentId", document.getId());

                    eventList.add(eventData);
                    eventListFull.add(eventData);
                }

                adapter.notifyDataSetChanged();

                if (eventList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Trenutno nema dostupnih događaja.");
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                }


                filterEvents(etSearch.getText().toString());
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void filterEvents(String text) {
        eventList.clear();

        if (text.isEmpty()) {

            eventList.addAll(eventListFull);
        } else {
            String filterPattern = text.toLowerCase().trim();


            for (Map<String, Object> event : eventListFull) {
                String eventName = event.get("name") != null ? event.get("name").toString().toLowerCase() : "";
                String eventCity = event.get("city") != null ? event.get("city").toString().toLowerCase() : "";


                if (eventName.contains(filterPattern) || eventCity.contains(filterPattern)) {
                    eventList.add(event);
                }
            }
        }

        adapter.notifyDataSetChanged();


        if (eventList.isEmpty() && !eventListFull.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Nema događaja koji odgovaraju pretrazi.");
        } else if (eventList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Trenutno nema dostupnih događaja.");
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }
}