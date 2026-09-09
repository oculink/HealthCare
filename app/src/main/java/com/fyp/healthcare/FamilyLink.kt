package com.fyp.healthcare

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

/**
 * Family Caregiver linking.
 *
 * Model (Firestore):
 *   users/{uid}
 *     role: "patient" | "caretaker"
 *     familyCode: "ABCD2345"                         (patient - created lazily by [myCode])
 *     caretakerUids: ["uid", ...]                      (patient - read by the security rules)
 *     caretakers: [ {uid, name, photoUrl, linkedAt} ](patient - for display)
 *     linkedPatientUid: "uid"                        (caretaker - v1: one link)
 *
 *   familyCodes/{CODE}          (document id IS the code)
 *     patientUid: "uid"
 *     caretakerUids: ["uid", ...] (mirror; the users/{uid} attach rule cross-checks this)
 *
 * A caretaker who knows a code can append their own uid to familyCodes/{CODE} (authorised by
 * knowing the doc id); that then lets the rules accept them attaching to users/{patientUid}.
 * See firestore.rules (kept in the repo notes / pasted into the Firebase console).
 */
object FamilyLink {

    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val CODE_LENGTH = 8

    private val db get() = Firebase.firestore
    private fun codes() = db.collection("familyCodes")
    private fun users() = db.collection("users")

    data class PatientInfo(val uid: String, val name: String, val photoUrl: String?)
    data class CaretakerInfo(val uid: String, val name: String, val photoUrl: String?)

    private fun randomCode(): String =
        buildString { repeat(CODE_LENGTH) { append(ALPHABET.random()) } }

    private suspend fun freshUnusedCode(): String {
        var code = randomCode()
        repeat(6) {
            if (!codes().document(code).get().awaitResult().exists()) return code
            code = randomCode()
        }
        return code
    }

    // Patient side

    /** This account's family code, creating one on first use. */
    suspend fun myCode(): String {
        val self = Cloud.selfUid ?: error("Not signed in")
        val selfSnap = users().document(self).get().awaitResult()
        val existing = selfSnap.getString("familyCode")
        if (!existing.isNullOrBlank() &&
            codes().document(existing).get().awaitResult().exists()
        ) return existing

        val code = freshUnusedCode()
        codes().document(code)
            .set(mapOf("patientUid" to self, "caretakerUids" to emptyList<String>()))
            .awaitResult()
        users().document(self)
            .set(mapOf("familyCode" to code, "role" to "patient"), SetOptions.merge())
            .awaitResult()
        return code
    }

    /** Issue a brand-new code. Existing caregiver links are carried over and keep working. */
    suspend fun resetCode(): String {
        val self = Cloud.selfUid ?: error("Not signed in")
        val selfSnap = users().document(self).get().awaitResult()
        val old = selfSnap.getString("familyCode")
        @Suppress("UNCHECKED_CAST")
        val keep = (selfSnap.get("caretakerUids") as? List<String>) ?: emptyList()

        val code = freshUnusedCode()
        codes().document(code)
            .set(mapOf("patientUid" to self, "caretakerUids" to keep))
            .awaitResult()
        users().document(self)
            .set(mapOf("familyCode" to code), SetOptions.merge())
            .awaitResult()
        if (!old.isNullOrBlank() && old != code) {
            runCatching { codes().document(old).delete().awaitResult() }
        }
        return code
    }

    /** Caregivers currently linked to this account. */
    suspend fun linkedCaretakers(): List<CaretakerInfo> {
        val self = Cloud.selfUid ?: return emptyList()
        val snap = users().document(self).get().awaitResult()
        @Suppress("UNCHECKED_CAST")
        val raw = (snap.get("caretakers") as? List<Map<String, Any?>>).orEmpty()
        return raw.mapNotNull { m ->
            val uid = m["uid"] as? String ?: return@mapNotNull null
            CaretakerInfo(
                uid = uid,
                name = (m["name"] as? String)?.takeIf { it.isNotBlank() } ?: "Caregiver",
                photoUrl = (m["photoUrl"] as? String)?.takeIf { it.isNotBlank() },
            )
        }
    }

    /** Remove a caregiver's access entirely (patient-initiated). */
    suspend fun revokeCaretaker(uid: String) {
        val self = Cloud.selfUid ?: return
        val snap = users().document(self).get().awaitResult()
        @Suppress("UNCHECKED_CAST")
        val remaining = (snap.get("caretakers") as? List<Map<String, Any?>>).orEmpty()
            .filterNot { it["uid"] == uid }

        users().document(self).set(
            mapOf(
                "caretakerUids" to FieldValue.arrayRemove(uid),
                "caretakers" to remaining,
            ),
            SetOptions.merge(),
        ).awaitResult()

        snap.getString("familyCode")?.takeIf { it.isNotBlank() }?.let { code ->
            runCatching {
                codes().document(code)
                    .update("caretakerUids", FieldValue.arrayRemove(uid)).awaitResult()
            }
        }
    }

