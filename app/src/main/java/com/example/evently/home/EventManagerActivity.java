package com.example.evently.home;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evently.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventManagerActivity extends AppCompatActivity {

    private ImageView ivEventImage;
    private MaterialButton btnSelectImage;
    private EditText etEventName, etEventDescription, etEventDate, etEventCity, etEventLocation, etMaxParticipants, etMaxVolunteers;
    private SwitchMaterial switchVolunteers;
    private TextInputLayout layoutMaxVolunteers;
    private MaterialButton btnSaveEvent;
    private ProgressBar progressBarCreate;

    private String base64Image = "";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;


    private boolean isEditMode = false;
    private String editEventId = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        ivEventImage.setImageBitmap(bitmap);
                        base64Image = encodeImageToBase64(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Greška pri učitavanju slike", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_manager);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ivEventImage = findViewById(R.id.ivEventImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        etEventName = findViewById(R.id.etEventName);
        etEventDescription = findViewById(R.id.etEventDescription);
        etEventDate = findViewById(R.id.etEventDate);
        etEventCity = findViewById(R.id.etEventCity);
        etEventLocation = findViewById(R.id.etEventLocation);
        etMaxParticipants = findViewById(R.id.etMaxParticipants);

        switchVolunteers = findViewById(R.id.switchVolunteers);
        layoutMaxVolunteers = findViewById(R.id.layoutMaxVolunteers);
        etMaxVolunteers = findViewById(R.id.etMaxVolunteers);

        btnSaveEvent = findViewById(R.id.btnSaveEvent);
        progressBarCreate = findViewById(R.id.progressBarCreate);


        switchVolunteers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                layoutMaxVolunteers.setVisibility(View.VISIBLE);
            } else {
                layoutMaxVolunteers.setVisibility(View.GONE);
                etMaxVolunteers.setText("");
            }
        });


        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });


        etEventDate.setOnClickListener(v -> showDatePicker());

        btnSaveEvent.setOnClickListener(v -> saveEventToFirestore());

        checkIfEditMode();
    }


    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Odaberite datum događaja")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            etEventDate.setText(sdf.format(new Date(selection)));
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void checkIfEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("IS_EDIT_MODE", false)) {
            isEditMode = true;
            editEventId = intent.getStringExtra("EVENT_ID");

            btnSaveEvent.setText("Spremi Promjene");

            etEventName.setText(intent.getStringExtra("EVENT_NAME"));
            etEventDescription.setText(intent.getStringExtra("EVENT_DESC"));
            etEventDate.setText(intent.getStringExtra("EVENT_DATE"));
            etEventCity.setText(intent.getStringExtra("EVENT_CITY"));
            etEventLocation.setText(intent.getStringExtra("EVENT_LOCATION"));

            int maxPart = intent.getIntExtra("EVENT_MAX_PARTICIPANTS", 0);
            if (maxPart > 0) etMaxParticipants.setText(String.valueOf(maxPart));

            boolean needsVol = intent.getBooleanExtra("EVENT_NEEDS_VOLUNTEERS", false);
            switchVolunteers.setChecked(needsVol);

            if (needsVol) {
                int maxVol = intent.getIntExtra("EVENT_MAX_VOLUNTEERS", 0);
                if (maxVol > 0) etMaxVolunteers.setText(String.valueOf(maxVol));
                layoutMaxVolunteers.setVisibility(View.VISIBLE);
            }

            String imgString = intent.getStringExtra("EVENT_IMAGE_BASE64");
            if (imgString != null && !imgString.isEmpty()) {
                base64Image = imgString;
                try {
                    byte[] decodedString = Base64.decode(imgString, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivEventImage.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        int maxWidth = 800;
        int maxHeight = 800;
        float ratio = Math.min(
                (float) maxWidth / bitmap.getWidth(),
                (float) maxHeight / bitmap.getHeight());

        int width = Math.round((float) ratio * bitmap.getWidth());
        int height = Math.round((float) ratio * bitmap.getHeight());

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void saveEventToFirestore() {
        String name = etEventName.getText().toString().trim();
        String desc = etEventDescription.getText().toString().trim();
        String date = etEventDate.getText().toString().trim();
        String city = etEventCity.getText().toString().trim();
        String location = etEventLocation.getText().toString().trim();
        String maxPartStr = etMaxParticipants.getText().toString().trim();

        if (name.isEmpty() || date.isEmpty() || city.isEmpty() || maxPartStr.isEmpty()) {
            Toast.makeText(this, "Molimo popunite sva obavezna polja", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxParticipants;
        try {
            maxParticipants = Integer.parseInt(maxPartStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Broj učesnika mora biti broj", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean needsVolunteers = switchVolunteers.isChecked();
        int maxVolunteers = 0;

        if (needsVolunteers) {
            String maxVolStr = etMaxVolunteers.getText().toString().trim();
            if (maxVolStr.isEmpty()) {
                Toast.makeText(this, "Unesite broj potrebnih volontera", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                maxVolunteers = Integer.parseInt(maxVolStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Broj volontera mora biti broj", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        progressBarCreate.setVisibility(View.VISIBLE);
        btnSaveEvent.setEnabled(false);

        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("description", desc);
        event.put("date", date);
        event.put("city", city);
        event.put("location", location);
        event.put("maxParticipants", maxParticipants);
        event.put("needsVolunteers", needsVolunteers);
        event.put("maxVolunteers", maxVolunteers);
        event.put("organizerId", userId);

        if (!base64Image.isEmpty()) {
            event.put("imageBase64", base64Image);
        }

        if (isEditMode && editEventId != null) {
            // EDIT MODE: UPDATE
            db.collection("Events").document(editEventId)
                    .update(event)
                    .addOnSuccessListener(aVoid -> {
                        progressBarCreate.setVisibility(View.GONE);
                        Toast.makeText(EventManagerActivity.this, "Događaj uspješno ažuriran!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBarCreate.setVisibility(View.GONE);
                        btnSaveEvent.setEnabled(true);
                        Toast.makeText(EventManagerActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {

            event.put("currentParticipants", 0);
            event.put("currentVolunteers", 0);
            event.put("timestamp", System.currentTimeMillis());

            db.collection("Events")
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        progressBarCreate.setVisibility(View.GONE);
                        Toast.makeText(EventManagerActivity.this, "Događaj uspješno kreiran!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBarCreate.setVisibility(View.GONE);
                        btnSaveEvent.setEnabled(true);
                        Toast.makeText(EventManagerActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }
}