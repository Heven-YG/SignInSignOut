package com.example.chatapp.firebase;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Performs the full sign-out sequence shared by every screen that offers it.
 */
public final class SessionManager {

    /** Prevents instantiation of this utility class. */
    private SessionManager() {
    }

    /**
     * Removes this installation's FCM token association (best effort, while still
     * authenticated), then signs out of Firebase and clears the cached profile and saved
     * sign-in flag. {@code onComplete} runs on the main thread whether or not the token
     * cleanup succeeded.
     *
     * @param context    any context
     * @param onComplete callback run after the user has been signed out
     */
    public static void signOut(@NonNull Context context, @NonNull Runnable onComplete) {
        Context appContext = context.getApplicationContext();
        FcmTokenManager.unregisterCurrentDevice(appContext).addOnCompleteListener(task -> {
            FirebaseAuth.getInstance().signOut();
            new PreferenceManager(appContext).clear();
            onComplete.run();
        });
    }
}
