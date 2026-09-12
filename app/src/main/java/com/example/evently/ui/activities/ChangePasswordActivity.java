package com.example.evently.ui.activities;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.evently.R;
import com.example.evently.data.AppDependencies;
import com.example.evently.databinding.ActivityChangePasswordBinding;
import com.example.evently.domain.auth.AccountSecurityPolicy;
import com.example.evently.domain.auth.AuthIdentity;
import com.example.evently.util.ErrorMapper;
import com.example.evently.viewmodel.AccountSecurityViewModel;
import com.example.evently.viewmodel.AccountSecurityViewModelFactory;

/** Password change for accounts that actually have a password provider. */
public final class ChangePasswordActivity extends AppCompatActivity {
    private ActivityChangePasswordBinding binding;
    private AccountSecurityViewModel model;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        model = new ViewModelProvider(this, new AccountSecurityViewModelFactory(AppDependencies.authRepository()))
                .get(AccountSecurityViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        AuthIdentity identity = model.getIdentity();
        if (identity == null || !identity.hasPasswordProvider()) { finish(); return; }
        binding.btnChangePassword.setOnClickListener(v -> submit());
        model.getStatus().observe(this, this::render);
        model.getError().observe(this, ignored -> render(model.getStatus().getValue()));
    }
    private void submit() {
        clearErrors();
        AccountSecurityPolicy.PasswordResult result = model.changePassword(text(binding.etCurrentPassword),
                text(binding.etNewPassword), text(binding.etConfirmPassword));
        if (result == AccountSecurityPolicy.PasswordResult.CURRENT_REQUIRED)
            binding.currentPasswordLayout.setError(getString(R.string.password_current_required));
        else if (result == AccountSecurityPolicy.PasswordResult.NEW_TOO_SHORT)
            binding.newPasswordLayout.setError(getString(R.string.auth_error_password_short));
        else if (result == AccountSecurityPolicy.PasswordResult.CONFIRMATION_MISMATCH)
            binding.confirmPasswordLayout.setError(getString(R.string.auth_error_password_mismatch));
        else if (result == AccountSecurityPolicy.PasswordResult.UNCHANGED)
            binding.newPasswordLayout.setError(getString(R.string.password_unchanged));
    }
    private void render(AccountSecurityViewModel.Status status) {
        if (status == null) return;
        boolean loading = status == AccountSecurityViewModel.Status.LOADING;
        boolean editable = !loading && status != AccountSecurityViewModel.Status.SUCCESS;
        binding.progressPassword.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnChangePassword.setEnabled(editable);
        binding.etCurrentPassword.setEnabled(editable); binding.etNewPassword.setEnabled(editable);
        binding.etConfirmPassword.setEnabled(editable);
        if (status == AccountSecurityViewModel.Status.SUCCESS) {
            binding.tvPasswordStatus.setText(R.string.password_changed);
            binding.tvPasswordStatus.setTextColor(getColor(R.color.colorSuccess));
            binding.tvPasswordStatus.setVisibility(View.VISIBLE);
        } else if (status == AccountSecurityViewModel.Status.ERROR) {
            binding.tvPasswordStatus.setText(ErrorMapper.toResId(model.getError().getValue()));
            binding.tvPasswordStatus.setTextColor(getColor(R.color.colorError));
            binding.tvPasswordStatus.setVisibility(View.VISIBLE);
        }
    }
    private void clearErrors() {
        binding.currentPasswordLayout.setError(null); binding.newPasswordLayout.setError(null);
        binding.confirmPasswordLayout.setError(null); binding.tvPasswordStatus.setVisibility(View.GONE);
    }
    private static String text(android.widget.EditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }
}
