package com.example.evently.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.R;
import com.example.evently.data.auth.AuthRepositoryImpl;
import com.example.evently.data.user.UserRepositoryImpl;
import com.example.evently.databinding.ActivityRegisterBinding;
import com.example.evently.ui.activities.HomeActivity;
import com.example.evently.util.PendingActionManager;
import com.example.evently.util.AuthenticatedNavigator;
import com.example.evently.data.AppDependencies;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;
    private SessionViewModel sessionViewModel;
    private boolean awaitingProfileReady;
    private boolean navigating;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        AuthRepositoryImpl authRepository = new AuthRepositoryImpl();
        UserRepositoryImpl userRepository = new UserRepositoryImpl();
        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(authRepository, userRepository))
                .get(AuthViewModel.class);
        sessionViewModel = new ViewModelProvider(this,
                new SessionViewModelFactory(AppDependencies.sessionRepository())).get(SessionViewModel.class);
        sessionViewModel.start();

        binding.btnRegister.setOnClickListener(v -> onRegisterClicked());
        binding.tvGoToLogin.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.progressBarReg.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.btnRegister.setEnabled(!isLoading);
            }
        });

        authViewModel.getUser().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                Toast.makeText(this, R.string.auth_registration_success, Toast.LENGTH_SHORT).show();
                awaitingProfileReady = true;
                continueWhenReady();
            }
        });
        sessionViewModel.getState().observe(this, state -> {
            continueWhenReady();
        });

        authViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                binding.progressBarReg.setVisibility(View.GONE);
                binding.btnRegister.setEnabled(true);
                showError(com.example.evently.util.ErrorMapper.toResId(error));
            }
        });
    }

    private void continueWhenReady() {
        com.example.evently.domain.session.SessionState state = sessionViewModel.getState().getValue();
        if (!navigating && awaitingProfileReady && state != null && state.isAuthenticated()
                && authViewModel.getUser().getValue() != null
                && state.getUid().equals(authViewModel.getUser().getValue().getUid())) {
            navigating = true;
            awaitingProfileReady = false;
            AuthenticatedNavigator.continueAfterProfileReady(this);
        }
    }


    private void onRegisterClicked() {
        binding.tvRegisterError.setVisibility(View.GONE);
        String firstName = textOf(binding.etFirstName);
        String lastName = textOf(binding.etLastName);
        String email = textOf(binding.etEmailReg);
        String password = textOf(binding.etPasswordReg);
        String confirm = textOf(binding.etConfirmPassword);

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)
                || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError(R.string.auth_error_empty_fields);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmailReg.setError(getString(R.string.auth_error_invalid_email));
            binding.etEmailReg.requestFocus();
            return;
        }
        if (password.length() < 6) {
            binding.etPasswordReg.setError(getString(R.string.auth_error_password_short));
            return;
        }
        if (!password.equals(confirm)) {
            binding.etConfirmPassword.setError(getString(R.string.auth_error_password_mismatch));
            return;
        }
        authViewModel.register(firstName, lastName, email, password);
    }

    private void showError(@StringRes int message) {
        binding.tvRegisterError.setText(message);
        binding.tvRegisterError.setVisibility(View.VISIBLE);
    }

    private String textOf(android.widget.EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
