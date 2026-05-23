package com.example.evently.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.evently.R;
import com.example.evently.baza.AppDatabase;
import com.example.evently.baza.UserDao;
import com.example.evently.baza.UserProfile;
import com.example.evently.ui.auth.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private UserDao userDao;
    private UserProfile localProfile;

    private TextView tvUserName, tvUserEmail, tvUserPhone, tvDob;
    private CircleImageView ivProfileImage;
    private MaterialButton btnVerifyPhone;
    private String mVerificationId;


    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {

                    Glide.with(this).load(uri).into(ivProfileImage);


                    if (localProfile != null) {
                        localProfile.imageUri = uri.toString();
                        userDao.saveProfile(localProfile);
                        Toast.makeText(getContext(), "Slika profila spašena!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();


        userDao = AppDatabase.getInstance(getContext()).userDao();


        if (currentUser != null) {
            localProfile = userDao.getProfile(currentUser.getUid());
            if (localProfile == null) {
                localProfile = new UserProfile(currentUser.getUid());
            }
        }

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        tvDob = view.findViewById(R.id.tvDob);
        ivProfileImage = view.findViewById(R.id.ivProfileImage);

        MaterialButton btnChangePassword = view.findViewById(R.id.btnChangePassword);
        MaterialButton btnChangeEmail = view.findViewById(R.id.btnChangeEmail);
        btnVerifyPhone = view.findViewById(R.id.btnVerifyPhone);
        MaterialButton btnSetDob = view.findViewById(R.id.btnSetDob);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        // --- Prikaz podataka (Firebase + Room) ---
        if (currentUser != null) {
            if (currentUser.getEmail() != null) tvUserEmail.setText(currentUser.getEmail());
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                tvUserName.setText(currentUser.getDisplayName());
            } else {
                tvUserName.setText("Novi Korisnik");
            }

            if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                tvUserPhone.setText("Telefon: " + currentUser.getPhoneNumber());
                btnVerifyPhone.setVisibility(View.GONE);
            }

            // UČITAVANJE IZ ROOM BAZE (Slika i Datum)
            if (localProfile.dateOfBirth != null) {
                tvDob.setText("Datum rođenja: " + localProfile.dateOfBirth);
            }
            if (localProfile.imageUri != null) {
                Glide.with(this).load(Uri.parse(localProfile.imageUri)).into(ivProfileImage);
            }
        }


        ivProfileImage.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // --- DATUM ROĐENJA ---
        btnSetDob.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Odaberite datum rođenja")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                String datum = sdf.format(new Date(selection));
                tvDob.setText("Datum rođenja: " + datum);


                if (localProfile != null) {
                    localProfile.dateOfBirth = datum;
                    userDao.saveProfile(localProfile);
                    Toast.makeText(getContext(), "Datum uspješno spašen!", Toast.LENGTH_SHORT).show();
                }
            });

            datePicker.show(getChildFragmentManager(), "DOB_PICKER");
        });

        btnChangeEmail.setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            EditText etPassword = new EditText(getContext());
            etPassword.setHint("Unesite trenutnu lozinku");
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.addView(etPassword);

            EditText etNewEmail = new EditText(getContext());
            etNewEmail.setHint("Unesite novi email");
            etNewEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            layout.addView(etNewEmail);

            new AlertDialog.Builder(getContext())
                    .setTitle("Sigurna promjena emaila")
                    .setView(layout)
                    .setPositiveButton("Promijeni", (dialog, which) -> {
                        String password = etPassword.getText().toString().trim();
                        String newEmail = etNewEmail.getText().toString().trim();

                        if (!password.isEmpty() && !newEmail.isEmpty() && currentUser != null && currentUser.getEmail() != null) {
                            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
                            currentUser.reauthenticate(credential).addOnSuccessListener(aVoid -> {
                                currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Link poslan na novi email!", Toast.LENGTH_LONG).show();
                                        mAuth.signOut();
                                        if (getActivity() != null) {
                                            startActivity(new Intent(getActivity(), MainActivity.class));
                                            getActivity().finish();
                                        }
                                    }
                                });
                            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Pogrešna lozinka!", Toast.LENGTH_SHORT).show());
                        }
                    }).setNegativeButton("Odustani", null).show();
        });

        btnChangePassword.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getEmail() != null) {
                mAuth.sendPasswordResetEmail(currentUser.getEmail()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) Toast.makeText(getContext(), "Link poslan na email!", Toast.LENGTH_SHORT).show();
                });
            }
        });

        btnVerifyPhone.setOnClickListener(v -> {
            EditText phoneInput = new EditText(getContext());
            phoneInput.setHint("npr. +38761234567");
            phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
            phoneInput.setPadding(40, 40, 40, 40);

            new AlertDialog.Builder(getContext())
                    .setTitle("Potvrda telefona")
                    .setView(phoneInput)
                    .setPositiveButton("Pošalji SMS", (dialog, which) -> {
                        String phoneNumber = phoneInput.getText().toString().trim();
                        if (!phoneNumber.isEmpty()) sendSmsCode(phoneNumber);
                    }).setNegativeButton("Odustani", null).show();
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                getActivity().finish();
            }
        });
    }

    private void sendSmsCode(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(getActivity())
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        linkPhoneCredential(credential);
                    }
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        showCodeDialog();
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showCodeDialog() {
        EditText codeInput = new EditText(getContext());
        codeInput.setHint("Unesite 6-cifreni kod");
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setPadding(40, 40, 40, 40);

        new AlertDialog.Builder(getContext())
                .setTitle("Unos koda")
                .setView(codeInput)
                .setPositiveButton("Potvrdi", (dialog, which) -> {
                    String code = codeInput.getText().toString().trim();
                    if (!code.isEmpty() && mVerificationId != null) {
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
                        linkPhoneCredential(credential);
                    }
                }).setNegativeButton("Odustani", null).show();
    }

    private void linkPhoneCredential(PhoneAuthCredential credential) {
        if (currentUser != null) {
            currentUser.linkWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    tvUserPhone.setText("Telefon: " + currentUser.getPhoneNumber());
                    btnVerifyPhone.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Broj uspješno povezan!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Greška pri povezivanju.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}