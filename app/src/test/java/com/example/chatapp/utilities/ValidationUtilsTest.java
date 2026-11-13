package com.example.chatapp.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ValidationUtils}.
 */
public class ValidationUtilsTest {

    /** Accepts common well-formed addresses, ignoring surrounding whitespace. */
    @Test
    public void isValidEmail_acceptsWellFormedAddresses() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("first.last+tag@mail.example.co.uk"));
        assertTrue(ValidationUtils.isValidEmail("  user@example.com  "));
    }

    /** Rejects missing parts, spaces, and missing top-level domains. */
    @Test
    public void isValidEmail_rejectsMalformedAddresses() {
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail("user"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@example"));
        assertFalse(ValidationUtils.isValidEmail("us er@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@example.c"));
    }

    /** Reports each sign-in problem in field order. */
    @Test
    public void validateSignIn_reportsFirstProblem() {
        assertEquals(ValidationUtils.Result.EMAIL_REQUIRED,
                ValidationUtils.validateSignIn("   ", "secret1"));
        assertEquals(ValidationUtils.Result.EMAIL_INVALID,
                ValidationUtils.validateSignIn("not-an-email", "secret1"));
        assertEquals(ValidationUtils.Result.PASSWORD_REQUIRED,
                ValidationUtils.validateSignIn("user@example.com", ""));
        assertEquals(ValidationUtils.Result.VALID,
                ValidationUtils.validateSignIn("user@example.com", "x"));
    }

    /** Requires a non-blank name before anything else. */
    @Test
    public void validateSignUp_requiresName() {
        assertEquals(ValidationUtils.Result.NAME_REQUIRED,
                ValidationUtils.validateSignUp(" ", "user@example.com", "secret1", "secret1"));
    }

    /** Enforces Firebase's minimum password length at the boundary. */
    @Test
    public void validateSignUp_enforcesMinimumPasswordLength() {
        String tooShort = repeat('a', Constants.MIN_PASSWORD_LENGTH - 1);
        String minimum = repeat('a', Constants.MIN_PASSWORD_LENGTH);
        assertEquals(ValidationUtils.Result.PASSWORD_TOO_SHORT,
                ValidationUtils.validateSignUp("Alex", "user@example.com", tooShort, tooShort));
        assertEquals(ValidationUtils.Result.VALID,
                ValidationUtils.validateSignUp("Alex", "user@example.com", minimum, minimum));
    }

    /** Requires the confirmation and checks that it matches exactly. */
    @Test
    public void validateSignUp_checksConfirmation() {
        assertEquals(ValidationUtils.Result.CONFIRM_PASSWORD_REQUIRED,
                ValidationUtils.validateSignUp("Alex", "user@example.com", "secret1", ""));
        assertEquals(ValidationUtils.Result.PASSWORDS_DO_NOT_MATCH,
                ValidationUtils.validateSignUp("Alex", "user@example.com", "secret1", "Secret1"));
    }

    /** Profile completion only needs a name. */
    @Test
    public void validateProfileCompletion_requiresOnlyName() {
        assertEquals(ValidationUtils.Result.NAME_REQUIRED,
                ValidationUtils.validateProfileCompletion(null));
        assertEquals(ValidationUtils.Result.VALID,
                ValidationUtils.validateProfileCompletion("Alex"));
    }

    /**
     * Builds a string of one repeated character.
     *
     * @param c     character to repeat
     * @param count number of repetitions
     * @return the resulting string
     */
    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
