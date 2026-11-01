package com.fyp.healthcare

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the "N per consume × M times a day" model and the automatic reminder spacing. */
@RunWith(AndroidJUnit4::class)
class MedicationScheduleTest {

    private fun med(amount: Int, consumes: Int) = Medication(
        id = 1L,
        name = "Test",
        strength = "",
        amount = amount,
        schedule = (0..6).associateWith { defaultTimesPerDay(consumes) },
        consumesPerDay = consumes,
    )

    @Test
    fun defaultsAreSpacedThroughTheDay() {
        assertEquals(listOf("08:00"), defaultTimesPerDay(1))
        assertEquals(listOf("08:00", "20:00"), defaultTimesPerDay(2))
        assertEquals(listOf("08:00", "14:00", "20:00"), defaultTimesPerDay(3))
        assertEquals(listOf("08:00", "12:00", "16:00", "20:00"), defaultTimesPerDay(4))
        assertEquals(6, defaultTimesPerDay(6).size)
        assertEquals(12, defaultTimesPerDay(12).size)
    }

    @Test
    fun doseSummaryReadsPerConsumeAndPerDay() {
        val m = med(amount = 3, consumes = 4)
        assertEquals("3 tablets", m.doseText)
        assertEquals(4, m.consumeCount)
        assertEquals("4 times a day", m.frequencyText)
        assertEquals("3 tablets · 4 times a day", m.dosageText)
    }

    @Test
    fun legacyMedicationDerivesCountFromItsSchedule() {
        val m = Medication(
            id = 2L,
            name = "Old",
            strength = "500",
            amount = 1,
            schedule = (0..6).associateWith { listOf("08:00", "14:00", "20:00") },
        )
        assertEquals(3, m.consumeCount)
        assertEquals(3, m.consumesPerDayOrDefault)
        assertEquals("500 mg · 1 tablet · 3 times a day", m.dosageText)
    }
}
