package com.example.chatapp.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chatapp.utilities.Constants;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Immutable view of a user's profile document stored at {@code users/{uid}}.
 */
public final class UserProfile {

    private final String userId;
    private final String name;
    private final String email;
    private final String image;

    /**
     * Creates a profile value.
     *
     * @param userId Firebase Auth UID
     * @param name   display name, never {@code null}
     * @param email  email address, never {@code null}
     * @param image  Base64 avatar, or an empty string for the default avatar
     */
    public UserProfile(@NonNull String userId, @NonNull String name, @NonNull String email,
                       @NonNull String image) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.image = image;
    }

    /**
     * Builds a profile from a Firestore document, replacing missing fields with empty strings.
     *
     * @param snapshot document read from {@code users/{uid}}
     * @return the profile, or {@code null} if the document does not exist
     */
    @Nullable
    public static UserProfile fromSnapshot(@NonNull DocumentSnapshot snapshot) {
        if (!snapshot.exists()) {
            return null;
        }
        return new UserProfile(
                snapshot.getId(),
                valueOrEmpty(snapshot.getString(Constants.KEY_NAME)),
                valueOrEmpty(snapshot.getString(Constants.KEY_EMAIL)),
                valueOrEmpty(snapshot.getString(Constants.KEY_IMAGE)));
    }

    /**
     * Replaces {@code null} with an empty string.
     *
     * @param value possibly-null text
     * @return the text, or an empty string
     */
    @NonNull
    private static String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    /**
     * Returns the Firebase Auth UID this profile belongs to.
     *
     * @return the user ID
     */
    @NonNull
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the display name.
     *
     * @return the name, possibly empty
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the email address saved in the profile.
     *
     * @return the email, possibly empty
     */
    @NonNull
    public String getEmail() {
        return email;
    }

    /**
     * Returns the Base64 avatar.
     *
     * @return the encoded image, or an empty string when the default avatar is used
     */
    @NonNull
    public String getImage() {
        return image;
    }
}
