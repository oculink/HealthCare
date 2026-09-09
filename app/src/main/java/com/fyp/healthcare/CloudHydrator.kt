package com.fyp.healthcare

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore

/**
 * Pulls the "current target" account's data down from Firestore into the local
 * SharedPreferences cache, so the screens (which read locally and synchronously) show
 * the right thing:
 *
 *  - self mode  -> refreshes the signed-in user's own cache on every app open, so a
 *                 linked caretaker's changes appear.
 *  - caretaker mode -> fills the `*__<patientUid>` cache with the patient's data.
 *
 * Which account is the target is decided by [Cloud.targetUid] (driven by [Session]).
 * Writes still flow the other way through the managers' normal mirror-to-cloud path.
 *
 * All best-effort: every step is wrapped so a missing network / empty collection never
 * throws and never wipes good local data (each `hydrateLocal` no-ops on empty input).
 */
object CloudHydrator {

    suspend fun hydrate(context: Context) {
        val target = Cloud.targetUid ?: return
        val userRef = Firebase.firestore.collection("users").document(target)

        runCatching {
            val snap = readDoc(userRef) ?: return@runCatching
            if (!snap.exists()) return@runCatching
            val profile = profileFromSnapshot(snap)
            if (profile.name.isNotBlank() || !profile.isBlank) {
                ProfileManager(context).hydrateLocal(profile)
            }
            val today = dateKey()
            val syncedSteps = stepsFromByDevice(snap.get("stepsByDevice"), today)
            val am = ActivityDataManager(context)
            am.hydrateLocal(
                snap.getLong("stepGoal")?.toInt(),
                syncedSteps,
                if (syncedSteps != null) today else null,
            )
            am.hydrateSleep(snap.get("sleepByDevice"))
        }

        runCatching {
            val hm = HealthDataManager(context)
            val readings = runCatching { hm.cloudHistory(fromServer = true) }.getOrNull()
                ?: runCatching { hm.cloudHistory(fromServer = false) }.getOrNull()
                ?: emptyList()
            hm.hydrateLocal(readings)
        }

        runCatching {
            val mm = MedicationManager(context)
            val meds = runCatching { mm.cloudList(fromServer = true) }.getOrNull()
                ?: runCatching { mm.cloudList(fromServer = false) }.getOrNull()
                ?: emptyList()
            mm.hydrateLocal(meds)
        }

        runCatching {
            val am = AppointmentManager(context)
            val appts = runCatching { am.cloudList(fromServer = true) }.getOrNull()
                ?: runCatching { am.cloudList(fromServer = false) }.getOrNull()
                ?: emptyList()
            am.hydrateLocal(appts)
        }

        Session.bumpDataVersion()
    }

    private suspend fun readDoc(ref: DocumentReference): DocumentSnapshot? = ref.getFresh()

    internal fun profileFromSnapshot(d: DocumentSnapshot): HealthProfile {
        @Suppress("UNCHECKED_CAST")
        val allergies = (d.get("allergies") as? List<String>).orEmpty()
        @Suppress("UNCHECKED_CAST")
        val conditions = (d.get("conditions") as? List<String>).orEmpty()
        @Suppress("UNCHECKED_CAST")
        val contactsRaw = (d.get("emergencyContacts") as? List<Map<String, Any?>>).orEmpty()
        return HealthProfile(
            name = d.getString("name").orEmpty(),
            bloodType = d.getString("bloodType").orEmpty(),
            heightCm = d.getString("heightCm").orEmpty(),
            weightKg = d.getString("weightKg").orEmpty(),
            birthDate = d.getString("birthDate").orEmpty(),
            sex = d.getString("sex").orEmpty(),
            allergies = allergies,
            conditions = conditions,
            emergencyContacts = contactsRaw.map {
                EmergencyContact(
                    name = it["name"] as? String ?: "",
                    relation = it["relation"] as? String ?: "",
                    phone = it["phone"] as? String ?: "",
                )
            },
        )
    }
}
