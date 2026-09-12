package com.example.evently;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;
import com.example.evently.ui.views.ActivityInsets;
import com.example.evently.data.AppDependencies;
import com.example.evently.ui.preferences.AppDisplay;

import com.cloudinary.android.MediaManager;
import com.example.evently.BuildConfig;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * Application klasa. Inicijalizira Cloudinary MediaManager sa konfiguracijom
 * iz {@code local.properties} (cloud name + unsigned upload preset).
 * Omogućava Firestore offline persistence.
 */
public class EventlyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppDependencies.initialize(this);
        AppDisplay.applyTheme(AppDependencies.appPreferences().getTheme());
        AppDisplay.applyLanguage(AppDependencies.appPreferences().getLanguage());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity activity) { ActivityInsets.apply(activity); }
            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });

        // Enable Firestore offline persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        if (!BuildConfig.CLOUDINARY_CLOUD_NAME.isEmpty()) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("secure", true);
            MediaManager.init(this, config);
        }
    }
}
