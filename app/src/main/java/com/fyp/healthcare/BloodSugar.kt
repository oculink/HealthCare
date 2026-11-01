package com.fyp.healthcare

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Blood glucose units.
 *
 * UK meters and labs report mmol/L; the app has always stored mg/dL (local history,
 * `bloodSugar` on reading docs, and the community `sugar` aggregate). Existing installs
 * still read those mg/dL fields, so mg/dL stays the stored value and this object only
 * converts at the edges: input from the Record Data screen and every display.
 *
 * Reading docs also carry `bloodSugarMmol`, written next to `bloodSugar`, so the UK value
 * is available in Firestore without a client-side conversion.
 */
object BloodSugar {

    /** The standard glucose conversion factor (glucose molar mass 180.16 g/mol). */
    const val MG_DL_PER_MMOL = 18.0182f

    fun mgDlToMmol(mgDl: Float): Float = mgDl / MG_DL_PER_MMOL

    fun mmolToMgDl(mmol: Float): Float = mmol * MG_DL_PER_MMOL

    /** 99 mg/dL -> "5.5" */
    fun formatMmol(mgDl: Float): String = formatMmolValue(mgDlToMmol(mgDl))

    /** An already-mmol value -> "5.5" */
    fun formatMmolValue(mmol: Float): String = String.format(Locale.US, "%.1f", mmol)

    /** A stored mg/dL string -> "5.5", or null when the string isn't a number. */
    fun formatStoredMmol(mgDl: String): String? =
        mgDl.trim().toFloatOrNull()?.let(::formatMmol)

    /** The canonical mg/dL string to store for a mmol/L entry: 5.5 -> "99.1". */
    fun storedMgDlFromMmol(mmol: Float): String {
        val mgDl = mmolToMgDl(mmol)
        val rounded = (mgDl * 10f).roundToInt() / 10f
        return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString()
        else String.format(Locale.US, "%.1f", rounded)
    }

    /** The one-decimal mmol/L value mirrored into Firestore. */
    fun mmolForCloud(mgDl: Float): Double =
        (mgDlToMmol(mgDl) * 10f).roundToInt() / 10.0
}
