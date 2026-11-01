package com.fyp.healthcare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Calendar

/**
 * Renders the health-report PDF with sample data and copies it to the app's external files
 * directory so it can be pulled off the device and eyeballed. Not a real assertion suite - a
 * visual-check harness for the PDF layout.
 *
 * adb pull /sdcard/Android/data/com.fyp.healthcare/files/report-preview/
 */
@RunWith(AndroidJUnit4::class)
class HealthReportPdfTest {

    private val profile = HealthProfile(
        name = "Aina Tan Mei Ling",
        bloodType = "B",
        heightCm = "162",
        weightKg = "58",
        birthDate = "2003-04-18",
        sex = "Female",
        allergies = listOf("Penicillin", "Shellfish"),
        conditions = listOf("Hypertension", "Type 2 Diabetes"),
    )

    /** A reading on [day] of last month at [hour]:[minute]. */
    private fun reading(
        day: Int, hour: Int, minute: Int,
        bp: String, hr: String, sugar: String, temp: String, oxygen: String,
    ): HealthDataManager.Reading {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return HealthDataManager.Reading(cal.timeInMillis, bp, sugar, hr, temp, oxygen)
    }

    private fun sampleReadings(): List<HealthDataManager.Reading> = listOf(
        reading(1, 8, 0, "116/74", "70", "99", "36.5", "98"),
        reading(1, 14, 0, "120/77", "73", "102", "36.6", "97"),
        reading(1, 21, 0, "117/75", "71", "98", "36.4", "97"),
        reading(2, 8, 10, "114/72", "68", "96", "36.4", "98"),
        reading(2, 14, 30, "118/75", "71", "101", "36.5", "97"),
        reading(2, 21, 30, "115/73", "69", "95", "36.5", "98"),
        reading(3, 9, 0, "119/76", "72", "100", "36.6", "97"),
    )

    private fun monthReadings(): List<HealthDataManager.Reading> {
        val out = mutableListOf<HealthDataManager.Reading>()
        for (day in 1..10) {
            out.add(reading(day, 8, 0, "${112 + day}/72", "${68 + day % 5}", "${96 + day}", "36.4", "98"))
            out.add(reading(day, 14, 30, "${115 + day}/74", "${70 + day % 4}", "${99 + day}", "36.6", "97"))
            out.add(reading(day, 21, 0, "${111 + day}/71", "${69 + day % 6}", "${97 + day}", "36.5", "98"))
        }
        return out
    }

    private fun abnormalReadings(): List<HealthDataManager.Reading> = listOf(
        reading(4, 8, 0, "152/96", "104", "168", "38.2", "92"),
        reading(4, 20, 0, "148/94", "101", "171", "38.4", "91"),
        reading(5, 8, 0, "146/92", "99", "164", "38.1", "93"),
        reading(5, 20, 0, "150/95", "102", "172", "38.3", "92"),
        reading(6, 8, 0, "144/90", "97", "160", "37.9", "94"),
    )

    private fun build(name: String, readings: List<HealthDataManager.Reading>, months: Int = 2) {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val reports = HealthReport.generateMonths(
            profile, readings, emptyList(), profile.name,
            rangeMonths = months, patientId = "CR-10023",
        )
        // Index 1 is last month: complete, and where the sample readings live.
        val report = reports.getOrElse(1) { reports.first() }
        val pdf = HealthReportExport.buildPdf(ctx, report)

        val outDir = File(ctx.getExternalFilesDir(null), "report-preview").apply { mkdirs() }
        val out = File(outDir, "$name.pdf")
        pdf.copyTo(out, overwrite = true)

        assertTrue("pdf missing", out.exists())
        assertTrue("pdf far too small (blank render?): ${out.length()}", out.length() > 8000)
        android.util.Log.i("HealthReportPdfTest", "$name: ${out.length()} bytes at ${out.absolutePath}")
    }

    @Test
    fun buildSamplePdf() {
        build("sample", sampleReadings())
    }

    @Test
    fun buildLongLogPdf() {
        build("long-log", monthReadings())
    }

    @Test
    fun buildEmptyPdf() {
        build("empty", emptyList())
    }

    @Test
    fun buildAlertsPdf() {
        build("alerts", abnormalReadings())
    }
}
