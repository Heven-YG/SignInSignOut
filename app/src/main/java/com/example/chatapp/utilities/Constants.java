package com.example.chatapp.utilities;

/**
 * Shared constant values for Firestore paths and fields, preference keys, intent extras,
 * and notification settings.
 *
 * <p>Keeping these names in one place makes sure every screen reads and writes the same
 * Firestore fields and preference keys.</p>
 */
public final class Constants {

    /** Firestore collection that holds one profile document per Firebase Auth UID. */
    public static final String KEY_COLLECTION_USERS = "users";

    /** Private subcollection under a user document that holds FCM registrations. */
    public static final String KEY_COLLECTION_DEVICES = "devices";

    /** Profile field holding the user's display name. */
    public static final String KEY_NAME = "name";

    /** Profile field holding the user's email address. */
    public static final String KEY_EMAIL = "email";

    /**
     * Profile field holding a Base64-encoded JPEG avatar. An empty value means the default
     * avatar should be shown.
     */
    public static final String KEY_IMAGE = "image";

    /** Profile field holding the server timestamp of profile creation. */
    public static final String KEY_CREATED_AT = "createdAt";

    /** Device field holding the FCM registration token. */
    public static final String KEY_FCM_TOKEN = "fcmToken";

    /** Device field identifying the client platform. */
    public static final String KEY_PLATFORM = "platform";

    /** Device field holding the server timestamp of the last token update. */
    public static final String KEY_UPDATED_AT = "updatedAt";

    /** Value stored in {@link #KEY_PLATFORM} for this app. */
    public static final String PLATFORM_ANDROID = "android";

    /** Name of the private preference file used for the cached session and profile. */
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";

    /**
     * Name of the private preference file that holds the installation ID. It is separate so
     * clearing the session cache on sign-out keeps the installation ID stable.
     */
    public static final String KEY_INSTALLATION_PREFERENCE_NAME = "chatAppInstallation";

    /** Preference key for the lecture-style "is signed in" flag. */
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";

    /** Preference key for the UID the cached profile belongs to. */
    public static final String KEY_USER_ID = "userId";

    /** Preference key for the randomly generated installation ID. */
    public static final String KEY_INSTALLATION_ID = "installationId";

    /** Intent extra telling the sign-up screen to only complete a missing profile. */
    public static final String EXTRA_COMPLETE_PROFILE = "completeProfile";

    /** Notification channel ID used for FCM notifications. */
    public static final String NOTIFICATION_CHANNEL_ID = "chat_messages";

    /** Data payload key that may carry a notification title for data-only messages. */
    public static final String KEY_MESSAGE_TITLE = "title";

    /** Data payload key that may carry a notification body for data-only messages. */
    public static final String KEY_MESSAGE_BODY = "body";

    /** Firebase Authentication's default minimum password length. */
    public static final int MIN_PASSWORD_LENGTH = 6;

    /** Maximum width or height, in pixels, of the stored profile image. */
    public static final int PROFILE_IMAGE_MAX_DIMENSION = 256;

    /** Upper limit for the Base64 profile image string, well below Firestore's 1 MiB limit. */
    public static final int PROFILE_IMAGE_MAX_BASE64_LENGTH = 80 * 1024;

    /** Timeout for Firestore network operations so the UI never waits indefinitely. */
    public static final long FIRESTORE_TIMEOUT_MILLIS = 15_000L;

    /** Shorter timeout for best-effort cleanup during sign-out. */
    public static final long SIGN_OUT_CLEANUP_TIMEOUT_MILLIS = 5_000L;

    /** Prevents instantiation of this constants holder. */
    private Constants() {
    }
}
