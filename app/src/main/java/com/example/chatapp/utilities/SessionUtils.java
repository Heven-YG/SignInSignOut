package com.example.chatapp.utilities;

import androidx.annotation.Nullable;

/**
 * Pure session rules with no Android or Firebase dependencies, so they can be unit tested.
 */
public final class SessionUtils {

    /** Prevents instantiation of this utility class. */
    private SessionUtils() {
    }

    /**
     * Decides whether a cached profile may be shown. The cache is only valid when Firebase
     * reports a signed-in user and the cache was written for that same user, so a saved flag
     * alone never grants access and another account's profile is never shown.
     *
     * @param savedSignedInFlag value of the saved sign-in preference flag
     * @param cachedUserId      UID stored with the cached profile, or {@code null}
     * @param currentUserId     UID of the current Firebase user, or {@code null} if signed out
     * @return {@code true} if the cached profile may be displayed
     */
    public static boolean isCachedProfileValid(boolean savedSignedInFlag,
                                               @Nullable String cachedUserId,
                                               @Nullable String currentUserId) {
        return savedSignedInFlag
                && currentUserId != null
                && !currentUserId.isEmpty()
                && currentUserId.equals(cachedUserId);
    }

    /**
     * Checks that the account which started an asynchronous operation is still the signed-in
     * account, so a late callback does not write data for a different user.
     *
     * @param expectedUserId UID captured when the operation started
     * @param currentUserId  UID of the current Firebase user, or {@code null} if signed out
     * @return {@code true} if both UIDs are present and equal
     */
    public static boolean isSameSignedInUser(@Nullable String expectedUserId,
                                             @Nullable String currentUserId) {
        return expectedUserId != null
                && !expectedUserId.isEmpty()
                && expectedUserId.equals(currentUserId);
    }
}
