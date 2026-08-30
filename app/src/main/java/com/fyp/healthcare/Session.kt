package com.fyp.healthcare

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Who is using the app right now, and whose data they are acting on.
 *
 * - [role] is chosen once on the Role Select screen (right after Google sign-in) and
 *   remembered locally: "" (not chosen yet), "patient", or "caretaker".
 * - When a caretaker links to a patient, [controlledPatientUid] is set. From then on
 *   every data manager reads/writes the PATIENT's data instead of the caretaker's own
 *   (see [scopedPrefsName] + `Cloud.targetUid`). [isCaretakerMode] is the switch the
 *   rest of the app checks.
 *
 * Backed by a small SharedPreferences file so the choice + active link survive a
 * process restart. Modelled on [com.fyp.healthcare.ui.theme.AppTheme].
 */
object Session {

    var role by mutableStateOf("")
        private set

    var controlledPatientUid by mutableStateOf<String?>(null)
        private set

    var controlledPatientName by mutableStateOf("")
        private set

    var controlledPatientPhoto by mutableStateOf<String?>(null)
        private set

    val isCaretakerMode: Boolean get() = controlledPatientUid != null

    /**
     * Bumped by [CloudHydrator] after it refreshes the local cache from Firestore. Screens read
     * this in a `remember(..., Session.dataVersion)` key so they re-read the managers once new
     * data has landed (the managers themselves are plain SharedPreferences, not observable).
     */
    var dataVersion by mutableStateOf(0)
        private set

    fun bumpDataVersion() { dataVersion++ }

    /** Load the saved choice — call once from MainActivity.onCreate, before setContent. */
    fun init(context: Context) {
        val p = prefs(context)
        role = p.getString(KEY_ROLE, "") ?: ""
        controlledPatientUid = p.getString(KEY_PATIENT_UID, null)
        controlledPatientName = p.getString(KEY_PATIENT_NAME, "") ?: ""
        controlledPatientPhoto = p.getString(KEY_PATIENT_PHOTO, null)
    }

    fun setRole(context: Context, newRole: String) {
        role = newRole
        prefs(context).edit().putString(KEY_ROLE, newRole).apply()
    }

    fun enterCaretakerMode(context: Context, patientUid: String, patientName: String, patientPhoto: String?) {
        controlledPatientUid = patientUid
        controlledPatientName = patientName
        controlledPatientPhoto = patientPhoto
        role = "caretaker"
        prefs(context).edit()
            .putString(KEY_ROLE, "caretaker")
            .putString(KEY_PATIENT_UID, patientUid)
            .putString(KEY_PATIENT_NAME, patientName)
            .putString(KEY_PATIENT_PHOTO, patientPhoto)
            .apply()
    }

    /** Leave the patient's account but keep the "caretaker" role. */
    fun exitCaretakerMode(context: Context) {
        controlledPatientUid = null
        controlledPatientName = ""
        controlledPatientPhoto = null
        prefs(context).edit()
            .remove(KEY_PATIENT_UID)
            .remove(KEY_PATIENT_NAME)
            .remove(KEY_PATIENT_PHOTO)
            .apply()
    }

    /** Full reset — called on sign out. */
    fun clear(context: Context) {
        role = ""
        controlledPatientUid = null
        controlledPatientName = ""
        controlledPatientPhoto = null
        prefs(context).edit().clear().apply()
        clearLocalData(context)
    }

    /**
     * Wipe the per-account local caches so the NEXT account doesn't inherit this one's data.
     * In self mode these files have fixed names (`health_data`, `profile`, …) shared by every
     * signed-in account, so without this a new sign-in shows the previous user's readings,
     * profile and meds until (if ever) the cloud overwrites them. Firestore stays untouched —
     * each account re-hydrates its own data on next open.
     *
     * Kept: `session`, `app_prefs` (theme), `cloud_sync` (per-uid backlog flags).
     */
    private fun clearLocalData(context: Context) {
        // Self-mode files have fixed names; caretaker-scoped copies are `<base>__<patientUid>`.
        // Enumerate shared_prefs/ so both are covered.
        val known = DATA_PREFS.toMutableSet()
        java.io.File(context.applicationInfo.dataDir, "shared_prefs").listFiles()?.forEach { f ->
            val n = f.name.removeSuffix(".xml")
            if (DATA_PREFS.any { n == it || n.startsWith("${it}__") }) known.add(n)
        }
        known.forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    private val DATA_PREFS = listOf("health_data", "profile", "activity", "medications")

    private fun prefs(c: Context) = c.getSharedPreferences("session", Context.MODE_PRIVATE)

    private const val KEY_ROLE = "role"
    private const val KEY_PATIENT_UID = "patient_uid"
    private const val KEY_PATIENT_NAME = "patient_name"
    private const val KEY_PATIENT_PHOTO = "patient_photo"
}

/**
 * Local SharedPreferences file name for a data store. In caretaker mode the patient's
 * data is cached in a separate `<base>__<patientUid>` file so it never mixes with the
 * caretaker's own records. Self mode keeps the original name (no migration needed).
 */
fun scopedPrefsName(base: String): String =
    Session.controlledPatientUid?.let { "${base}__$it" } ?: base
