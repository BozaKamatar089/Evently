package com.example.evently.data;

import android.content.Context;

import com.example.evently.data.auth.AuthRepositoryImpl;
import com.example.evently.data.event.EventRepositoryImpl;
import com.example.evently.data.registration.RegistrationRepositoryImpl;
import com.example.evently.data.session.SessionRepositoryImpl;
import com.example.evently.data.user.UserRepositoryImpl;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.registration.RegistrationRepository;
import com.example.evently.domain.session.SessionRepository;
import com.example.evently.domain.auth.AuthRepository;
import com.example.evently.domain.preferences.AppPreferencesRepository;
import com.example.evently.data.preferences.SharedPreferencesAppPreferences;

/** Small composition root; avoids a DI framework while sharing the app session source. */
public final class AppDependencies {
    private static SessionRepository sessionRepository;
    private static AuthRepository authRepository;
    private static UserRepositoryImpl userRepository;
    private static AppPreferencesRepository appPreferences;
    private static EventRepository eventRepository;
    private static RegistrationRepository registrationRepository;
    private AppDependencies() { }
    public static synchronized void initialize(Context context) {
        if (appPreferences == null) appPreferences = new SharedPreferencesAppPreferences(context);
    }
    public static synchronized AuthRepository authRepository() {
        if (authRepository == null) authRepository = new AuthRepositoryImpl();
        return authRepository;
    }
    public static synchronized UserRepositoryImpl userRepository() {
        if (userRepository == null) userRepository = new UserRepositoryImpl();
        return userRepository;
    }
    public static synchronized AppPreferencesRepository appPreferences() {
        if (appPreferences == null) throw new IllegalStateException("AppDependencies not initialized");
        return appPreferences;
    }
    public static synchronized SessionRepository sessionRepository() {
        if (sessionRepository == null) {
            sessionRepository = new SessionRepositoryImpl(authRepository(), userRepository());
        }
        return sessionRepository;
    }

    public static synchronized EventRepository eventRepository() {
        if (eventRepository == null) eventRepository = new EventRepositoryImpl();
        return eventRepository;
    }

    public static synchronized RegistrationRepository registrationRepository() {
        if (registrationRepository == null) registrationRepository = new RegistrationRepositoryImpl();
        return registrationRepository;
    }
}
