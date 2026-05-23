package com.example.evently.baza;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveProfile(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE userId = :uid")
    UserProfile getProfile(String uid);
}