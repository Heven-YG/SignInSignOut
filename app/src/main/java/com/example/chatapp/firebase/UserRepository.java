package com.example.chatapp.firebase;

import androidx.annotation.NonNull;

import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.TaskUtils;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Reads and writes user profile documents at {@code users/{uid}}. Passwords are never part
 * of the profile.
 */
public final class UserRepository {

    /** Prevents instantiation of this utility class. */
    private UserRepository() {
    }

    /**
     * Returns the document reference for a user's profile.
     *
     * @param userId Firebase Auth UID
     * @return reference to {@code users/{userId}}
     */
    @NonNull
    static DocumentReference userDocument(@NonNull String userId) {
        return FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(userId);
    }

    /**
     * Loads a user's profile.
     *
     * @param userId Firebase Auth UID
     * @return a task with the profile, or {@code null} if no profile document exists; the
     *         task fails on network, permission, or timeout errors
     */
    @NonNull
    public static Task<UserProfile> loadProfile(@NonNull String userId) {
        return TaskUtils.withTimeout(userDocument(userId).get(),
                        Constants.FIRESTORE_TIMEOUT_MILLIS)
                .continueWith(task -> UserProfile.fromSnapshot(task.getResult()));
    }

    /**
     * Creates or replaces a user's profile with a server-generated {@code createdAt}.
     *
     * @param userId Firebase Auth UID, used as the document ID
     * @param name   display name
     * @param email  email address
     * @param image  Base64 avatar, or an empty string for the default avatar
     * @return a task that succeeds once the server confirms the write, or fails on error or
     *         timeout
     */
    @NonNull
    public static Task<Void> saveProfile(@NonNull String userId, @NonNull String name,
                                         @NonNull String email, @NonNull String image) {
        Map<String, Object> profile = new HashMap<>();
        profile.put(Constants.KEY_NAME, name);
        profile.put(Constants.KEY_EMAIL, email);
        profile.put(Constants.KEY_IMAGE, image);
        profile.put(Constants.KEY_CREATED_AT, FieldValue.serverTimestamp());
        return TaskUtils.withTimeout(userDocument(userId).set(profile),
                Constants.FIRESTORE_TIMEOUT_MILLIS);
    }
}
