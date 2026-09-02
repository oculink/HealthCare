package com.fyp.healthcare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Calendar

/**
 * Renders the health-report PDF with sample data and copies it to external files so it can be
 * pulled off the device and eyeballed. Not a real assertion suite — a visual-check harness.
 */
@RunWith(AndroidJUnit4::class)
class HealthReportPdfTest {

    @Test
    fun buildSamplePdf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext

        val profile = HealthProfile(
            name = "Meow",
            bloodType = "B",
            heightCm = "170",
            weightKg = "80",
            birthDate = "1960-02-01",
            sex = "Male",
            allergies = listOf("Penicillin", "Shellfish"),
            conditions = listOf("Hypertension", "Type 2 Diabetes"),
        )

        fun day(offset: Int, hour: Int): Long = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offset)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0)
        }.timeInMillis

        val readings = listOf(
            HealthDataManager.Reading(day(-1, 9), "104/86", "68", "66", "36.4", "93"),
            HealthDataManager.Reading(day(-1, 20), "106/88", "70", "65", "36.6", "94"),
            HealthDataManager.Reading(day(0, 8), "110/92", "62", "64", "36.5", "91"),
            HealthDataManager.Reading(day(0, 19), "106/92", "62", "63", "36.4", "92"),
        )

        val report = HealthReport.generate(profile, readings, emptyList(), "Meow")
        val pdf = HealthReportExport.buildPdf(ctx, report)

        assertTrue("pdf missing", pdf.exists())
        assertTrue("pdf far too small (blank render?): ${pdf.length()}", pdf.length() > 8000)
        android.util.Log.i("HealthReportPdfTest", "built ${pdf.length()} byte PDF at ${pdf.absolutePath}")
        // To eyeball it: add `HealthReportExport.download(ctx, report)` here, re-run, then
        // `adb pull /sdcard/Download/CareApp-Health-Report-*.pdf`.
    }
}
