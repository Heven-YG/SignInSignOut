package com.example.chatapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * Small wrapper around private {@link SharedPreferences} used for the cached profile, the
 * saved sign-in flag, and the stable installation ID.
 *
 * <p>The cached values are only a convenience for showing the profile quickly. Firebase
 * Authentication remains the source of truth: callers must confirm there is a signed-in
 * Firebase user, and that the cache belongs to that user, before trusting anything here.
 * Passwords are never stored.</p>
 */
public class PreferenceManager {

    private final SharedPreferences sessionPreferences;
    private final SharedPreferences installationPreferences;

    /**
     * Creates a manager backed by the app's private preference files.
     *
     * @param context any context; the application context is used internally
     */
    public PreferenceManager(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        sessionPreferences = appContext.getSharedPreferences(
                Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
        installationPreferences = appContext.getSharedPreferences(
                Constants.KEY_INSTALLATION_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Stores a boolean session value.
     *
     * @param key   preference key
     * @param value value to store
     */
    public void putBoolean(@NonNull String key, boolean value) {
        sessionPreferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Reads a boolean session value.
     *
     * @param key preference key
     * @return the stored value, or {@code false} when missing
     */
    public boolean getBoolean(@NonNull String key) {
        return sessionPreferences.getBoolean(key, false);
    }

    /**
     * Stores a string session value.
     *
     * @param key   preference key
     * @param value value to store; {@code null} removes the key
     */
    public void putString(@NonNull String key, @Nullable String value) {
        sessionPreferences.edit().putString(key, value).apply();
    }

    /**
     * Reads a string session value.
     *
     * @param key preference key
     * @return the stored value, or {@code null} when missing
     */
    @Nullable
    public String getString(@NonNull String key) {
        return sessionPreferences.getString(key, null);
    }

    /**
     * Caches the signed-in user's profile and sets the saved sign-in flag in one write.
     *
     * @param userId Firebase Auth UID the profile belongs to
     * @param name   display name
     * @param email  email address
     * @param image  Base64 avatar, or an empty string for the default avatar
     */
    public void saveUserProfile(@NonNull String userId, @Nullable String name,
                                @Nullable String email, @Nullable String image) {
        sessionPreferences.edit()
                .putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                .putString(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_NAME, name)
                .putString(Constants.KEY_EMAIL, email)
                .putString(Constants.KEY_IMAGE, image)
                .apply();
    }

    /**
     * Reports whether the cached profile belongs to the given Firebase user.
     *
     * @param currentUserId UID of the currently signed-in Firebase user, or {@code null}
     * @return {@code true} only if the cache was written for exactly this user
     */
    public boolean hasCachedProfileFor(@Nullable String currentUserId) {
        return SessionUtils.isCachedProfileValid(
                getBoolean(Constants.KEY_IS_SIGNED_IN),
                getString(Constants.KEY_USER_ID),
                currentUserId);
    }

    /**
     * Removes the cached profile and the saved sign-in flag. The installation ID is kept.
     */
    public void clear() {
        sessionPreferences.edit().clear().apply();
    }

    /**
     * Returns this installation's stable random ID, creating and saving it on first use.
     *
     * @return a random UUID string that stays the same until app data is cleared
     */
    @NonNull
    public synchronized String getInstallationId() {
        String id = installationPreferences.getString(Constants.KEY_INSTALLATION_ID, null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            installationPreferences.edit().putString(Constants.KEY_INSTALLATION_ID, id).apply();
        }
        return id;
    }
}
