package com.fyp.healthcare

import com.fyp.healthcare.i18n.AppLanguage
import com.fyp.healthcare.i18n.MalayStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Malay side of the dictionary.
 *
 * The app looks strings up by their English text, so the two ways this can go wrong are a key
 * that no longer matches a screen (the label silently stays English) and a translation that
 * loses the number it was built around ("3 readings" becoming just "bacaan"). Both are cheap
 * to check here and expensive to spot by tapping through the app.
 */
class MalayStringsTest {

    @Test
    fun translatesTheGetStartedCopy() {
        assertEquals("Mula", MalayStrings.lookup("Get Started"))
        assertEquals("Log masuk", MalayStrings.lookup("Sign in"))
        assertEquals("Tambah", MalayStrings.lookup("Add"))
    }

    @Test
    fun leavesUnknownTextAlone() {
        assertEquals("Amlodipine", MalayStrings.lookup("Amlodipine"))
        assertEquals("auntieMay", MalayStrings.lookup("auntieMay"))
        assertEquals("", MalayStrings.lookup(""))
    }

    @Test
    fun keepsNumbersWhenTheUnitIsTranslated() {
        assertEquals("1,000 langkah", MalayStrings.lookup("1,000 steps"))
        assertEquals("3 bacaan", MalayStrings.lookup("3 readings"))
        assertEquals("2 hari lalu", MalayStrings.lookup("2 days ago"))
        assertEquals("4 kali sehari", MalayStrings.lookup("4 times a day"))
        assertEquals("Padam Amlodipine?", MalayStrings.lookup("Delete Amlodipine?"))
        assertEquals("· halaman 2 daripada 5", MalayStrings.lookup("· page 2 of 5"))
    }

    @Test
    fun englishIsTheUnchangedSourceLanguage() {
        assertSame(AppLanguage.ENGLISH, AppLanguage.entries.first { it.shortLabel == "EN" })
        assertNotEquals(AppLanguage.ENGLISH.tag, AppLanguage.MALAY.tag)
        assertNotEquals(AppLanguage.ENGLISH.shortLabel, AppLanguage.MALAY.shortLabel)
    }

    @Test
    fun everyTranslationIsNonBlankApartFromDeliberateElisions() {
        // "'s account" collapses to nothing in Malay ("Mengurus akaun Ahmad"), which is the
        // only entry that is allowed to be blank.
        val blank = listOf("'s account")
        listOf(
            "Get Started", "Profile", "Language", "Reminders", "Emergency", "Settings",
            "Save", "Cancel", "Delete", "Blood Pressure", "Appointments", "Heart Rate",
        ).forEach { key ->
            val translated = MalayStrings.lookup(key)
            assertTrue("$key has no Malay translation", translated.isNotBlank())
            assertTrue("$key was left in English", translated != key || blank.contains(key))
        }
    }
}
