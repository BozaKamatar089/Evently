package com.example.evently.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.R;
import com.example.evently.data.auth.AuthRepositoryImpl;
import com.example.evently.data.user.UserRepositoryImpl;
import com.example.evently.databinding.ActivityResetPasswordBinding;
import com.example.evently.util.ErrorMapper;

public class ResetPasswordActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        AuthRepositoryImpl authRepository = new AuthRepositoryImpl();
        UserRepositoryImpl userRepository = new UserRepositoryImpl();
        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(authRepository, userRepository))
                .get(AuthViewModel.class);

        binding.btnResetSend.setOnClickListener(v -> onSendClicked());
        observeViewModel();
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.progressBarReset.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.btnResetSend.setEnabled(!isLoading);
            }
        });

        authViewModel.getResetSent().observe(this, aVoid -> {
            binding.tvResetStatus.setText(R.string.auth_reset_sent);
            binding.tvResetStatus.setTextColor(getColor(R.color.colorSuccess));
            binding.tvResetStatus.setVisibility(View.VISIBLE);
        });

        authViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.progressBarReset.setVisibility(View.GONE);
                binding.btnResetSend.setEnabled(true);
                binding.tvResetStatus.setText(ErrorMapper.toResId(error));
                binding.tvResetStatus.setTextColor(getColor(R.color.colorError));
                binding.tvResetStatus.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onSendClicked() {
        binding.tvResetStatus.setVisibility(View.GONE);
        String email = binding.etResetEmail.getText() != null
                ? binding.etResetEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etResetEmail.setError(getString(R.string.auth_error_invalid_email));
            return;
        }
        authViewModel.resetPassword(email);
    }
}
