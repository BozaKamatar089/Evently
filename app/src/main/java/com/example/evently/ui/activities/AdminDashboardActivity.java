package com.example.evently.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.evently.R;
import com.example.evently.data.admin.AdminRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.domain.admin.AdminCapability;
import com.example.evently.domain.common.AccountActionPolicy;
import com.example.evently.data.model.AdminStats;
import com.example.evently.data.model.Event;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.databinding.ActivityAdminDashboardBinding;
import com.example.evently.databinding.DialogAdminRejectBinding;
import com.example.evently.ui.adapters.AdminEventAdapter;
import com.example.evently.ui.adapters.AdminVerificationAdapter;
import com.example.evently.viewmodel.AdminViewModel;
import com.example.evently.viewmodel.AdminViewModelFactory;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.viewmodel.AccountScreenState;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private AdminViewModel viewModel;

    private SessionViewModel sessionViewModel;
    private boolean adminLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionViewModel = new ViewModelProvider(this,
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);
        sessionViewModel.start();

        viewModel = new ViewModelProvider(this,
                new AdminViewModelFactory(new AdminRepositoryImpl()))
                .get(AdminViewModel.class);

        setupToolbar();
        setupRecyclerViews();
        binding.btnRetryAdmin.setOnClickListener(v -> {
            if (adminLoaded) viewModel.loadAll(); else sessionViewModel.retry();
        });
        observeViewModel();
        sessionViewModel.getState().observe(this, state -> {
            if (state == null) return;
            if (state.isAuthenticated() && state.isAdmin(AdminCapability.configuredUid())) {
                if (!adminLoaded) { adminLoaded = true; viewModel.loadAll(); }
            } else {
                adminLoaded = false;
                viewModel.clearSession();
                if (state.isGuest() || state.isAuthenticated()) {
                    Toast.makeText(this, R.string.admin_access_denied, Toast.LENGTH_SHORT).show();
                    finish();
                } else renderScreenState(state.getStatus()
                        == com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED
                        ? AccountScreenState.error("PROFILE_LOAD_ERROR") : AccountScreenState.loading());
            }
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private AdminVerificationAdapter verificationAdapter;
    private AdminEventAdapter eventAdapter;

    private void setupRecyclerViews() {
        verificationAdapter = new AdminVerificationAdapter(new AdminVerificationAdapter.Listener() {
            @Override public void onViewDocument(@NonNull VerificationRequest request) {
                openDocument(request);
            }
            @Override public void onApprove(@NonNull VerificationRequest request) {
                showApproveDialog(request);
            }
            @Override public void onReject(@NonNull VerificationRequest request) {
                showRejectDialog(request);
            }
        });
        binding.recyclerVerifications.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerVerifications.setAdapter(verificationAdapter);

        eventAdapter = new AdminEventAdapter();
        binding.recyclerAdminEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAdminEvents.setAdapter(eventAdapter);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getScreenState().observe(this, this::renderScreenState);
        viewModel.getVerificationState().observe(this, this::renderVerificationState);
        viewModel.getStatsState().observe(this, this::renderStatsState);
        viewModel.getEventsState().observe(this, this::renderEventsState);

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty() && !"ADMIN_LOAD_ERROR".equals(error)
                    && !"ADMIN_PARTIAL_LOAD_ERROR".equals(error)) {
                Toast.makeText(this, R.string.admin_error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getActionSuccess().observe(this, action -> {
            if (VerificationRequest.STATUS_APPROVED.equals(action)) {
                Toast.makeText(this, R.string.admin_approved, Toast.LENGTH_SHORT).show();
            } else if (VerificationRequest.STATUS_REJECTED.equals(action)) {
                Toast.makeText(this, R.string.admin_rejected, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getStats().observe(this, this::bindStats);
        viewModel.getVerifications().observe(this, this::bindVerifications);
        viewModel.getEvents().observe(this, this::bindEvents);
    }

    private void renderScreenState(AccountScreenState state) {
        if (state == null) return;
        binding.progressBar.setVisibility(
                state.getStatus() == AccountScreenState.Status.LOADING ? View.VISIBLE : View.GONE);
        if (state.getStatus() == AccountScreenState.Status.LOADING) {
            binding.contentAdmin.setVisibility(View.GONE);
            binding.stateAdmin.setVisibility(View.GONE);
        } else if (state.getStatus() == AccountScreenState.Status.ERROR) {
            binding.contentAdmin.setVisibility(View.GONE);
            binding.stateAdmin.setVisibility(View.VISIBLE);
        } else {
            binding.stateAdmin.setVisibility(View.GONE);
            binding.contentAdmin.setVisibility(View.VISIBLE);
        }
    }

    private void renderVerificationState(AccountScreenState state) {
        if (state == null) return;
        java.util.List<VerificationRequest> list = viewModel.getVerifications().getValue();
        boolean empty = list == null || list.isEmpty();
        boolean error = state.getStatus() == AccountScreenState.Status.ERROR;
        binding.tvVerificationSectionState.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.tvNoVerifications.setVisibility(!error && state.getStatus() == AccountScreenState.Status.EMPTY ? View.VISIBLE : View.GONE);
        binding.recyclerVerifications.setVisibility(!error && !empty ? View.VISIBLE : View.GONE);
        binding.tvPendingCount.setVisibility(!error && !empty ? View.VISIBLE : View.GONE);
    }

    private void renderStatsState(AccountScreenState state) {
        binding.tvStatsSectionState.setVisibility(state != null
                && state.getStatus() == AccountScreenState.Status.ERROR ? View.VISIBLE : View.GONE);
    }

    private void renderEventsState(AccountScreenState state) {
        if (state == null) return;
        java.util.List<Event> list = viewModel.getEvents().getValue();
        boolean empty = list == null || list.isEmpty();
        boolean error = state.getStatus() == AccountScreenState.Status.ERROR;
        binding.tvEventsSectionState.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.tvNoEvents.setVisibility(!error && state.getStatus() == AccountScreenState.Status.EMPTY ? View.VISIBLE : View.GONE);
        binding.recyclerAdminEvents.setVisibility(!error && !empty ? View.VISIBLE : View.GONE);
    }

    private void bindStats(AdminStats stats) {
        if (stats == null) return;
        binding.tvStatUsers.setText(String.valueOf(stats.getUserCount()));
        binding.tvStatEvents.setText(String.valueOf(stats.getEventCount()));
        binding.tvStatRegistrations.setText(String.valueOf(stats.getRegistrationCount()));
    }

    private void bindVerifications(java.util.List<VerificationRequest> list) {
        if (list == null) return;
        binding.tvPendingCount.setText(getString(R.string.admin_verifications_pending_count, list.size()));
        binding.tvPendingCount.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvNoVerifications.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerVerifications.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        verificationAdapter.submitList(list);
        renderVerificationState(viewModel.getVerificationState().getValue());
    }

    private void bindEvents(java.util.List<Event> list) {
        if (list == null) return;
        binding.tvNoEvents.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerAdminEvents.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        eventAdapter.submitList(list);
        renderEventsState(viewModel.getEventsState().getValue());
    }

    private void showApproveDialog(VerificationRequest request) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.admin_approve_title)
                .setMessage(getString(R.string.admin_approve_body, request.getOrgName()))
                .setPositiveButton(R.string.action_approve, (dialogInterface, which) ->
                        viewModel.approveVerification(request.getUserId(), request.getOrgName()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showRejectDialog(VerificationRequest request) {
        DialogAdminRejectBinding dialogBinding = DialogAdminRejectBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.admin_reject_title)
                .setMessage(getString(R.string.admin_reject_body, request.getOrgName()))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_reject, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String reason = dialogBinding.etRejectReason.getText() != null
                            ? dialogBinding.etRejectReason.getText().toString().trim() : "";
                    if (!AccountActionPolicy.hasRejectReason(reason)) {
                        dialogBinding.etRejectReason.setError(getString(R.string.admin_reject_reason_required));
                        dialogBinding.etRejectReason.requestFocus();
                        return;
                    }
                    dialogBinding.etRejectReason.setError(null);
                    viewModel.rejectVerification(
                            request.getUserId(), request.getOrgName(), reason);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void openDocument(@NonNull VerificationRequest request) {
        if (request.getDocumentUrl() == null || request.getDocumentUrl().isEmpty()) return;
        startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(request.getDocumentUrl())));
    }
}
