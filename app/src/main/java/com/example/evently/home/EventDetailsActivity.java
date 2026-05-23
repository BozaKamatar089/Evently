package com.example.evently.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evently.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {

    private ImageView ivImage;
    private TextView tvName, tvDateLocation, tvDescription, tvParticipantsCount, tvVolunteersCount, tvStatusMessage;
    private ProgressBar progressBar;
    private LinearLayout layoutButtons, layoutOrganizerButtons;
    private MaterialButton btnJoinParticipant, btnJoinVolunteer, btnEditEvent, btnDeleteEvent;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String eventId;

    private DocumentSnapshot currentEventDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        eventId = getIntent().getStringExtra("EVENT_ID");

        ivImage = findViewById(R.id.ivDetailImage);
        tvName = findViewById(R.id.tvDetailName);
        tvDateLocation = findViewById(R.id.tvDetailDateLocation);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvParticipantsCount = findViewById(R.id.tvParticipantsCount);
        tvVolunteersCount = findViewById(R.id.tvVolunteersCount);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        progressBar = findViewById(R.id.progressBarDetails);
        layoutButtons = findViewById(R.id.layoutButtons);
        btnJoinParticipant = findViewById(R.id.btnJoinParticipant);
        btnJoinVolunteer = findViewById(R.id.btnJoinVolunteer);

        layoutOrganizerButtons = findViewById(R.id.layoutOrganizerButtons);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);

        if (eventId == null) {
            Toast.makeText(this, "Greška: Događaj nije pronađen!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventDetails();
    }

    private void loadEventDetails() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        currentEventDoc = documentSnapshot;
                        displayEventData(documentSnapshot);
                    } else {
                        Toast.makeText(this, "Ovaj događaj više ne postoji.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Greška pri učitavanju.", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayEventData(DocumentSnapshot doc) {
        String myUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        String name = doc.getString("name");
        String date = doc.getString("date");
        String city = doc.getString("city");
        String location = doc.getString("location");
        String desc = doc.getString("description");
        String organizerId = doc.getString("organizerId");

        tvName.setText(name);
        tvDateLocation.setText(date + " • " + city + " (" + location + ")");
        tvDescription.setText((desc != null && !desc.isEmpty()) ? desc : "Nema opisa.");

        String base64Image = doc.getString("imageBase64");
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivImage.setImageBitmap(decodedByte);
            } catch (Exception e) {

            }
        }

        long maxPart = doc.getLong("maxParticipants") != null ? doc.getLong("maxParticipants") : 0;
        long currPart = doc.getLong("currentParticipants") != null ? doc.getLong("currentParticipants") : 0;
        tvParticipantsCount.setText("Učesnici: " + currPart + " / " + maxPart);

        boolean needsVol = doc.getBoolean("needsVolunteers") != null && doc.getBoolean("needsVolunteers");
        long maxVol = doc.getLong("maxVolunteers") != null ? doc.getLong("maxVolunteers") : 0;
        long currVol = doc.getLong("currentVolunteers") != null ? doc.getLong("currentVolunteers") : 0;

        if (needsVol) {
            tvVolunteersCount.setVisibility(View.VISIBLE);
            tvVolunteersCount.setText("Volonteri: " + currVol + " / " + maxVol);
            btnJoinVolunteer.setVisibility(View.VISIBLE);
        } else {
            tvVolunteersCount.setVisibility(View.GONE);
            btnJoinVolunteer.setVisibility(View.GONE);
        }

        if (myUserId.equals(organizerId)) {

            layoutButtons.setVisibility(View.GONE);
            tvStatusMessage.setVisibility(View.GONE);
            layoutOrganizerButtons.setVisibility(View.VISIBLE);

            btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmationDialog());
            btnEditEvent.setOnClickListener(v -> openEditEventActivity());

        } else {

            layoutOrganizerButtons.setVisibility(View.GONE);

            String registrationId = myUserId + "_" + eventId;
            db.collection("Registrations").document(registrationId).get()
                    .addOnSuccessListener(regDoc -> {
                        if (regDoc.exists()) {
                            layoutButtons.setVisibility(View.GONE);
                            tvStatusMessage.setVisibility(View.VISIBLE);
                            tvStatusMessage.setText("Već ste prijavljeni na ovaj događaj kao " + regDoc.getString("role") + "!");
                        } else {
                            layoutButtons.setVisibility(View.VISIBLE);
                            tvStatusMessage.setVisibility(View.GONE);

                            if (currPart >= maxPart) {
                                btnJoinParticipant.setEnabled(false);
                                btnJoinParticipant.setText("Popunjeno");
                            }
                            if (needsVol && currVol >= maxVol) {
                                btnJoinVolunteer.setEnabled(false);
                                btnJoinVolunteer.setText("Volonteri Popunjeni");
                            }

                            btnJoinParticipant.setOnClickListener(v -> {
                                btnJoinParticipant.setEnabled(false);
                                registerUserForEvent(eventId, "participant");
                            });

                            btnJoinVolunteer.setOnClickListener(v -> {
                                btnJoinVolunteer.setEnabled(false);
                                registerUserForEvent(eventId, "volunteer");
                            });
                        }
                    });
        }
    }

    private void openEditEventActivity() {
        Intent intent = new Intent(this, EventManagerActivity.class);
        intent.putExtra("IS_EDIT_MODE", true);
        intent.putExtra("EVENT_ID", eventId);

        intent.putExtra("EVENT_NAME", currentEventDoc.getString("name"));
        intent.putExtra("EVENT_DESC", currentEventDoc.getString("description"));
        intent.putExtra("EVENT_DATE", currentEventDoc.getString("date"));
        intent.putExtra("EVENT_CITY", currentEventDoc.getString("city"));
        intent.putExtra("EVENT_LOCATION", currentEventDoc.getString("location"));


        Long maxPart = currentEventDoc.getLong("maxParticipants");
        intent.putExtra("EVENT_MAX_PARTICIPANTS", maxPart != null ? maxPart.intValue() : 0);

        Boolean needsVol = currentEventDoc.getBoolean("needsVolunteers");
        intent.putExtra("EVENT_NEEDS_VOLUNTEERS", needsVol != null ? needsVol : false);

        Long maxVol = currentEventDoc.getLong("maxVolunteers");
        intent.putExtra("EVENT_MAX_VOLUNTEERS", maxVol != null ? maxVol.intValue() : 0);

        intent.putExtra("EVENT_IMAGE_BASE64", currentEventDoc.getString("imageBase64"));

        startActivity(intent);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Brisanje događaja")
                .setMessage("Da li ste sigurni da želite obrisati ovaj događaj?")
                .setPositiveButton("Izbriši", (dialog, which) -> deleteEvent())
                .setNegativeButton("Odustani", null)
                .show();
    }

    private void deleteEvent() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EventDetailsActivity.this, "Događaj izbrisan.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EventDetailsActivity.this, "Greška pri brisanju: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void registerUserForEvent(String eventId, String role) {
        String myUserId = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> regData = new HashMap<>();
        regData.put("eventId", eventId);
        regData.put("userId", myUserId);
        regData.put("role", role);
        regData.put("timestamp", System.currentTimeMillis());

        String registrationId = myUserId + "_" + eventId;

        db.collection("Registrations").document(registrationId).set(regData)
                .addOnSuccessListener(aVoid -> {
                    String counterField = role.equals("participant") ? "currentParticipants" : "currentVolunteers";

                    db.collection("Events").document(eventId)
                            .update(counterField, FieldValue.increment(1))
                            .addOnSuccessListener(aVoid1 -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(EventDetailsActivity.this, "Uspješno ste se prijavili!", Toast.LENGTH_SHORT).show();
                                loadEventDetails();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EventDetailsActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}