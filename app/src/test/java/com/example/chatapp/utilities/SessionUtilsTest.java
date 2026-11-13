package com.example.chatapp.utilities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link SessionUtils}.
 */
public class SessionUtilsTest {

    /** A cache written for the current Firebase user is valid. */
    @Test
    public void cachedProfile_validForSameUser() {
        assertTrue(SessionUtils.isCachedProfileValid(true, "uid-1", "uid-1"));
    }

    /** A saved flag alone, without a Firebase user, never grants access. */
    @Test
    public void cachedProfile_invalidWithoutFirebaseUser() {
        assertFalse(SessionUtils.isCachedProfileValid(true, "uid-1", null));
        assertFalse(SessionUtils.isCachedProfileValid(true, "uid-1", ""));
    }

    /** A previous account's cache is never shown to a different account. */
    @Test
    public void cachedProfile_invalidForDifferentUser() {
        assertFalse(SessionUtils.isCachedProfileValid(true, "uid-1", "uid-2"));
        assertFalse(SessionUtils.isCachedProfileValid(true, null, "uid-2"));
    }

    /** Without the saved flag, the cache is treated as cleared. */
    @Test
    public void cachedProfile_invalidWithoutSavedFlag() {
        assertFalse(SessionUtils.isCachedProfileValid(false, "uid-1", "uid-1"));
    }

    /** Async callbacks may write only for the account that started them. */
    @Test
    public void sameSignedInUser_detectsAccountChanges() {
        assertTrue(SessionUtils.isSameSignedInUser("uid-1", "uid-1"));
        assertFalse(SessionUtils.isSameSignedInUser("uid-1", "uid-2"));
        assertFalse(SessionUtils.isSameSignedInUser("uid-1", null));
        assertFalse(SessionUtils.isSameSignedInUser(null, null));
        assertFalse(SessionUtils.isSameSignedInUser("", ""));
    }
}
