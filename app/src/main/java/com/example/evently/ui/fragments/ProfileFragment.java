package com.example.evently.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.evently.R;
import com.example.evently.data.model.User;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.data.user.UserRepositoryImpl;
import com.example.evently.data.verification.VerificationRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.databinding.FragmentProfileBinding;
import com.example.evently.ui.activities.EditProfileActivity;
import com.example.evently.ui.auth.LoginActivity;
import com.example.evently.ui.activities.VerificationRequestActivity;
import com.example.evently.ui.activities.SettingsActivity;
import com.example.evently.ui.activities.AdminDashboardActivity;
import com.example.evently.domain.admin.AdminCapability;
import com.example.evently.util.ErrorMapper;
import com.example.evently.viewmodel.ProfileViewModel;
import com.example.evently.viewmodel.ProfileViewModelFactory;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.viewmodel.AccountScreenState;
import com.example.evently.domain.session.SessionState;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private SessionViewModel sessionViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                new ProfileViewModelFactory(new UserRepositoryImpl(), new VerificationRepositoryImpl(),
                        AppDependencies.sessionRepository()))
                .get(ProfileViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity(),
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        binding.btnVerifyOrganizer.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), VerificationRequestActivity.class)));

        binding.btnSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
        binding.btnAdminDashboard.setOnClickListener(v -> startActivity(new Intent(requireContext(), AdminDashboardActivity.class)));

        observeViewModel();
        sessionViewModel.getState().observe(getViewLifecycleOwner(), this::renderSession);
    }

    private void observeViewModel() {
        viewModel.getScreenState().observe(getViewLifecycleOwner(), ignored -> renderProfile());
        viewModel.getUser().observe(getViewLifecycleOwner(), ignored -> renderProfile());
        viewModel.getVerificationRequest().observe(getViewLifecycleOwner(), ignored -> renderProfile());

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && !"PROFILE_LOAD_ERROR".equals(error)) {
                Toast.makeText(requireContext(),
                        ErrorMapper.toResId(error), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLogoutDone().observe(getViewLifecycleOwner(), done -> {
            if (done != null && done) {
                androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.nav_home);
            }
        });
    }

    private void renderProfile() {
        if (binding == null) return;
        com.example.evently.viewmodel.ProfileScreenState state = viewModel.snapshot(
                sessionViewModel.getState().getValue(), AdminCapability.configuredUid());
        binding.emptyStateProfile.hide();
        binding.scrollProfile.setVisibility(View.GONE);
        binding.progressBarProfile.setVisibility(View.GONE);
        binding.btnAdminDashboard.setVisibility(View.GONE);
        binding.btnVerifyOrganizer.setVisibility(View.GONE);
        binding.cardVerificationDetail.setVisibility(View.GONE);
        binding.chipProfileVerification.setVisibility(View.GONE);
        if (state.getStatus() != com.example.evently.viewmodel.ProfileScreenState.Status.AUTHENTICATED) {
            Glide.with(this).clear(binding.ivProfileAvatar);
            binding.ivProfileAvatar.setImageResource(R.drawable.profile_placeholder);
            binding.tvProfileName.setText(null);
            binding.tvProfileEmail.setText(null);
            binding.tvProfileBio.setText(null);
            binding.tvProfilePhone.setText(null);
            binding.chipProfileRole.setText(null);
            binding.tvVerificationDetailTitle.setText(null);
            binding.tvVerificationDetailBody.setText(null);
        }
        switch (state.getStatus()) {
            case GUEST: showGuestState(); break;
            case AUTH_LOADING: binding.progressBarProfile.setVisibility(View.VISIBLE); break;
            case ERROR:
                binding.emptyStateProfile.show(R.drawable.mascot_error,
                        getString(state.isMissing() ? R.string.d1_profile_missing_title : R.string.d1_profile_load_error_title),
                        getString(state.isMissing() ? R.string.d1_profile_missing_body : R.string.d1_profile_load_error_body),
                        R.string.action_retry, v -> retryProfile());
                break;
            case AUTHENTICATED:
                renderUser(state.getUser());
                if (state.getRequest() != null) renderVerification(state.getRequest());
                binding.btnSettings.setVisibility(View.VISIBLE);
                binding.btnAdminDashboard.setVisibility(state.isAdmin() ? View.VISIBLE : View.GONE);
                binding.scrollProfile.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void renderUser(User user) {
        binding.btnVerifyOrganizer.setText(R.string.profile_become_organizer);
        binding.tvProfileName.setText(user.getName() != null && !user.getName().isEmpty()
                ? user.getName() : getString(R.string.profile_new_user));
        binding.tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        if (user.getRole() != null) {
            if (User.ROLE_ORGANIZER.equals(user.getRole())) {
                binding.chipProfileRole.setText(R.string.role_organizer);
                binding.btnVerifyOrganizer.setVisibility(View.GONE);
            } else {
                binding.chipProfileRole.setText(R.string.role_participant);
                // Organizer button visible only if no approved verification
                binding.btnVerifyOrganizer.setVisibility(View.VISIBLE);
            }
        } else {
            binding.chipProfileRole.setText(R.string.role_participant);
            binding.btnVerifyOrganizer.setVisibility(View.VISIBLE);
        }

        boolean hasPhone = user.getPhone() != null && !user.getPhone().trim().isEmpty();
        boolean hasBio = user.getBio() != null && !user.getBio().trim().isEmpty();
        binding.tvProfilePhone.setText(hasPhone ? user.getPhone() : "");
        binding.tvProfileBio.setText(hasBio ? user.getBio() : "");
        binding.tvProfilePhone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        binding.tvProfilePhoneLabel.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        binding.tvProfileBio.setVisibility(hasBio ? View.VISIBLE : View.GONE);
        binding.cardInfo.setVisibility(hasPhone || hasBio ? View.VISIBLE : View.GONE);

        Glide.with(this).clear(binding.ivProfileAvatar);
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.profile_placeholder).error(R.drawable.profile_placeholder)
                    .circleCrop()
                    .into(binding.ivProfileAvatar);
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.profile_placeholder);
        }
    }

    private void renderVerification(VerificationRequest request) {
        String status = request.getStatus();
        if (status == null) return;

        // The adjacent status explanation carries this information without a duplicate badge.
        binding.chipProfileVerification.setVisibility(View.GONE);
        binding.cardVerificationDetail.setVisibility(View.VISIBLE);
        binding.btnVerifyOrganizer.setVisibility(View.GONE);

        switch (status) {
            case VerificationRequest.STATUS_PENDING:
                binding.btnVerifyOrganizer.setVisibility(View.VISIBLE);
                binding.btnVerifyOrganizer.setText(R.string.e_review_verification);
                binding.chipProfileVerification.setText(R.string.verification_pending);
                binding.chipProfileVerification.setChipBackgroundColorResource(R.color.status_pending);
                binding.tvVerificationDetailTitle.setText(R.string.verification_pending);
                binding.tvVerificationDetailBody.setText(R.string.verification_pending_body);
                break;
            case VerificationRequest.STATUS_APPROVED:
                binding.chipProfileVerification.setText(R.string.verification_verified);
                binding.chipProfileVerification.setChipBackgroundColorResource(R.color.status_success);
                binding.tvVerificationDetailTitle.setText(R.string.verification_verified);
                binding.tvVerificationDetailBody.setText(R.string.verification_verified_body);
                break;
            case VerificationRequest.STATUS_REJECTED:
                binding.chipProfileVerification.setText(R.string.verification_rejected);
                binding.chipProfileVerification.setChipBackgroundColorResource(R.color.status_error);
                binding.tvVerificationDetailTitle.setText(R.string.verification_rejected);
                String reason = request.getRejectionReason() != null
                        ? request.getRejectionReason() : getString(R.string.verification_rejected_body);
                binding.tvVerificationDetailBody.setText(reason);
                // Allow re-request
                binding.btnVerifyOrganizer.setVisibility(View.VISIBLE);
                binding.btnVerifyOrganizer.setText(R.string.e_resubmit_verification);
                break;
            default:
                binding.chipProfileVerification.setText(R.string.verification_none);
                binding.chipProfileVerification.setChipBackgroundColorResource(R.color.colorSecondary);
                binding.cardVerificationDetail.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        renderSession(sessionViewModel.getState().getValue());
    }

    private void renderSession(SessionState state) {
        if (state != null && state.isAuthenticated()) viewModel.startListening(state.getUid());
        else viewModel.stopListening();
        renderProfile();
    }

    private void retryProfile() {
        SessionState session = sessionViewModel.getState().getValue();
        if (session == null || !session.isAuthenticated()
                || viewModel.getUser().getValue() == null) sessionViewModel.retry();
        else viewModel.retry();
    }

    private void showGuestState() {
        binding.progressBarProfile.setVisibility(View.GONE);
        binding.scrollProfile.setVisibility(View.GONE);
        binding.btnSettings.setVisibility(View.GONE);
        binding.btnAdminDashboard.setVisibility(View.GONE);
        binding.emptyStateProfile.show(
                R.drawable.mascot_happy,
                getString(R.string.profile_guest_title),
                getString(R.string.profile_guest_body), R.string.auth_login,
                v -> startActivity(new Intent(requireContext(), LoginActivity.class)));
    }

    @Override
    public void onStop() {
        viewModel.stopListening();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        viewModel.stopListening();
        super.onDestroyView();
        binding = null;
    }
}
