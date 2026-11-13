package com.example.chatapp.firebase;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chatapp.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.concurrent.TimeoutException;

/**
 * Converts Firebase and task exceptions into short, user-readable messages. Raw exception
 * text is never shown because it may be technical or reveal account details.
 */
public final class FirebaseErrorMessages {

    private static final String ERROR_USER_DISABLED = "ERROR_USER_DISABLED";

    /** Prevents instantiation of this utility class. */
    private FirebaseErrorMessages() {
    }

    /**
     * Returns a message describing why an operation failed.
     *
     * @param context   context used to read string resources
     * @param exception the failure, possibly {@code null}
     * @return a localized, user-facing message
     */
    @NonNull
    public static String from(@NonNull Context context, @Nullable Exception exception) {
        return context.getString(messageRes(exception));
    }

    /**
     * Chooses the string resource for a failure. Order matters because some Firebase
     * exceptions extend others, for example the weak-password exception extends the
     * invalid-credentials exception.
     *
     * @param exception the failure, possibly {@code null}
     * @return a string resource ID
     */
    private static int messageRes(@Nullable Exception exception) {
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return R.string.error_weak_password;
        }
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return R.string.error_email_in_use;
        }
        if (exception instanceof FirebaseAuthInvalidUserException) {
            String code = ((FirebaseAuthInvalidUserException) exception).getErrorCode();
            return ERROR_USER_DISABLED.equals(code)
                    ? R.string.error_user_disabled : R.string.error_invalid_credentials;
        }
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return R.string.error_invalid_credentials;
        }
        if (exception instanceof FirebaseTooManyRequestsException) {
            return R.string.error_too_many_requests;
        }
        if (exception instanceof FirebaseNetworkException) {
            return R.string.error_network;
        }
        if (exception instanceof TimeoutException) {
            return R.string.error_timeout;
        }
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code =
                    ((FirebaseFirestoreException) exception).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    || code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                return R.string.error_permission_denied;
            }
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                    || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                return R.string.error_network;
            }
        }
        return R.string.error_generic;
    }
}
