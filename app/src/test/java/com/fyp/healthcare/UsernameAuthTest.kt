package com.fyp.healthcare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure parts of the username + password flow: the shape rules the form enforces, the
 * username-to-address mapping Firebase needs, and the copy users see when a request is
 * rejected. Firebase itself is only reachable from a device, so it isn't covered here.
 */
class UsernameAuthTest {

    @Test
    fun acceptsUsernamesInRange() {
        listOf(
            "may",
            "auntieMay",
            "john_doe",
            "s.s",
            "bob-1",
            "9lives",
            "a".repeat(MAX_USERNAME_LENGTH),
        ).forEach { assertTrue("$it should be valid", isValidUsername(it)) }
    }

    @Test
    fun rejectsUsernamesOutsideTheRules() {
        listOf(
            "",
            "   ",
            "ma",                                   // under the minimum
            "a".repeat(MAX_USERNAME_LENGTH + 1),    // over the maximum
            ".may", "_may", "-may",                 // can't start with punctuation
            "may.", "may_", "may-",                  // can't end with punctuation
            "auntie may",                           // space
            "auntie@may",                           // not an allowed character
            "may!",
            "مَي",                                  // non-ASCII letters aren't accepted
        ).forEach { assertFalse("$it should be invalid", isValidUsername(it)) }
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        assertTrue(isValidUsername("  auntieMay  "))
    }

    @Test
    fun usernameMapsToOneLowercasedAddress() {
        // The address is an implementation detail: case-insensitive so "Alice" and "alice"
        // are one account, and on a TLD that can never receive mail.
        assertEquals("auntiemay@careapp.invalid", syntheticEmail("AuntieMay"))
        assertEquals("auntiemay@careapp.invalid", syntheticEmail("  AuntieMay  "))
        assertTrue(isSyntheticEmail("auntiemay@careapp.invalid"))
        assertFalse(isSyntheticEmail("may@gmail.com"))
        assertFalse(isSyntheticEmail(null))
    }

    @Test
    fun syntheticAddressIsNeverShownToUsers() {
        assertEquals("", displayAccountEmail("auntiemay@careapp.invalid"))
        assertEquals("", displayAccountEmail(null))
        assertEquals("may@gmail.com", displayAccountEmail("may@gmail.com"))
    }

    @Test
    fun failureCodesBecomePlainLanguage() {
        assertEquals("Wrong username or password.", authMessage("ERROR_INVALID_LOGIN_CREDENTIALS", null))
        assertEquals("That username is taken. Try another.", authMessage("ERROR_EMAIL_ALREADY_IN_USE", null))
        assertTrue(authMessage("ERROR_OPERATION_NOT_ALLOWED", null).contains("Firebase console"))
        // Unmapped codes fall back to the SDK's own message rather than a blank string.
        assertEquals("Some SDK text", authMessage("ERROR_BRAND_NEW_CODE", "Some SDK text"))
    }

    @Test
    fun googleLinkFailuresSayWhatToDoNext() {
        assertTrue(authMessage("ERROR_REQUIRES_RECENT_LOGIN", null).contains("password"))
        assertTrue(
            authMessage("ERROR_CREDENTIAL_ALREADY_IN_USE", null)
                .contains("already has its own CareApp account"),
        )
        assertEquals(
            "Google is already connected to this account.",
            authMessage("ERROR_PROVIDER_ALREADY_LINKED", null),
        )
    }

    /**
     * The same Firebase code means different things depending on what the user was doing, and
     * the connect path used to borrow the sign-up wording.
     */
    @Test
    fun aCollisionIsExplainedForTheOperationThatHitIt() {
        assertEquals(
            "That username is taken. Try another.",
            authMessage("ERROR_EMAIL_ALREADY_IN_USE", null, AuthOp.SignUp),
        )
        assertTrue(
            authMessage("ERROR_EMAIL_ALREADY_IN_USE", null, AuthOp.Link)
                .contains("already has its own CareApp account"),
        )
        // Omitting the operation keeps the sign-in wording.
        assertEquals("Wrong username or password.", authMessage("ERROR_INVALID_LOGIN_CREDENTIALS", null))
    }
}
