package com.example.evently.ui.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.evently.R;
import com.example.evently.data.image.CloudinaryImageUploader;
import com.example.evently.data.image.ImageUploader;
import com.example.evently.data.model.User;
import com.example.evently.data.user.UserRepositoryImpl;
import com.example.evently.data.verification.VerificationRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.databinding.ActivityEditProfileBinding;
import com.example.evently.viewmodel.ProfileViewModel;
import com.example.evently.viewmodel.ProfileViewModelFactory;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.util.ErrorMapper;
import com.example.evently.viewmodel.AccountScreenState;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private ProfileViewModel viewModel;
    private SessionViewModel sessionViewModel;
    private ImageUploader imageUploader;

    private String currentPhotoUrl;
    private String newPhotoUrl;
    private Uri selectedImageUri;
    private final EditProfileDraftGuard draftGuard = new EditProfileDraftGuard();
    private String boundUid = "";
    private boolean isUploading;
    private boolean isSaving;
    private boolean destroyed;
    private int uploadGeneration;
    private boolean populating;
    private boolean sessionReady;
    private String uploadRequestId;

    private static final String STATE_UID = "edit_profile_uid";
    private static final String STATE_CURRENT_PHOTO = "edit_profile_current_photo";
    private static final String STATE_NEW_PHOTO = "edit_profile_new_photo";
    private static final String STATE_SELECTED_IMAGE = "edit_profile_selected_image";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.ivProfileAvatar.setImageURI(uri);
                    uploadNewPhoto(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageUploader = new CloudinaryImageUploader();
        if (savedInstanceState != null) {
            boundUid = savedInstanceState.getString(STATE_UID, "");
            currentPhotoUrl = savedInstanceState.getString(STATE_CURRENT_PHOTO);
            newPhotoUrl = savedInstanceState.getString(STATE_NEW_PHOTO);
            String uri = savedInstanceState.getString(STATE_SELECTED_IMAGE);
            selectedImageUri = uri != null ? Uri.parse(uri) : null;
            draftGuard.restore(boundUid);
        }
        viewModel = new ViewModelProvider(this,
                new ProfileViewModelFactory(new UserRepositoryImpl(), new VerificationRepositoryImpl(),
                        AppDependencies.sessionRepository()))
                .get(ProfileViewModel.class);
        sessionViewModel = new ViewModelProvider(this,
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);
        sessionViewModel.start();

        setupToolbar();
        setupClickListeners();
        setupDraftTracking();
        observeViewModel();

        sessionViewModel.getState().observe(this, state -> {
            sessionReady = state != null && state.isAuthenticated();
            if (state != null && state.isAuthenticated()) {
                String nextUid = state.getUid() != null ? state.getUid() : "";
                if (!boundUid.isEmpty() && !nextUid.equals(boundUid)) {
                    viewModel.stopListening();
                    binding.etName.setText(""); binding.etPhone.setText(""); binding.etBio.setText("");
                    finish(); return;
                }
                if (!nextUid.equals(boundUid)) {
                    if (uploadRequestId != null) {
                        imageUploader.cancelUpload(uploadRequestId);
                        uploadRequestId = null;
                    }
                    boundUid = nextUid;
                    draftGuard.clear();
                    selectedImageUri = null;
                    currentPhotoUrl = null;
                    newPhotoUrl = null;
                    uploadGeneration++;
                    isUploading = false;
                    isSaving = false;
                }
                viewModel.startListening(nextUid);
                renderScreenState(viewModel.getScreenState().getValue());
                if (selectedImageUri != null && newPhotoUrl == null && !isUploading && !isSaving)
                    uploadNewPhoto(selectedImageUri);
            } else if (state != null && state.isGuest()) finish();
            else renderScreenState(state != null && state.getStatus()
                    == com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED
                    ? AccountScreenState.error("PROFILE_LOAD_ERROR") : AccountScreenState.loading());
            updateControls();
        });
    }

    private void setupDraftTracking() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!populating) draftGuard.markDirty();
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        binding.etName.addTextChangedListener(watcher);
        binding.etPhone.addTextChangedListener(watcher);
        binding.etBio.addTextChangedListener(watcher);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        binding.cardAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void observeViewModel() {
        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                if (draftGuard.shouldApplySnapshot(boundUid)) populateForm(user);
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                updateControls();
            }
        });
        viewModel.getScreenState().observe(this, this::renderScreenState);
        viewModel.getSaving().observe(this, saving -> {
            isSaving = Boolean.TRUE.equals(saving);
            if (isSaving) binding.tvProfileSaveError.setVisibility(View.GONE);
            updateControls();
        });

        viewModel.getProfileUpdated().observe(this, updated -> {
            if (updated != null && updated) {
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                if ("PROFILE_SAVE_ERROR".equals(error)) {
                    binding.tvProfileSaveError.setText(ErrorMapper.toResId(error));
                    binding.tvProfileSaveError.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void renderScreenState(AccountScreenState state) {
        if (state == null) return;
        if (state.getStatus() == AccountScreenState.Status.LOADING) {
            binding.contentEditProfile.setVisibility(View.GONE);
            binding.stateEditProfile.hide();
        } else if (state.getStatus() == AccountScreenState.Status.ERROR
                || state.getStatus() == AccountScreenState.Status.EMPTY) {
            binding.contentEditProfile.setVisibility(View.GONE);
            binding.stateEditProfile.show(0,
                    getString(R.string.d1_profile_load_error_title),
                    getString(R.string.d1_profile_load_error_body), R.string.action_retry,
                    v -> { if (!sessionReady || viewModel.getUser().getValue() == null) sessionViewModel.retry(); else viewModel.retry(); });
        } else if (state.getStatus() == AccountScreenState.Status.CONTENT) {
            binding.stateEditProfile.hide();
            binding.contentEditProfile.setVisibility(View.VISIBLE);
        }
    }

    private void populateForm(User user) {
        if (!user.getUid().equals(boundUid)) return;
        populating = true;
        binding.etName.setText(user.getName() != null ? user.getName() : "");
        binding.etPhone.setText(user.getPhone() != null ? user.getPhone() : "");
        binding.etBio.setText(user.getBio() != null ? user.getBio() : "");
        currentPhotoUrl = user.getPhotoUrl();
        populating = false;

        Glide.with(this).clear(binding.ivProfileAvatar);
        binding.ivProfileAvatar.setImageResource(R.drawable.profile_placeholder);
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder)
                    .into(binding.ivProfileAvatar);
        }
    }

    private void uploadNewPhoto(Uri uri) {
        if (isUploading || isSaving || !sessionReady) return;
        newPhotoUrl = null;
        final int generation = ++uploadGeneration;
        isUploading = true;
        draftGuard.markDirty();
        updateControls();
        uploadRequestId = imageUploader.uploadImage(uri, new ImageUploader.OnUploadCallback() {
            @Override
            public void onSuccess(@NonNull String secureUrl) {
                runOnUiThread(() -> {
                    if (destroyed || generation != uploadGeneration) return;
                    newPhotoUrl = secureUrl;
                    isUploading = false;
                    uploadRequestId = null;
                    updateControls();
                });
            }

            @Override
            public void onError(@NonNull Exception error) {
                runOnUiThread(() -> {
                    if (destroyed || generation != uploadGeneration) return;
                    isUploading = false;
                    uploadRequestId = null;
                    selectedImageUri = null;
                    restoreCurrentAvatar();
                    updateControls();
                    binding.tvProfileSaveError.setText(R.string.upload_error);
                    binding.tvProfileSaveError.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onUploadProgress(int percent) {
                // optional progress
            }
        });

        if (uploadRequestId == null) {
            isUploading = false;
            selectedImageUri = null;
            restoreCurrentAvatar();
            updateControls();
            Toast.makeText(this, R.string.profile_save_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String preview = newPhotoUrl != null ? newPhotoUrl : currentPhotoUrl;
            Glide.with(this).clear(binding.ivProfileAvatar);
            Glide.with(this).load(preview).placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder).circleCrop().into(binding.ivProfileAvatar);
        }
        if (savedInstanceState != null && selectedImageUri != null
                && (newPhotoUrl == null || newPhotoUrl.isEmpty())) {
            binding.ivProfileAvatar.setImageURI(selectedImageUri);
            uploadNewPhoto(selectedImageUri);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_UID, boundUid);
        outState.putString(STATE_CURRENT_PHOTO, currentPhotoUrl);
        outState.putString(STATE_NEW_PHOTO, newPhotoUrl);
        if (selectedImageUri != null) outState.putString(STATE_SELECTED_IMAGE, selectedImageUri.toString());
        super.onSaveInstanceState(outState);
    }

    private void restoreCurrentAvatar() {
        Glide.with(this).clear(binding.ivProfileAvatar);
        if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
            Glide.with(this).load(currentPhotoUrl).circleCrop()
                    .placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder).into(binding.ivProfileAvatar);
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.profile_placeholder);
        }
    }

    private void saveProfile() {
        if (!sessionReady) return;
        if (isUploading) {
            Toast.makeText(this, R.string.d1_profile_wait_for_upload, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isSaving) return;
        String name = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null
                ? binding.etPhone.getText().toString().trim() : "";
        String bio = binding.etBio.getText() != null
                ? binding.etBio.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.auth_error_empty_fields));
            return;
        }

        binding.tilName.setError(null);
        String photoToSave = (newPhotoUrl != null && !newPhotoUrl.isEmpty()) ? newPhotoUrl : currentPhotoUrl;

        isSaving = true;
        updateControls();
        viewModel.updateProfile(name, bio, phone, photoToSave);
    }

    private void updateControls() {
        if (binding == null) return;
        boolean busy = isUploading || isSaving || !sessionReady || Boolean.TRUE.equals(viewModel.getLoading().getValue());
        com.example.evently.domain.session.SessionState session = sessionViewModel.getState().getValue();
        boolean failed = session != null && session.getStatus() == com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED;
        binding.progressBar.setVisibility(busy && !failed ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!busy);
        binding.cardAvatar.setEnabled(!busy);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        uploadGeneration++;
        if (uploadRequestId != null) {
            imageUploader.cancelUpload(uploadRequestId);
            uploadRequestId = null;
        }
        super.onDestroy();
    }
}
