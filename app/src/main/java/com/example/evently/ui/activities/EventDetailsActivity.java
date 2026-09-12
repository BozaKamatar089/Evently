package com.example.evently.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.evently.R;
import com.example.evently.data.event.EventRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.data.model.Event;
import com.example.evently.data.model.Registration;
import com.example.evently.data.registration.RegistrationRepositoryImpl;
import com.example.evently.databinding.ActivityEventDetailsBinding;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.registration.RegistrationRepository;
import com.example.evently.ui.adapters.RegistrationAdapter;
import com.example.evently.util.PendingActionManager;
import com.example.evently.util.AuthGate;
import com.example.evently.util.EventActionEligibility;
import com.google.android.material.button.MaterialButton;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.domain.session.SessionState;

import com.example.evently.util.ErrorMapper;

import com.google.firebase.firestore.ListenerRegistration;


public class EventDetailsActivity extends AppCompatActivity {

    private ActivityEventDetailsBinding binding;
    private EventRepository eventRepository;
    private RegistrationRepository registrationRepository;
    private String eventId;

    private boolean isOrganizer = false;
    private Registration myRegistration;
    private Event currentEvent;
    private ListenerRegistration eventListener;
    private ListenerRegistration registrationsDialogListener;
    private SessionViewModel sessionViewModel;
    private com.example.evently.viewmodel.FavoriteActionViewModel favoriteViewModel;
    private String currentUid = "";
    private boolean replayPending;
    private String replayType;
    private boolean registrationOperationInProgress;
    private boolean sessionReady;
    private int eventGeneration;
    private int registrationReadGeneration;
    private AlertDialog registrationsDialog;

