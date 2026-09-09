package com.fyp.healthcare

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.firestore
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * LIVE caregiver remote monitoring.
 *
 * [CloudHydrator.hydrate] pulls the linked patient's data down once per app open / resume.
 * This adds the *live tail*: while a caretaker is in caretaker mode AND the app is in the
 * foreground, it keeps Firestore snapshot listeners open on the patient's docs, so a
 * reading the patient records (or a dose they log, or a profile edit) lands in the
 * caretaker's local `*__<patientUid>` cache within a second — no reopen, no pull-to-refresh.
 *
 * It writes through the SAME [ProfileManager.hydrateLocal] / [HealthDataManager.hydrateLocal]
 * / [MedicationManager.hydrateLocal] path the hydrator uses and then bumps
 * [Session.dataVersion], so every screen that already re-reads on `dataVersion` updates
 * live with no further changes.
 *
 * Foreground-only by design: `MainActivity` calls [start] from `onResume` and [stop] from
 * `onStop`. Firestore keeps the listener's last payload in its on-device cache, so the next
 * resume shows fresh data instantly while the listeners re-attach. Backgrounded push alerts
 * would need FCM + a server (not available on the Spark plan) — see the FYP report.
 */
object PatientMonitor {

    /** true while live listeners are attached to a patient — drives the Home "· live" pill. */
    var isLive by mutableStateOf(false)
        private set

    private val registrations = mutableListOf<ListenerRegistration>()
    private var watching: String? = null

    private val worker: Executor = Executors.newSingleThreadExecutor()

    /**
     * Start (or re-target) live monitoring of the currently linked patient.
     * No-op in self mode, and idempotent while already watching the same patient.
     */
    fun start(context: Context) {
        val patientUid = Session.controlledPatientUid
        if (patientUid == null) { stop(); return }
        if (watching == patientUid && registrations.isNotEmpty()) return
        stop()
        watching = patientUid

        val app = context.applicationContext
        val userRef: DocumentReference =
            Firebase.firestore.collection("users").document(patientUid)

        registrations += userRef.addSnapshotListener(worker, MetadataChanges.EXCLUDE) { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
            runCatching {
                val profile = CloudHydrator.profileFromSnapshot(snap)
                if (profile.name.isNotBlank() || !profile.isBlank) {
                    ProfileManager(app).hydrateLocal(profile)
                }
                val today = dateKey()
                val syncedSteps = stepsFromByDevice(snap.get("stepsByDevice"), today)
                val am = ActivityDataManager(app)
                am.hydrateLocal(
                    snap.getLong("stepGoal")?.toInt(),
                    syncedSteps,
                    if (syncedSteps != null) today else null,
                )
                am.hydrateSleep(snap.get("sleepByDevice"))
            }
            Session.bumpDataVersion()
        }

        registrations += userRef.collection("readings")
            .addSnapshotListener(worker, MetadataChanges.EXCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                runCatching {
                    val readings = snap.documents
                        .map { HealthDataManager.readingFrom(it) }
                        .sortedBy { it.timestamp }
                    HealthDataManager(app).hydrateLocal(readings)
                }
                Session.bumpDataVersion()
            }

        registrations += userRef.collection("medications")
            .addSnapshotListener(worker, MetadataChanges.EXCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                runCatching {
                    val meds = snap.documents.mapNotNull { MedicationManager.medicationFrom(it) }
                    MedicationManager(app).hydrateLocal(meds)
                }
                Session.bumpDataVersion()
            }

        registrations += userRef.collection("appointments")
            .addSnapshotListener(worker, MetadataChanges.EXCLUDE) { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                runCatching {
                    val appts = snap.documents.mapNotNull { Appointment.appointmentFrom(it) }
                    AppointmentManager(app).hydrateLocal(appts)
                }
                Session.bumpDataVersion()
            }

        isLive = true
    }

    /** Detach all live listeners (background / unlink / sign-out). */
    fun stop() {
        registrations.forEach { it.remove() }
        registrations.clear()
        watching = null
        isLive = false
    }
}
