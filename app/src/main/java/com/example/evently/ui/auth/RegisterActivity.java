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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.example.evently.R;
import com.example.evently.data.auth.AuthRepositoryImpl;
import com.example.evently.domain.auth.AuthRepository;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etFirstName, etLastName, etPhone, etEmail, etPassword, etConfirmPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmailReg);
        etPassword = findViewById(R.id.etPasswordReg);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);
        progressBar = findViewById(R.id.progressBarReg);

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

        authViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                btnRegister.setEnabled(!isLoading);
            }
        });

        authViewModel.getUser().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                // Dodajemo ime i prezime u Firebase profil korisnika
                String fullName = etFirstName.getText().toString().trim() + " " + etLastName.getText().toString().trim();
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build();

                firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    Toast.makeText(this, "Registracija uspješna!", Toast.LENGTH_SHORT).show();
                    goToHome();
                });
            }
        });

        authViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Greška: " + error, Toast.LENGTH_LONG).show();
            }
        });

        btnRegister.setOnClickListener(v -> {
            String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
            String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "Molimo popunite sva obavezna polja", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Lozinka mora imati barem 6 karaktera", Toast.LENGTH_SHORT).show();
                return;
            }

            // NAPOMENA: Broj telefona ovdje samo čitamo, a kasnije ćemo ga (kao i sve ostale
            // podatke korisnika) čuvati u Room ili Firestore bazi kada je budemo pravili.

            progressBar.setVisibility(View.VISIBLE);
            authViewModel.register(email, password);
        });

        tvGoToLogin.setOnClickListener(v -> {
            finish(); // Zatvara RegisterActivity i vraća nas na Login ekran
        });
    }

    private void goToHome() {
        // Za sada samo Toast, kasnije otvara HomeActivity
        Toast.makeText(this, "Otvaram Home ekran...", Toast.LENGTH_SHORT).show();
        finish();
    }
}