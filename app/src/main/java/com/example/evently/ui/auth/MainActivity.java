package com.example.evently.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputEditText;
import com.example.evently.R;
import com.example.evently.data.auth.AuthRepositoryImpl;
import com.example.evently.domain.auth.AuthRepository;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etEmail, etPassword;
    private ProgressBar progressBar;
    private CredentialManager credentialManager;

    private final String WEB_CLIENT_ID = "1077997056530-uk0ur3p35l417b6vrqsn098hcfv88k1n.apps.googleusercontent.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextView tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBar = findViewById(R.id.progressBar);

        credentialManager = CredentialManager.create(this);

        AuthRepository authRepository = new AuthRepositoryImpl();

        authViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            public <T extends ViewModel> T create(Class<T> modelClass) {
                if (modelClass.isAssignableFrom(AuthViewModel.class)) {
                    return (T) new AuthViewModel(authRepository);
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        }).get(AuthViewModel.class);

        authViewModel.logout();

        authViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                btnLogin.setEnabled(!isLoading);
                btnGoogleSignIn.setEnabled(!isLoading);
            }
        });

        authViewModel.getUser().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                Toast.makeText(this, "Prijava uspješna!", Toast.LENGTH_SHORT).show();
                goToHome();
            }
        });

        authViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(MainActivity.this, "Greška: " + error, Toast.LENGTH_LONG).show();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Unesite email i lozinku", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.login(email, password);
        });

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();

        credentialManager.getCredentialAsync(this, request, null, executor,
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            CustomCredential credential = (CustomCredential) result.getCredential();
                            if (credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                                String idToken = googleIdTokenCredential.getIdToken();

                                runOnUiThread(() -> authViewModel.loginWithGoogle(idToken));
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Greška pri obradi: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }

                    @Override
                    public void onError(androidx.credentials.exceptions.GetCredentialException e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void goToHome() {
        Intent intent = new Intent(MainActivity.this, com.example.evently.home.HomeActivity.class);
        startActivity(intent);
        finish();
    }
}