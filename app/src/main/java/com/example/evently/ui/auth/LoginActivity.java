package com.example.evently.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.R;
import com.example.evently.data.auth.AuthRepositoryImpl;
import com.example.evently.data.user.UserRepositoryImpl;
import com.example.evently.databinding.ActivityLoginBinding;
import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.user.UserRepository;
import com.example.evently.ui.activities.HomeActivity;
import com.example.evently.util.ErrorMapper;
import com.example.evently.util.PendingActionManager;
import com.example.evently.util.AuthenticatedNavigator;
import com.example.evently.data.AppDependencies;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;
    private SessionViewModel sessionViewModel;
    private boolean awaitingProfileReady;
    private boolean navigating;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            authViewModel.loginWithGoogle(account.getIdToken(), account.getDisplayName());
                        } else {
                            showError(getString(R.string.auth_error_generic));
                        }
                    } catch (ApiException e) {
                        showError(getString(R.string.auth_error_generic));
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        AuthRepository authRepository = new AuthRepositoryImpl();
        UserRepository userRepository = new UserRepositoryImpl();
        authViewModel = new ViewModelProvider(this,
                new AuthViewModelFactory(authRepository, userRepository))
                .get(AuthViewModel.class);
        sessionViewModel = new ViewModelProvider(this,
                new SessionViewModelFactory(AppDependencies.sessionRepository())).get(SessionViewModel.class);
        sessionViewModel.start();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnLogin.setOnClickListener(v -> onLoginClicked());
        binding.btnGoogleSignIn.setOnClickListener(v -> onGoogleClicked());
        binding.btnForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
        binding.tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        observeViewModel();
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                setLoadingUi(isLoading);
            }
        });

        authViewModel.getUser().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                awaitingProfileReady = true;
                continueWhenReady();
            }
        });
        sessionViewModel.getState().observe(this, state -> {
            continueWhenReady();
        });

        authViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
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


    private void onLoginClicked() {
        binding.tvLoginError.setVisibility(View.GONE);
        String email = textOf(binding.etEmail);
        String password = textOf(binding.etPassword);

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError(getString(R.string.auth_error_empty_fields));
            binding.etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError(getString(R.string.auth_error_invalid_email));
            binding.etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError(getString(R.string.auth_error_empty_fields));
            binding.etPassword.requestFocus();
            return;
        }
        authViewModel.login(email, password);
    }

    private void onGoogleClicked() {
        binding.tvLoginError.setVisibility(View.GONE);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void setLoadingUi(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
        binding.etEmail.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
    }

    private void showError(String message) {
        setLoadingUi(false);
        showError(ErrorMapper.toResId(message));
    }

    private void showError(@StringRes int message) {
        binding.tvLoginError.setText(message);
        binding.tvLoginError.setVisibility(View.VISIBLE);
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private String textOf(android.widget.EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
