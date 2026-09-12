package com.example.evently.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.evently.R;
import com.example.evently.data.event.EventRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.data.image.CloudinaryImageUploader;
import com.example.evently.data.image.ImageUploader;
import com.example.evently.data.model.Event;
import com.example.evently.databinding.ActivityEventManagerBinding;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.util.EventFormPolicy;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.data.model.User;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.viewmodel.EventFormViewModel;
import com.example.evently.viewmodel.EventFormViewModelFactory;
import com.example.evently.domain.session.SessionState;

import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.util.TimeZone;

public class EventManagerActivity extends AppCompatActivity {

    private ActivityEventManagerBinding binding;
    private EventRepository eventRepository;
    private ImageUploader imageUploader;

    private boolean isEditMode = false;
    private String editEventId = null;
    private long currentParticipants;
    private long currentVolunteers;

    private String imageUrl = "";
    private SessionViewModel sessionViewModel;
    private EventFormViewModel formViewModel;
    private User sessionUser;
    private String boundUid;
    private boolean sessionRecoveryFailed;
    private long selectedDateLong;
    private com.example.evently.domain.event.EventCategory selectedCategory;
    private String originalCategoryValue = "";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::onImageSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventRepository = new EventRepositoryImpl();
        imageUploader = new CloudinaryImageUploader();
        formViewModel = new ViewModelProvider(this,
                new EventFormViewModelFactory(eventRepository, imageUploader))
                .get(EventFormViewModel.class);
        sessionViewModel = new ViewModelProvider(this,
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);
        sessionViewModel.getState().observe(this, state -> {
            if (state == null || !state.isAuthenticated()) sessionUser = null;
            binding.contentManager.setVisibility(state != null && state.isAuthenticated() ? View.VISIBLE : View.GONE);
            if (state != null && state.isAuthenticated()) {
                String uid=state.getUid();
                if(boundUid!=null && !boundUid.equals(uid)){ formViewModel.cancelAndClear(); finish(); return; }
                boundUid=uid; sessionUser=state.getUser(); formViewModel.bindOwner(uid);
                sessionRecoveryFailed=false;
                if(formViewModel.getStatus().getValue()!=EventFormViewModel.Status.SAVE_ERROR
                        && formViewModel.getStatus().getValue()!=EventFormViewModel.Status.UPLOAD_ERROR)
                    binding.formError.setVisibility(View.GONE);
                validateForm();
            } else if (state != null && state.isGuest()) { formViewModel.cancelAndClear(); finish(); }
            else if (state != null && state.getStatus()==SessionState.Status.PROFILE_RECOVERY_FAILED) {
                sessionRecoveryFailed=true;
                binding.formError.setVisibility(View.VISIBLE);
                validateForm();
            }
        });
        sessionViewModel.start();

