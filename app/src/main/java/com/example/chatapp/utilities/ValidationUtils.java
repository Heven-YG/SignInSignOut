package com.example.chatapp.utilities;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Input validation for the sign-in and sign-up forms.
 *
 * <p>The checks are plain Java so they can run in local unit tests. Firebase still performs
 * its own server-side checks, which the screens report separately.</p>
 */
public final class ValidationUtils {

    /** Possible outcomes of validating a form. */
    public enum Result {
        /** All fields are valid. */
        VALID,
        /** The name field is empty. */
        NAME_REQUIRED,
        /** The email field is empty. */
        EMAIL_REQUIRED,
        /** The email field is not a valid address. */
        EMAIL_INVALID,
        /** The password field is empty. */
        PASSWORD_REQUIRED,
        /** The password is shorter than Firebase's minimum length. */
        PASSWORD_TOO_SHORT,
        /** The confirm-password field is empty. */
        CONFIRM_PASSWORD_REQUIRED,
        /** The two password fields differ. */
        PASSWORDS_DO_NOT_MATCH
    }

    /**
     * Practical email pattern: a local part, an "@", and a domain with at least one dot and
     * a top-level domain of two or more letters.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    /** Prevents instantiation of this utility class. */
    private ValidationUtils() {
    }

    /**
     * Checks whether text is null, empty, or only whitespace.
     *
     * @param value text to check
     * @return {@code true} if there is no meaningful content
     */
    public static boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Checks whether an email address has a valid format. Surrounding whitespace is ignored.
     *
     * @param email address to check
     * @return {@code true} if the address matches the expected format
     */
    public static boolean isValidEmail(@Nullable String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates the sign-in form.
     *
     * @param email    entered email
     * @param password entered password
     * @return the first problem found, or {@link Result#VALID}
     */
    public static Result validateSignIn(@Nullable String email, @Nullable String password) {
        if (isBlank(email)) {
            return Result.EMAIL_REQUIRED;
        }
        if (!isValidEmail(email)) {
            return Result.EMAIL_INVALID;
        }
        if (password == null || password.isEmpty()) {
            return Result.PASSWORD_REQUIRED;
        }
        return Result.VALID;
    }

    /**
     * Validates the full sign-up form, including Firebase's minimum password length.
     *
     * @param name            entered display name
     * @param email           entered email
     * @param password        entered password
     * @param confirmPassword entered password confirmation
     * @return the first problem found, or {@link Result#VALID}
     */
    public static Result validateSignUp(@Nullable String name, @Nullable String email,
                                        @Nullable String password,
                                        @Nullable String confirmPassword) {
        if (isBlank(name)) {
            return Result.NAME_REQUIRED;
        }
        if (isBlank(email)) {
            return Result.EMAIL_REQUIRED;
        }
        if (!isValidEmail(email)) {
            return Result.EMAIL_INVALID;
        }
        if (password == null || password.isEmpty()) {
            return Result.PASSWORD_REQUIRED;
        }
        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            return Result.PASSWORD_TOO_SHORT;
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return Result.CONFIRM_PASSWORD_REQUIRED;
        }
        if (!password.equals(confirmPassword)) {
            return Result.PASSWORDS_DO_NOT_MATCH;
        }
        return Result.VALID;
    }

    /**
     * Validates the profile-completion form, which only needs a name.
     *
     * @param name entered display name
     * @return {@link Result#NAME_REQUIRED} or {@link Result#VALID}
     */
    public static Result validateProfileCompletion(@Nullable String name) {
        return isBlank(name) ? Result.NAME_REQUIRED : Result.VALID;
    }
}
