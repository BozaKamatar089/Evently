package com.example.evently.baza;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    @NonNull
    public String userId;

    public String dateOfBirth;
    public String imageUri;
    public String role;

    public UserProfile(@NonNull String userId) {
        this.userId = userId;
    }
}