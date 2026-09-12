package com.example.evently.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.evently.domain.admin.AdminRepository;

public class AdminViewModelFactory implements ViewModelProvider.Factory {

    private final AdminRepository adminRepository;

    public AdminViewModelFactory(@NonNull AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AdminViewModel.class)) {
            return (T) new AdminViewModel(adminRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
