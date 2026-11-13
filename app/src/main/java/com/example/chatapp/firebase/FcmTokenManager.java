package com.example.chatapp.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.example.chatapp.utilities.SessionUtils;
import com.example.chatapp.utilities.TaskUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * Associates this installation's FCM registration token with the signed-in user at
 * {@code users/{uid}/devices/{installationId}}.
 *
 * <p>Every write first checks that the account captured when the work started is still the
 * signed-in account, so a late token callback never writes into another user's data. No
 * server credentials are used; the app only stores its own token.</p>
 */
public final class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";

    /** Prevents instantiation of this utility class. */
    private FcmTokenManager() {
    }

    /**
     * Fetches the current FCM token and stores it for the signed-in user. Call after a
     * successful sign-in or session restore; this also covers tokens generated before the
     * user signed in. Does nothing when no user is signed in.
     *
     * @param context any context
     */
    public static void registerCurrentToken(@NonNull Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        final String expectedUserId = user.getUid();
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> saveToken(appContext, expectedUserId, token))
                .addOnFailureListener(e -> Log.w(TAG, "Unable to fetch FCM token", e));
    }

    /**
     * Handles a token refresh reported by {@code FirebaseMessagingService#onNewToken}. The
     * token is stored only if a user is signed in; otherwise it is picked up by
     * {@link #registerCurrentToken(Context)} after the next sign-in.
     *
     * @param context any context
     * @param token   the new registration token
     */
    public static void onTokenRefreshed(@NonNull Context context, @NonNull String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            saveToken(context.getApplicationContext(), user.getUid(), token);
        }
    }

    /**
     * Removes this installation's device document for the signed-in user. Must be called
     * before signing out, while the user is still authenticated for the security rules.
     *
     * @param context any context
     * @return a task that completes when the delete finishes, fails, or times out; it
     *         completes immediately if no user is signed in
     */
    @NonNull
    public static Task<Void> unregisterCurrentDevice(@NonNull Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Tasks.forResult(null);
        }
        String installationId = new PreferenceManager(context).getInstallationId();
        return TaskUtils.withTimeout(deviceDocument(user.getUid(), installationId).delete(),
                Constants.SIGN_OUT_CLEANUP_TIMEOUT_MILLIS);
    }

    /**
     * Writes the token for {@code expectedUserId} if that user is still signed in.
     *
     * @param context        application context
     * @param expectedUserId UID captured when the token request started
     * @param token          registration token to store; ignored if empty
     */
    private static void saveToken(@NonNull Context context, @NonNull String expectedUserId,
                                  @Nullable String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = current == null ? null : current.getUid();
        if (!SessionUtils.isSameSignedInUser(expectedUserId, currentUserId)) {
            return;
        }
        String installationId = new PreferenceManager(context).getInstallationId();
        Map<String, Object> device = new HashMap<>();
        device.put(Constants.KEY_FCM_TOKEN, token);
        device.put(Constants.KEY_PLATFORM, Constants.PLATFORM_ANDROID);
        device.put(Constants.KEY_UPDATED_AT, FieldValue.serverTimestamp());
        deviceDocument(expectedUserId, installationId).set(device)
                .addOnFailureListener(e -> Log.w(TAG, "Unable to store FCM token", e));
    }

    /**
     * Returns the device document for a user and installation.
     *
     * @param userId         Firebase Auth UID
     * @param installationId this installation's stable random ID
     * @return reference to {@code users/{userId}/devices/{installationId}}
     */
    @NonNull
    private static DocumentReference deviceDocument(@NonNull String userId,
                                                    @NonNull String installationId) {
        return UserRepository.userDocument(userId)
                .collection(Constants.KEY_COLLECTION_DEVICES)
                .document(installationId);
    }
}