    public static Intent createIntent(Context context, String eventId) {
        Intent intent = new Intent(context, EventDetailsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventRepository = new EventRepositoryImpl();
        registrationRepository = new RegistrationRepositoryImpl();
        favoriteViewModel = new ViewModelProvider(this,
                new com.example.evently.viewmodel.FavoriteActionViewModelFactory(
                        new com.example.evently.data.favorites.FavoriteRepositoryImpl()))
                .get(com.example.evently.viewmodel.FavoriteActionViewModel.class);
        favoriteViewModel.getFavoriteIds().observe(this, ids -> renderFavorite());
        favoriteViewModel.getStreamError().observe(this, failed -> renderFavorite());
        favoriteViewModel.getResult().observe(this, result -> {
            if (result == null || !result.getEventId().equals(eventId)) return;
            binding.tvFavoriteError.setVisibility(result.getErrorCode() == null ? View.GONE : View.VISIBLE);
        });
        binding.btnDetailFavorite.setOnClickListener(v -> {
            if (!sessionReady || currentEvent == null) return;
            if (currentUid.isEmpty()) {
                AuthGate.requireSignIn(this, PendingActionManager.ActionType.FAVORITE_TOGGLE, eventId);
            } else if (Boolean.TRUE.equals(favoriteViewModel.getStreamError().getValue())) {
                favoriteViewModel.observeForUser(currentUid);
            } else {
                binding.tvFavoriteError.setVisibility(View.GONE);
                favoriteViewModel.toggle(eventId);
            }
        });
        sessionViewModel = new ViewModelProvider(this,
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);
        sessionViewModel.getState().observe(this, state -> {
            registrationReadGeneration++;
            myRegistration = null;
            currentUid = state != null && state.isAuthenticated() && state.getUid() != null
                    ? state.getUid() : "";
            sessionReady = state != null && (state.isAuthenticated() || state.isGuest());
            if (sessionReady && !currentUid.isEmpty()) favoriteViewModel.observeForUser(currentUid);
            else favoriteViewModel.stopObserving();
            renderFavorite();
            if (state != null && state.getStatus() == SessionState.Status.PROFILE_RECOVERY_FAILED) {
                showDetailsMessage(R.string.d1_event_load_error, true);
            } else if (currentEvent != null && sessionReady) displayEventData(currentEvent);
            else if (!sessionReady) {
                binding.contentDetails.setVisibility(View.GONE);
                binding.errorDetails.setVisibility(View.GONE);
                binding.progressBarDetails.setVisibility(View.VISIBLE);
            }
        });
        sessionViewModel.start();
        eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventId == null) {
            Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();

        // Check for auto-register intent from login replay
        checkAutoRegister();
        binding.btnRetryDetails.setOnClickListener(v -> {
            sessionViewModel.retry();
            stopEventListener();
            startEventListener();
        });
    }

    private void checkAutoRegister() {
        boolean autoRegister = getIntent().getBooleanExtra("AUTO_REGISTER", false);
        if (autoRegister) {
            String type = getIntent().getStringExtra("AUTO_REGISTER_TYPE");
            if (type != null) {
                replayPending = true;
                replayType = type;
            }
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionReady && !currentUid.isEmpty()) favoriteViewModel.observeForUser(currentUid);
        startEventListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopEventListener();
        favoriteViewModel.stopObserving();
        stopRegistrationsDialogListener();
        if (registrationsDialog != null) { registrationsDialog.dismiss(); registrationsDialog = null; }
    }

    private void startEventListener() {
        if (eventListener != null || eventId == null) return;
        currentEvent = null;
        final int generation = ++eventGeneration;
        binding.progressBarDetails.setVisibility(View.VISIBLE);
        binding.errorDetails.setVisibility(View.GONE);
        binding.contentDetails.setVisibility(View.GONE);
        eventListener = eventRepository.listenEvent(eventId, (event, error) -> {
            if (generation != eventGeneration || isDestroyed()) return;
            if (error != null) {
                currentEvent = null;
                showDetailsMessage(R.string.d1_event_load_error, true);
                return;
            }
            if (event == null || Event.STATUS_DELETED.equals(event.getStatus())) {
                currentEvent = null;
                showDetailsMessage(R.string.event_not_found, false);
                return;
            }
            currentEvent = event;
            if (sessionReady) {
                displayEventData(event);
                binding.contentDetails.setVisibility(View.VISIBLE);
            }
        });
    }

    private void stopEventListener() {
        eventGeneration++;
        registrationReadGeneration++;
        if (eventListener != null) {
            eventListener.remove();
            eventListener = null;
        }
    }

    private void showDetailsMessage(int message, boolean retry) {
        if (getIntent().getBooleanExtra("PENDING_ACTION_REPLAY", false) && !registrationOperationInProgress)
            new PendingActionManager(this).finishConsumption(false);
        binding.progressBarDetails.setVisibility(View.GONE);
        binding.contentDetails.setVisibility(View.GONE);
        binding.errorDetails.setVisibility(View.VISIBLE);
        binding.tvDetailsState.setText(message);
        binding.btnRetryDetails.setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private void displayEventData(Event event) {
        if (event == null) {
            binding.progressBarDetails.setVisibility(View.GONE);
            Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentEvent = event;
        binding.progressBarDetails.setVisibility(View.GONE);
        binding.errorDetails.setVisibility(View.GONE);
        binding.contentDetails.setVisibility(View.VISIBLE);

        String myUserId = currentUid;
        isOrganizer = myUserId != null && !myUserId.isEmpty()
                && myUserId.equals(event.getOrganizerId());

        binding.tvDetailTitle.setText(event.getTitle());
        binding.tvDetailDateLocation.setText(formatEventDate(event));
        binding.tvDetailLocation.setText(event.getLocation());
        binding.tvDetailOrganizer.setText(event.getOrganizerName());
        binding.tvDetailVerified.setVisibility(event.isOrganizerVerified() ? View.VISIBLE : View.GONE);
        binding.layoutVolunteerAction.setVisibility(event.getMaxVolunteers() > 0 ? View.VISIBLE : View.GONE);
        binding.tvRegistrationState.setText(registrationStateText(event));
        renderFavorite();
        binding.tvDetailDescription.setText(
                event.getDescription() != null && !event.getDescription().isEmpty()
                        ? event.getDescription() : getString(R.string.event_no_description));

        if (event.getCategory() != null && !event.getCategory().isEmpty()) {
            binding.chipDetailCategory.setText(com.example.evently.ui.views.EventCategoryLabels.display(this, event.getCategory()));
            binding.chipDetailCategory.setVisibility(View.VISIBLE);
        } else {
            binding.chipDetailCategory.setVisibility(View.GONE);
        }

        Glide.with(this).clear(binding.ivDetailImage);
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.event_placeholder)
                    .error(R.drawable.event_placeholder)
                    .centerCrop()
                    .into(binding.ivDetailImage);
        } else {
            binding.ivDetailImage.setImageResource(R.drawable.event_placeholder);
        }

        // Kapacitet
        long maxPart = event.getMaxParticipants();
        long currPart = event.getCurrentParticipants();
        long seatsLeft = Math.max(0, maxPart - currPart);
        binding.tvParticipantsCount.setText(
                getString(R.string.event_spots, seatsLeft, maxPart));
        binding.progressDetailParticipants.setMax(progressInt(maxPart));
        binding.progressDetailParticipants.setProgressCompat(progressInt(Math.min(currPart, maxPart)), true);
        if (seatsLeft <= 0) {
            binding.tvDetailParticipantsLabel.setText(R.string.event_capacity_full);
        } else {
            binding.tvDetailParticipantsLabel.setText("");
        }

        // Volonteri
        boolean needsVol = event.getMaxVolunteers() > 0;
        if (needsVol) {
            long maxVol = event.getMaxVolunteers();
            long currVol = event.getCurrentVolunteers();
            long volLeft = Math.max(0, maxVol - currVol);
            binding.tvVolunteersCount.setText(
                    getString(R.string.event_spots, volLeft, maxVol));
            binding.progressDetailVolunteers.setMax(progressInt(maxVol));
            binding.progressDetailVolunteers.setProgressCompat(progressInt(Math.min(currVol, maxVol)), true);
            binding.layoutVolunteers.setVisibility(View.VISIBLE);
        } else {
            binding.layoutVolunteers.setVisibility(View.GONE);
        }

        // Buttons
        if (isOrganizer) {
            binding.layoutJoinButtons.setVisibility(View.GONE);
            binding.layoutOrganizerButtons.setVisibility(View.VISIBLE);
            binding.btnViewRegistrations.setOnClickListener(v -> showRegistrationsDialog());
            binding.btnEditEvent.setOnClickListener(v -> openEditEventActivity(event));
            binding.btnDeleteEvent.setOnClickListener(v -> showDeleteConfirmationDialog());
            boolean empty = event.getCurrentParticipants() == 0 && event.getCurrentVolunteers() == 0;
            binding.btnDeleteEvent.setEnabled(empty && !registrationOperationInProgress);
            binding.btnDeleteEvent.setText(empty ? R.string.event_delete : R.string.d1_delete_requires_empty);
        } else {
            binding.layoutOrganizerButtons.setVisibility(View.GONE);
            binding.layoutJoinButtons.setVisibility(View.VISIBLE);
            checkRegistrationAndSetupButtons(myUserId, event);
        }
    }

    private void checkRegistrationAndSetupButtons(String myUserId, Event event) {
        final int readGeneration = ++registrationReadGeneration;
        binding.btnJoinParticipant.setEnabled(false);
        binding.btnJoinVolunteer.setEnabled(false);
        if (myUserId.isEmpty()) {
            // Gost - onemogući dugmad
            setupGuestButtons(event);
            return;
        }

        registrationRepository.getRegistration(eventId)
                .addOnSuccessListener(registration -> {
                    if (readGeneration != registrationReadGeneration || !myUserId.equals(currentUid) || isDestroyed()) return;
                    myRegistration = registration;
                    setupUserButtons(event, registration);
                    replayWhenReady(event, registration);
                })
                .addOnFailureListener(e -> {
                    if (readGeneration != registrationReadGeneration || !myUserId.equals(currentUid) || isDestroyed()) return;
                    showDetailsMessage(R.string.d1_event_load_error, true);
                });
    }

    private void replayWhenReady(Event event, Registration registration) {
        if (!replayPending || replayType == null || registrationOperationInProgress || currentUid.isEmpty()) return;
        getIntent().removeExtra("AUTO_REGISTER");
        EventActionEligibility.Reason reason = EventActionEligibility.forType(
                event, registration, replayType, System.currentTimeMillis());
        if (reason == EventActionEligibility.Reason.ELIGIBLE) {
            replayPending = false;
            registerAs(replayType);
        } else if (reason == EventActionEligibility.Reason.ALREADY_REGISTERED) {
            replayPending = false;
            new PendingActionManager(this).finishConsumption(true);
            getIntent().removeExtra("PENDING_ACTION_REPLAY");
        } else {
            replayPending = false;
            new PendingActionManager(this).finishConsumption(false);
        }
    }

    private void setupGuestButtons(Event event) {
        applyEligibility(binding.btnJoinParticipant, event, null, Registration.TYPE_PARTICIPANT);
        if (binding.btnJoinParticipant.isEnabled()) binding.btnJoinParticipant.setText(R.string.d1_sign_in_participant);

        binding.btnJoinParticipant.setOnClickListener(v -> {
            if (!binding.btnJoinParticipant.isEnabled()) return;
            AuthGate.requireSignIn(this, PendingActionManager.ActionType.PARTICIPANT_REGISTER, event.getId());
        });

        applyEligibility(binding.btnJoinVolunteer, event, null, Registration.TYPE_VOLUNTEER);
        if (binding.btnJoinVolunteer.isEnabled()) binding.btnJoinVolunteer.setText(R.string.d1_sign_in_volunteer);

        binding.btnJoinVolunteer.setOnClickListener(v -> {
            if (!binding.btnJoinVolunteer.isEnabled()) return;
            AuthGate.requireSignIn(this, PendingActionManager.ActionType.VOLUNTEER_REGISTER, event.getId());
        });
    }

    private void setupUserButtons(Event event, Registration registration) {
        binding.btnJoinParticipant.setOnClickListener(null);
        binding.btnJoinVolunteer.setOnClickListener(null);
        if (registration != null) {
            binding.tvRegistrationState.setText(R.string.event_already_registered);
            // Već prijavljen - prikazi status + dugme za odjavu
            binding.btnJoinParticipant.setEnabled(false);
            binding.btnJoinParticipant.setText(R.string.registration_type_participant);

            binding.btnJoinVolunteer.setEnabled(false);
            binding.btnJoinVolunteer.setText(R.string.registration_type_volunteer);

            // Dodaj dugme za odjavu
            if (Registration.TYPE_PARTICIPANT.equals(registration.getType())) {
                binding.btnJoinParticipant.setText(R.string.registration_unregister);
                binding.btnJoinParticipant.setEnabled(true);
                binding.btnJoinParticipant.setOnClickListener(v -> showCancelDialog(registration));
            } else if (Registration.TYPE_VOLUNTEER.equals(registration.getType())) {
                binding.btnJoinVolunteer.setText(R.string.registration_unregister);
                binding.btnJoinVolunteer.setEnabled(true);
                binding.btnJoinVolunteer.setOnClickListener(v -> showCancelDialog(registration));
            }
        } else {
            applyEligibility(binding.btnJoinParticipant, event, null, Registration.TYPE_PARTICIPANT);
            if (binding.btnJoinParticipant.isEnabled()) {
                binding.btnJoinParticipant.setOnClickListener(v -> registerAs(Registration.TYPE_PARTICIPANT));
            }
            applyEligibility(binding.btnJoinVolunteer, event, null, Registration.TYPE_VOLUNTEER);
            if (binding.btnJoinVolunteer.isEnabled()) {
                    binding.btnJoinVolunteer.setOnClickListener(v -> registerAs(Registration.TYPE_VOLUNTEER));
            }
        }
        if (registrationOperationInProgress) {
            binding.btnJoinParticipant.setEnabled(false);
            binding.btnJoinVolunteer.setEnabled(false);
        }
    }

    private void applyEligibility(MaterialButton button, Event event, Registration registration,
                                  String type) {
        EventActionEligibility.Reason reason = EventActionEligibility.forType(event, registration, type,
                System.currentTimeMillis());
        button.setEnabled(reason == EventActionEligibility.Reason.ELIGIBLE);
        int text;
        switch (reason) {
            case FULL: text = R.string.event_capacity_full; break;
            case CLOSED: text = R.string.d1_registration_closed; break;
            case PAST: text = R.string.d1_event_ended; break;
            case NO_VOLUNTEERS: text = R.string.d1_no_volunteers; break;
            case CANCELLED: case DELETED: text = R.string.d1_event_unavailable; break;
            default: text = Registration.TYPE_VOLUNTEER.equals(type)
                    ? R.string.event_join_as_volunteer : R.string.event_join_as_participant;
        }
        button.setText(text);
    }

    private void registerAs(String type) {
        if (registrationOperationInProgress || currentEvent == null || currentUid.isEmpty() || isOrganizer) return;
        if (EventActionEligibility.forType(currentEvent, myRegistration, type, System.currentTimeMillis())
                != EventActionEligibility.Reason.ELIGIBLE) return;
        registrationOperationInProgress = true;
        final String operationUid = currentUid;
        binding.btnJoinParticipant.setEnabled(false);
        binding.btnJoinVolunteer.setEnabled(false);
        binding.progressBarDetails.setVisibility(View.VISIBLE);
        registrationRepository.register(eventId, type)
                .addOnSuccessListener(aVoid -> {
                    registrationOperationInProgress = false;
                    if (isDestroyed() || !operationUid.equals(currentUid)) return;
                    binding.progressBarDetails.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                    if (getIntent().getBooleanExtra("PENDING_ACTION_REPLAY", false)) {
                        new PendingActionManager(this).finishConsumption(true);
                        getIntent().removeExtra("PENDING_ACTION_REPLAY");
                    }
                    // Realtime listener will auto-refresh
                    if (currentEvent != null) checkRegistrationAndSetupButtons(currentUid, currentEvent);
                })
                .addOnFailureListener(e -> {
                    registrationOperationInProgress = false;
                    if (isDestroyed() || !operationUid.equals(currentUid)) return;
                    binding.progressBarDetails.setVisibility(View.GONE);
                    if (getIntent().getBooleanExtra("PENDING_ACTION_REPLAY", false)) {
                        new PendingActionManager(this).finishConsumption(false);
                    }
                    handleRegistrationError(e.getMessage());
                });
    }

    private void handleRegistrationError(String errorMsg) {
        Toast.makeText(this, ErrorMapper.toResId(errorMsg), Toast.LENGTH_SHORT).show();
        showDetailsMessage(ErrorMapper.toResId(errorMsg), true);
    }

    private void showCancelDialog(Registration registration) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.registration_cancel_confirm_title)
                .setMessage(R.string.registration_cancel_confirm_body)
                .setPositiveButton(R.string.registration_unregister, (dialog, which) -> cancelRegistration())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void cancelRegistration() {
        if (registrationOperationInProgress || currentUid.isEmpty() || myRegistration == null) return;
        registrationOperationInProgress = true;
        final String operationUid = currentUid;
        binding.btnJoinParticipant.setEnabled(false);
        binding.btnJoinVolunteer.setEnabled(false);
        binding.progressBarDetails.setVisibility(View.VISIBLE);
        registrationRepository.cancelRegistration(eventId)
                .addOnSuccessListener(aVoid -> {
                    registrationOperationInProgress = false;
                    if (isDestroyed() || !operationUid.equals(currentUid)) return;
                    binding.progressBarDetails.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.registration_cancelled, Toast.LENGTH_SHORT).show();
                    myRegistration = null;
                    if (currentEvent != null) checkRegistrationAndSetupButtons(currentUid, currentEvent);
                })
                .addOnFailureListener(e -> {
                    registrationOperationInProgress = false;
                    if (isDestroyed() || !operationUid.equals(currentUid)) return;
                    handleRegistrationError(e.getMessage());
                });
    }

    private void openEditEventActivity(Event event) {
        Intent intent = new Intent(this, EventManagerActivity.class);
        intent.putExtra("IS_EDIT_MODE", true);
        intent.putExtra("EVENT_ID", event.getId());
        intent.putExtra("EVENT_TITLE", event.getTitle());
        intent.putExtra("EVENT_DESC", event.getDescription());
        intent.putExtra("EVENT_DATE_LONG", event.getDateLong());
        intent.putExtra("EVENT_DISPLAY_DATE", event.getDisplayDate());
        intent.putExtra("EVENT_CATEGORY", event.getCategory());
        intent.putExtra("EVENT_LOCATION", event.getLocation());
        intent.putExtra("EVENT_MAX_PARTICIPANTS", event.getMaxParticipants());
        intent.putExtra("EVENT_MAX_VOLUNTEERS", event.getMaxVolunteers());
        intent.putExtra("EVENT_CURRENT_PARTICIPANTS", event.getCurrentParticipants());
        intent.putExtra("EVENT_CURRENT_VOLUNTEERS", event.getCurrentVolunteers());
        intent.putExtra("EVENT_IMAGE_URL", event.getImageUrl());
        startActivity(intent);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.event_delete_confirm_title)
                .setMessage(R.string.event_delete_confirm_body)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteEvent())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showRegistrationsDialog() {
        if (currentEvent == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_registrations, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewRegistrations);
        TextView tvEmpty = dialogView.findViewById(R.id.tvRegistrationsEmpty);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBarRegistrations);
        View retry = dialogView.findViewById(R.id.btnRetryRegistrations);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.organizer_registrations_title)
                .setView(dialogView)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        registrationsDialog = dialog;
        retry.setOnClickListener(v -> { dialog.dismiss(); showRegistrationsDialog(); });

        RegistrationAdapter adapter = new RegistrationAdapter(registration ->
                confirmRemoveRegistration(registration));

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBar.setVisibility(View.VISIBLE);
        stopRegistrationsDialogListener();
        registrationsDialogListener = registrationRepository.listenEventRegistrations(eventId, (registrations, error) -> {
            if (isDestroyed() || registrationsDialog != dialog) return;
            progressBar.setVisibility(View.GONE);
            if (error != null) {
                tvEmpty.setText(R.string.registration_error_generic);
                tvEmpty.setVisibility(View.VISIBLE);
                retry.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }
            tvEmpty.setText(R.string.organizer_registrations_empty);
            retry.setVisibility(View.GONE);
            if (registrations == null || registrations.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(registrations);
            }
        });

        dialog.setOnDismissListener(d -> { stopRegistrationsDialogListener(); registrationsDialog = null; });
        dialog.show();
    }

    private void stopRegistrationsDialogListener() {
        if (registrationsDialogListener != null) {
            registrationsDialogListener.remove();
            registrationsDialogListener = null;
        }
    }

    private void confirmRemoveRegistration(Registration registration) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.organizer_remove_confirm_title)
                .setMessage(getString(R.string.organizer_remove_confirm_body,
                        getString(R.string.d1_attendee)))
                .setPositiveButton(R.string.action_delete, (d, w) -> removeRegistration(registration))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void removeRegistration(Registration registration) {
        if (registration == null || registration.getUserId() == null) return;

        binding.progressBarDetails.setVisibility(View.VISIBLE);
        registrationRepository.removeRegistration(eventId, registration.getUserId())
                .addOnSuccessListener(aVoid -> {
                    binding.progressBarDetails.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.registration_removed, Toast.LENGTH_SHORT).show();
                    // Realtime listener will auto-refresh
                })
                .addOnFailureListener(e -> {
                    binding.progressBarDetails.setVisibility(View.GONE);
                    Toast.makeText(this, ErrorMapper.toResId(e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteEvent() {
        if (registrationOperationInProgress || !isOrganizer || currentEvent == null
                || currentEvent.getCurrentParticipants() != 0 || currentEvent.getCurrentVolunteers() != 0) return;
        registrationOperationInProgress = true;
        final String operationUid = currentUid;
        binding.btnDeleteEvent.setEnabled(false);
        binding.progressBarDetails.setVisibility(View.VISIBLE);
        eventRepository.deleteEvent(eventId)
                .addOnSuccessListener(aVoid -> {
                    registrationOperationInProgress = false;
                    if (isDestroyed() || !operationUid.equals(currentUid)) return;
                    binding.progressBarDetails.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.event_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    registrationOperationInProgress = false;
                    if (isDestroyed() || !operationUid.equals(currentUid)) return;
                    showDetailsMessage(ErrorMapper.toResId(e.getMessage()), true);
                });
    }

    private String formatEventDate(Event event) {
        java.text.DateFormat formatter = android.text.format.DateFormat.getMediumDateFormat(this);
        formatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String date = event.getDateLong() > 0 ? formatter.format(new java.util.Date(event.getDateLong())) : "";
        if (date == null || date.isEmpty()) {
            long dateLong = event.getDateLong();
            if (dateLong > 0) {
                date = android.text.format.DateFormat.getMediumDateFormat(this).format(dateLong);
            }
        }
        return date != null ? date : "";
    }

    private int registrationStateText(Event event) {
        EventActionEligibility.Reason reason = EventActionEligibility.overall(
                event, System.currentTimeMillis());
        switch (reason) {
            case CANCELLED: case DELETED: return R.string.d1_event_unavailable;
            case PAST: return R.string.d1_event_ended;
            case CLOSED: return R.string.e_registration_closed;
            case FULL: return R.string.event_capacity_full;
            default: return R.string.e_registration_open;
        }
    }

    private void renderFavorite() {
        boolean failed = Boolean.TRUE.equals(favoriteViewModel.getStreamError().getValue());
        java.util.Set<String> ids = favoriteViewModel.getFavoriteIds().getValue();
        boolean saved = ids != null && ids.contains(eventId);
        binding.btnDetailFavorite.setEnabled(sessionReady && currentEvent != null);
        int description = failed ? R.string.action_retry
                : saved ? R.string.event_remove_favorite : R.string.event_add_favorite;
        binding.btnDetailFavorite.setContentDescription(getString(description));
        binding.btnDetailFavorite.setImageResource(saved
                ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        if (failed) binding.tvFavoriteError.setVisibility(View.VISIBLE);
        else if (favoriteViewModel.getResult().getValue() == null
                || favoriteViewModel.getResult().getValue().getErrorCode() == null)
            binding.tvFavoriteError.setVisibility(View.GONE);
    }

    private int progressInt(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }
}