    // Caretaker side

    /** Link this account to the patient who owns [rawCode]. */
    suspend fun link(rawCode: String): Result<PatientInfo> {
        val self = Cloud.selfUid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val code = rawCode.trim().uppercase()
        if (code.length != CODE_LENGTH) {
            return Result.failure(IllegalArgumentException("Enter the $CODE_LENGTH-character code"))
        }

        val codeSnap = runCatching { codes().document(code).get().awaitResult() }
            .getOrElse { return Result.failure(it) }
        if (!codeSnap.exists()) {
            return Result.failure(IllegalArgumentException("That code doesn't exist"))
        }
        val patientUid = codeSnap.getString("patientUid")
            ?: return Result.failure(IllegalStateException("That code is not valid"))
        if (patientUid == self) {
            return Result.failure(IllegalArgumentException("That's your own code"))
        }

        val me = Firebase.auth.currentUser
        val myInfo = mapOf(
            "uid" to self,
            "name" to (me?.displayName?.takeIf { it.isNotBlank() }
                ?: me?.email?.substringBefore("@") ?: "Caregiver"),
            "photoUrl" to (me?.photoUrl?.toString() ?: ""),
            "linkedAt" to System.currentTimeMillis(),
        )

        return runCatching {
            codes().document(code)
                .update("caretakerUids", FieldValue.arrayUnion(self)).awaitResult()
            users().document(patientUid).set(
                mapOf(
                    "caretakerUids" to FieldValue.arrayUnion(self),
                    "caretakers" to FieldValue.arrayUnion(myInfo),
                ),
                SetOptions.merge(),
            ).awaitResult()
            users().document(self).set(
                mapOf("linkedPatientUid" to patientUid, "role" to "caretaker"),
                SetOptions.merge(),
            ).awaitResult()

            val p = users().document(patientUid).get().awaitResult()
            PatientInfo(
                uid = patientUid,
                name = displayNameOf(p),
                photoUrl = p.getString("photoUrl")?.takeIf { it.isNotBlank() },
            )
        }
    }

    /** Undo the current caretaker link. */
    suspend fun unlink() {
        val self = Cloud.selfUid ?: return
        val selfSnap = users().document(self).get().awaitResult()
        val patientUid = selfSnap.getString("linkedPatientUid") ?: return

        runCatching {
            val p = users().document(patientUid).get().awaitResult()
            @Suppress("UNCHECKED_CAST")
            val remaining = (p.get("caretakers") as? List<Map<String, Any?>>).orEmpty()
                .filterNot { it["uid"] == self }
            users().document(patientUid).set(
                mapOf(
                    "caretakerUids" to FieldValue.arrayRemove(self),
                    "caretakers" to remaining,
                ),
                SetOptions.merge(),
            ).awaitResult()

            p.getString("familyCode")?.takeIf { it.isNotBlank() }?.let { code ->
                runCatching {
                    codes().document(code)
                        .update("caretakerUids", FieldValue.arrayRemove(self)).awaitResult()
                }
            }
        }
        users().document(self)
            .set(mapOf("linkedPatientUid" to FieldValue.delete()), SetOptions.merge())
            .awaitResult()
    }

    /**
     * Rebuild [Session] from the signed-in account's cloud doc - the chosen role, and any
     * existing caretaker link. Call right after sign-in (local session state is per-device and
     * is cleared on sign-out, so a returning user must be restored from the cloud).
     */
    suspend fun restoreSession(context: Context) {
        val self = Cloud.selfUid ?: return
        val snap = users().document(self).getFresh() ?: return

        val role = snap.getString("role").orEmpty()
        if (role.isNotBlank()) Session.setRole(context, role)

        val patientUid = snap.getString("linkedPatientUid")?.takeIf { it.isNotBlank() }
        if (role == "caretaker" && patientUid != null) {
            val p = users().document(patientUid).getFresh()
            if (p != null && p.exists()) {
                Session.enterCaretakerMode(
                    context,
                    patientUid,
                    displayNameOf(p),
                    p.getString("photoUrl")?.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    /** Health-profile name, then the Google account name, then a generic fallback. */
    private fun displayNameOf(d: com.google.firebase.firestore.DocumentSnapshot): String =
        d.getString("name")?.takeIf { it.isNotBlank() }
            ?: d.getString("accountName")?.takeIf { it.isNotBlank() }
            ?: "Patient"
}
