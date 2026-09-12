package com.example.evently.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.BuildConfig;
import com.example.evently.R;
import com.example.evently.data.AppDependencies;
import com.example.evently.data.model.User;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.databinding.ActivitySettingsBinding;
import com.example.evently.domain.auth.AuthIdentity;
import com.example.evently.domain.preferences.AppPreferencesRepository;
import com.example.evently.ui.preferences.AppDisplay;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.viewmodel.SettingsViewModel;
import com.example.evently.viewmodel.SettingsViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Account-control hub. Firebase and persistence remain behind repositories/ViewModels. */
public final class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private SessionViewModel session;
    private SettingsViewModel settings;
    private String boundUid;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        session = new ViewModelProvider(this,
                new SessionViewModelFactory(AppDependencies.sessionRepository())).get(SessionViewModel.class);
        settings = new ViewModelProvider(this,
                new SettingsViewModelFactory(AppDependencies.authRepository(), AppDependencies.appPreferences()))
                .get(SettingsViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.settingsRetry.setOnClickListener(v -> { settings.refresh(); session.retry(); });
        binding.rowEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        binding.rowChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));
        binding.rowLanguage.setOnClickListener(v -> showLanguageDialog());
        binding.rowTheme.setOnClickListener(v -> showThemeDialog());
        binding.rowOrganizer.setOnClickListener(v -> startActivity(new Intent(this, VerificationRequestActivity.class)));
        binding.btnSettingsLogout.setOnClickListener(v -> confirmLogout());
        binding.tvVersionValue.setText(BuildConfig.VERSION_NAME);
        settings.getIdentity().observe(this, ignored -> renderAccount());
        settings.getLanguage().observe(this, this::bindLanguage);
        settings.getTheme().observe(this, this::bindTheme);
        session.getState().observe(this, value -> {
            renderAccount();
        });
        session.start();
    }

    @Override protected void onResume() { super.onResume(); settings.refresh(); }

    private void renderAccount() {
        com.example.evently.domain.session.SessionState current = session.getState().getValue();
        AuthIdentity identity = settings.getIdentity().getValue();
        binding.settingsContent.setVisibility(View.GONE);
        binding.settingsLoading.setVisibility(View.VISIBLE);
        binding.settingsError.setVisibility(View.GONE);
        binding.tvSettingsName.setText(null);
        binding.tvSettingsEmail.setText(null);
        binding.tvSettingsProvider.setText(null);
        binding.tvOrganizerValue.setText(null);
        binding.rowChangePassword.setVisibility(View.GONE);
        if (current == null) return;
        if (current.isGuest() || (boundUid != null && !boundUid.equals(current.getUid()))) {
            finish(); return;
        }
        if (current.getStatus() == com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED) {
            binding.settingsLoading.setVisibility(View.GONE);
            binding.settingsError.setVisibility(View.VISIBLE);
            return;
        }
        if (!current.isAuthenticated() || identity == null || !current.getUid().equals(identity.getUid())) return;
        boundUid = current.getUid();
        binding.settingsLoading.setVisibility(View.GONE);
        bindIdentity(identity);
        bindProfile(current.getUser());
        binding.settingsContent.setVisibility(View.VISIBLE);
    }

    private void bindIdentity(AuthIdentity identity) {
        if (identity == null) return;
        binding.tvSettingsEmail.setText(identity.getEmail() == null ? "" : identity.getEmail());
        int provider = R.string.settings_provider_unknown;
        if (identity.hasMultipleProviders()) provider = R.string.settings_provider_multiple;
        else if (identity.hasPasswordProvider()) provider = R.string.settings_provider_password;
        else if (identity.hasGoogleProvider()) provider = R.string.settings_provider_google;
        binding.tvSettingsProvider.setText(provider);
        binding.rowChangePassword.setVisibility(identity.hasPasswordProvider() ? View.VISIBLE : View.GONE);
        binding.tvSecurityInfo.setText(identity.hasGoogleProvider() && !identity.hasPasswordProvider()
                ? R.string.settings_google_security_info : R.string.settings_account_security_info);
    }

    private void bindProfile(User user) {
        if (user == null) return;
        binding.tvSettingsName.setText(user.getName() == null || user.getName().trim().isEmpty()
                ? getString(R.string.profile_new_user) : user.getName());
        if (binding.tvSettingsEmail.getText().length() == 0 && user.getEmail() != null) {
            binding.tvSettingsEmail.setText(user.getEmail());
        }
        String status = user.getVerificationStatus();
        int label = R.string.settings_organizer_none;
        if (VerificationRequest.STATUS_PENDING.equals(status)) label = R.string.settings_organizer_pending;
        else if (VerificationRequest.STATUS_REJECTED.equals(status)) label = R.string.settings_organizer_rejected;
        else if (VerificationRequest.STATUS_APPROVED.equals(status) && user.isVerified()) label = R.string.settings_organizer_approved;
        binding.tvOrganizerValue.setText(label);
    }

    private void bindLanguage(AppPreferencesRepository.Language value) {
        binding.tvLanguageValue.setText(value == AppPreferencesRepository.Language.ENGLISH
                ? R.string.settings_language_english : R.string.settings_language_bosnian);
    }

    private void bindTheme(AppPreferencesRepository.Theme value) {
        int label = value == AppPreferencesRepository.Theme.LIGHT ? R.string.settings_theme_light
                : value == AppPreferencesRepository.Theme.DARK ? R.string.settings_theme_dark
                : R.string.settings_theme_system;
        binding.tvThemeValue.setText(label);
    }

    private void showLanguageDialog() {
        AppPreferencesRepository.Language current = settings.getLanguage().getValue();
        int checked = current == AppPreferencesRepository.Language.ENGLISH ? 1 : 0;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.settings_language_dialog)
                .setSingleChoiceItems(new String[]{getString(R.string.settings_language_bosnian),
                        getString(R.string.settings_language_english)}, checked, (dialog, which) -> {
                    AppPreferencesRepository.Language selected = which == 1
                            ? AppPreferencesRepository.Language.ENGLISH : AppPreferencesRepository.Language.BOSNIAN;
                    settings.selectLanguage(selected); dialog.dismiss(); AppDisplay.applyLanguage(selected);
                }).setNegativeButton(R.string.action_cancel, null).show();
    }

    private void showThemeDialog() {
        AppPreferencesRepository.Theme current = settings.getTheme().getValue();
        int checked = current == AppPreferencesRepository.Theme.LIGHT ? 1
                : current == AppPreferencesRepository.Theme.DARK ? 2 : 0;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.settings_theme_dialog)
                .setSingleChoiceItems(new String[]{getString(R.string.settings_theme_system),
                        getString(R.string.settings_theme_light), getString(R.string.settings_theme_dark)}, checked,
                        (dialog, which) -> {
                            AppPreferencesRepository.Theme selected = which == 1
                                    ? AppPreferencesRepository.Theme.LIGHT : which == 2
                                    ? AppPreferencesRepository.Theme.DARK : AppPreferencesRepository.Theme.SYSTEM;
                            settings.selectTheme(selected); dialog.dismiss(); AppDisplay.applyTheme(selected);
                        }).setNegativeButton(R.string.action_cancel, null).show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.settings_logout_title)
                .setMessage(R.string.settings_logout_body)
                .setPositiveButton(R.string.auth_logout, (dialog, which) -> { session.logout(); finish(); })
                .setNegativeButton(R.string.action_cancel, null).show();
    }
}