        setupViews();
        checkIfEditMode();
        if (savedInstanceState != null) {
            selectedDateLong = savedInstanceState.getLong("D1_SELECTED_DATE", selectedDateLong);
            imageUrl = savedInstanceState.getString("D1_IMAGE_URL", imageUrl);
            if (selectedDateLong > 0) binding.etEventDate.setText(formatDate(selectedDateLong));
        }
        if (savedInstanceState != null) {
            originalCategoryValue = savedInstanceState.getString("E4_CATEGORY_VALUE", "");
            selectedCategory = com.example.evently.domain.event.EventCategory.fromStored(originalCategoryValue);
            if (!TextUtils.isEmpty(originalCategoryValue)) binding.etEventCategory.setText(
                    com.example.evently.ui.views.EventCategoryLabels.display(this, originalCategoryValue), false);
        }
        formViewModel.initializeImage(imageUrl);
        formViewModel.initializeDate(selectedDateLong);
        selectedDateLong=formViewModel.getSelectedDateLong();
        observeFormOperations();
    }

    private void observeFormOperations() {
        formViewModel.getImageUrl().observe(this, value -> {
            if(value==null) return;
            imageUrl=value;
            if(!value.isEmpty()) Glide.with(this).load(value).placeholder(R.drawable.event_placeholder)
                    .error(R.drawable.event_placeholder).centerCrop().into(binding.ivEventImage);
        });
        formViewModel.getUploadProgress().observe(this, percent -> {
            if(percent!=null) binding.tvUploadStatus.setText(getString(R.string.upload_progress, percent));
        });
        formViewModel.getStatus().observe(this, status -> {
            boolean busy=status==EventFormViewModel.Status.UPLOADING || status==EventFormViewModel.Status.SAVING;
            binding.progressBarCreate.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.btnSelectImage.setEnabled(!busy);
            binding.tvUploadStatus.setVisibility(status == EventFormViewModel.Status.UPLOADING
                    || (imageUrl != null && !imageUrl.isEmpty()) ? View.VISIBLE : View.GONE);
            if (status == EventFormViewModel.Status.UPLOAD_ERROR) binding.tvUploadStatus.setVisibility(View.GONE);
            else if (status != EventFormViewModel.Status.UPLOADING) binding.tvUploadStatus.setText(R.string.event_image_ready);
            if ((status == EventFormViewModel.Status.UPLOADING || status == EventFormViewModel.Status.UPLOAD_ERROR)
                    && formViewModel.getSelectedImage() != null)
                Glide.with(this).load(formViewModel.getSelectedImage()).centerCrop().into(binding.ivEventImage);
            binding.btnSelectImage.setText(imageUrl != null && !imageUrl.isEmpty()
                    ? R.string.event_image_replace : R.string.event_pick_image);
            boolean error=status==EventFormViewModel.Status.SAVE_ERROR || status==EventFormViewModel.Status.UPLOAD_ERROR;
            binding.formError.setVisibility(error || sessionRecoveryFailed ? View.VISIBLE : View.GONE);
            binding.formErrorMessage.setText(status==EventFormViewModel.Status.UPLOAD_ERROR
                    ? R.string.upload_error : R.string.d1_event_form_error);
            binding.btnDismissFormError.setText(status==EventFormViewModel.Status.UPLOAD_ERROR
                    ? R.string.event_image_retry : status==EventFormViewModel.Status.SAVE_ERROR
                    ? R.string.d1_retry : R.string.d1_dismiss);
            if(status==EventFormViewModel.Status.SAVE_SUCCESS){
                Toast.makeText(this, isEditMode ? R.string.event_edit_success : R.string.event_create_success, Toast.LENGTH_LONG).show();
                finish(); return;
            }
            validateForm();
        });
        binding.btnDismissFormError.setOnClickListener(v -> {
            if(sessionRecoveryFailed) sessionViewModel.retry();
            else if(formViewModel.canRetryUpload()) formViewModel.retryUpload();
            else if(formViewModel.canRetrySave()) formViewModel.retrySave(); else formViewModel.acknowledgeError();
        });
    }

    private void setupViews() {
        // Date picker
        binding.etEventDate.setOnClickListener(v -> showDatePicker());

        com.example.evently.ui.views.EventCategoryAdapter categoryAdapter =
                new com.example.evently.ui.views.EventCategoryAdapter(
                        this, () -> selectedCategory);
        binding.etEventCategory.setAdapter(categoryAdapter);
        if (selectedCategory != null) {
            binding.etEventCategory.setListSelection(selectedCategory.ordinal());
        }
        binding.etEventCategory.setOnClickListener(v -> {
            if (selectedCategory != null) {
                binding.etEventCategory.setListSelection(selectedCategory.ordinal());
            }
            binding.etEventCategory.showDropDown();
        });
        binding.etEventCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = com.example.evently.domain.event.EventCategory.values()[position];
            binding.etEventCategory.setListSelection(position);
            categoryAdapter.notifyDataSetChanged();
            binding.categoryLayout.setError(null);
            binding.categoryLayout.setHelperText(null);
            validateForm();
        });
        binding.etEventDate.setKeyListener(null);
        binding.dateLayout.setEndIconOnClickListener(v -> showDatePicker());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Volunteers switch
        binding.switchVolunteers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.maxVolunteersLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            validateForm();
        });

        // Image picker
        binding.btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.btnSaveEvent.setOnClickListener(v -> saveEventToFirestore());

        // Real-time validation feedback
        addValidationWatchers();
    }

    private void addValidationWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etEventTitle.addTextChangedListener(watcher);
        binding.etEventDate.addTextChangedListener(watcher);
        binding.etEventCategory.addTextChangedListener(watcher);
        binding.etEventLocation.addTextChangedListener(watcher);
        binding.etMaxParticipants.addTextChangedListener(watcher);
        binding.etMaxVolunteers.addTextChangedListener(watcher);
    }

    private void validateForm() {
        boolean titleOk = !TextUtils.isEmpty(binding.etEventTitle.getText());
        boolean dateOk = !TextUtils.isEmpty(binding.etEventDate.getText());
        boolean catOk = !TextUtils.isEmpty(
                com.example.evently.domain.event.EventCategory.valueForEdit(
                        selectedCategory, isEditMode ? originalCategoryValue : ""));
        boolean locOk = !TextUtils.isEmpty(binding.etEventLocation.getText());
        boolean maxPartOk = isPositiveInteger(binding.etMaxParticipants.getText());

        boolean maxVolOk = EventFormPolicy.volunteerCapacity(binding.switchVolunteers.isChecked(),
                binding.etMaxVolunteers.getText()) != null;

        binding.btnSaveEvent.setEnabled(sessionUser != null && sessionUser.isVerified()
                && User.ROLE_ORGANIZER.equals(sessionUser.getRole()) && !formViewModel.isBusy()
                && formViewModel.getStatus().getValue() != EventFormViewModel.Status.UPLOAD_ERROR
                && titleOk && dateOk && catOk && locOk && maxPartOk && maxVolOk);
    }

    private boolean isPositiveInteger(CharSequence value) {
        if (value == null || TextUtils.isEmpty(value.toString().trim())) {
            return false;
        }
        try {
            return EventFormPolicy.positiveLong(value) != null;
        } catch (RuntimeException ignored) { return false; }
    }

    private void onImageSelected(Uri imageUri) {
        if (imageUri == null) return;
        // PrikaÃ…Â¾i odabranu sliku odmah
        Glide.with(this)
                .load(imageUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(binding.ivEventImage);

        formViewModel.upload(imageUri);
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(com.google.android.material.datepicker.DateValidatorPointForward.from(
                        MaterialDatePicker.todayInUtcMilliseconds()))
                .build();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.event_select_date))
                .setCalendarConstraints(constraints)
                .setSelection(selectedDateLong > 0 ? selectedDateLong : MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateLong = formViewModel.selectDate(selection);
            binding.etEventDate.setText(formatDate(selectedDateLong));
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void checkIfEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("IS_EDIT_MODE", false)) {
            isEditMode = true;
            editEventId = intent.getStringExtra("EVENT_ID");

            binding.btnSaveEvent.setText(R.string.action_save_changes);
            binding.toolbar.setTitle(R.string.event_edit);

            binding.etEventTitle.setText(intent.getStringExtra("EVENT_TITLE"));
            binding.etEventDescription.setText(intent.getStringExtra("EVENT_DESC"));
            selectedDateLong = intent.getLongExtra("EVENT_DATE_LONG", 0L);
            if (selectedDateLong > 0) binding.etEventDate.setText(formatDate(selectedDateLong));
            originalCategoryValue = intent.getStringExtra("EVENT_CATEGORY");
            if (originalCategoryValue == null) originalCategoryValue = "";
            selectedCategory = com.example.evently.domain.event.EventCategory.fromStored(originalCategoryValue);
            if (!TextUtils.isEmpty(originalCategoryValue)) binding.etEventCategory.setText(
                    com.example.evently.ui.views.EventCategoryLabels.display(this, originalCategoryValue), false);
            if (selectedCategory == null && !TextUtils.isEmpty(originalCategoryValue))
                binding.categoryLayout.setHelperText(getString(R.string.event_category_legacy));
            binding.etEventLocation.setText(intent.getStringExtra("EVENT_LOCATION"));

            long maxPart = intent.getLongExtra("EVENT_MAX_PARTICIPANTS", 0);
            if (maxPart > 0) binding.etMaxParticipants.setText(String.valueOf(maxPart));

            long maxVol = intent.getLongExtra("EVENT_MAX_VOLUNTEERS", 0);
            if (maxVol > 0) {
                binding.switchVolunteers.setChecked(true);
                binding.etMaxVolunteers.setText(String.valueOf(maxVol));
            }

            imageUrl = intent.getStringExtra("EVENT_IMAGE_URL");
            currentParticipants = intent.getLongExtra("EVENT_CURRENT_PARTICIPANTS", 0);
            currentVolunteers = intent.getLongExtra("EVENT_CURRENT_VOLUNTEERS", 0);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.event_placeholder)
                        .error(R.drawable.event_placeholder)
                        .centerCrop()
                        .into(binding.ivEventImage);
            }
        }
    }

    private void saveEventToFirestore() {
        if (formViewModel.isBusy()) {
            Toast.makeText(this, R.string.upload_progress_wait, Toast.LENGTH_SHORT).show();
            return;
        }
        String title = binding.etEventTitle.getText().toString().trim();
        String desc = binding.etEventDescription.getText().toString().trim();
        String displayDate = binding.etEventDate.getText().toString().trim();
        String category = com.example.evently.domain.event.EventCategory.valueForEdit(
                selectedCategory, isEditMode ? originalCategoryValue : "");
        String location = binding.etEventLocation.getText().toString().trim();
        String maxPartStr = binding.etMaxParticipants.getText().toString().trim();

        if (!validateAllFields(title, displayDate, category, location, maxPartStr)) {
            return;
        }

        Long parsedParticipants = EventFormPolicy.positiveLong(maxPartStr);
        if (parsedParticipants == null) {
            binding.etMaxParticipants.setError(getString(R.string.event_error_invalid_number));
            return;
        }
        long maxParticipants = parsedParticipants;

        boolean needsVolunteers = binding.switchVolunteers.isChecked();
        Long volunteerCapacity = EventFormPolicy.volunteerCapacity(needsVolunteers, binding.etMaxVolunteers.getText());
        if (volunteerCapacity == null) {
            binding.maxVolunteersLayout.setError(getString(R.string.event_error_invalid_number));
            return;
        }
        long maxVolunteers = volunteerCapacity;

        if (isEditMode && !EventFormPolicy.capacityValid(maxParticipants, currentParticipants)) {
            binding.maxParticipantsLayout.setError(getString(R.string.d1_capacity_below_occupancy));
            return;
        }
        if (isEditMode && !EventFormPolicy.capacityValid(maxVolunteers, currentVolunteers)) {
            binding.switchVolunteers.setChecked(true);
            binding.maxVolunteersLayout.setError(getString(R.string.d1_capacity_below_occupancy));
            return;
        }

        User currentUser = sessionUser;
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_auth_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        String userName = currentUser.getName() != null && !currentUser.getName().trim().isEmpty()
                ? currentUser.getName()
                : getString(R.string.organizer_default_name);

        long dateLong = selectedDateLong;
        if (dateLong <= 0) {
            binding.etEventDate.setError(getString(R.string.event_error_fill_required));
            return;
        }
        if (!EventFormPolicy.registrationDayNotExpired(dateLong, System.currentTimeMillis())) {
            binding.dateLayout.setError(getString(R.string.d1_event_date_expired));
            return;
        }
        displayDate = formatDate(dateLong);

        if (isEditMode && editEventId != null) {
            updateEvent(editEventId, title, desc, category, dateLong, displayDate,
                    location, imageUrl, maxParticipants, maxVolunteers, needsVolunteers);
        } else {
            createEvent(userId, userName, title, desc, category, dateLong, displayDate,
                    location, imageUrl, maxParticipants, maxVolunteers, needsVolunteers,
                    currentUser.isVerified());
        }
    }

    private boolean validateAllFields(String title, String displayDate, String category,
                                      String location, String maxPartStr) {
        if (TextUtils.isEmpty(title)) {
            binding.etEventTitle.setError(getString(R.string.event_error_name_required));
            return false;
        }
        if (TextUtils.isEmpty(displayDate)) {
            binding.etEventDate.setError(getString(R.string.event_error_fill_required));
            return false;
        }
        if (TextUtils.isEmpty(category)) {
            binding.etEventCategory.setError(getString(R.string.event_error_fill_required));
            return false;
        }
        if (TextUtils.isEmpty(location)) {
            binding.etEventLocation.setError(getString(R.string.event_error_fill_required));
            return false;
        }
        if (TextUtils.isEmpty(maxPartStr)) {
            binding.etMaxParticipants.setError(getString(R.string.event_error_fill_required));
            return false;
        }
        return true;
    }


    private void createEvent(String userId, String userName, String title, String desc,
                             String category, long dateLong, String displayDate,
                             String location, String imageUrl,
                             long maxParticipants, long maxVolunteers, boolean needsVolunteers,
                             boolean organizerVerified) {

        Event event = new Event(""); // id Ã„â€¡e dodati Firestore
        event.setTitle(title);
        event.setDescription(desc);
        event.setCategory(category);
        event.setDateLong(dateLong);
        event.setDisplayDate(displayDate);
        event.setLocation(location);
        event.setImageUrl(imageUrl);
        event.setOrganizerId(userId);
        event.setOrganizerName(userName);
        event.setOrganizerVerified(organizerVerified);
        event.setMaxParticipants(maxParticipants);
        event.setMaxVolunteers(maxVolunteers);
        event.setRegistrationsOpen(true);
        event.setStatus(Event.STATUS_ACTIVE);
        event.setCreatedAt(System.currentTimeMillis());

        formViewModel.save(event, false);
    }

    private void updateEvent(String eventId, String title, String desc,
                             String category, long dateLong, String displayDate,
                             String location, String imageUrl,
                             long maxParticipants, long maxVolunteers, boolean needsVolunteers) {

        Event event = new Event(eventId);
        event.setTitle(title);
        event.setDescription(desc);
        event.setCategory(category);
        event.setDateLong(dateLong);
        event.setDisplayDate(displayDate);
        event.setLocation(location);
        event.setImageUrl(imageUrl);
        event.setMaxParticipants(maxParticipants);
        event.setMaxVolunteers(maxVolunteers);

        formViewModel.save(event, true);
    }

    private String formatDate(long epochMillis) {
        DateFormat formatter=android.text.format.DateFormat.getDateFormat(this);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(new Date(epochMillis));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("D1_SELECTED_DATE", selectedDateLong);
        outState.putString("E4_CATEGORY_VALUE",
                com.example.evently.domain.event.EventCategory.valueForEdit(
                        selectedCategory, isEditMode ? originalCategoryValue : ""));
        outState.putString("D1_IMAGE_URL", imageUrl);
        super.onSaveInstanceState(outState);
    }
}
