package com.fyp.healthcare

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Phase-1 cloud sync.
 *
 * Every local save in the app is ALSO mirrored to Firestore under `users/{uid}`:
 *   users/{uid}                     <- health profile fields + stepGoal
 *   users/{uid}/readings/{auto}     <- one doc per recorded set of vitals
 *   users/{uid}/medications/{id}    <- one doc per medication (incl. the taken/missed log)
 *
 * Most reads still come from local SharedPreferences (instant, works offline). The
 * exception is Health Trends, which reads the full `readings` history straight from
 * Firestore (cache-first, then a background server refresh) so the stats and chart
 * follow the account across devices. Firestore is the durable copy + the basis for
 * multi-device / the caregiver view later.
 *
 * Writes are fire-and-forget — Firestore's on-device cache queues them while offline and
 * flushes automatically on reconnect.
 */
object Cloud {

    val uid: String? get() = Firebase.auth.currentUser?.uid

    val userDoc: DocumentReference?
        get() = uid?.let { Firebase.firestore.collection("users").document(it) }

    /**
     * One-time upload of data that already existed locally before cloud sync (or that was
     * created while offline / signed out). Normal saves mirror themselves, so this only
     * fills the backlog. Runs at most once per account per install.
     */
    fun pushBacklog(context: Context) {
        val u = uid ?: return
        val prefs = context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
        if (prefs.getBoolean("backlog_$u", false)) return

        runCatching {
            ProfileManager(context).syncToCloud()
            ActivityDataManager(context).syncToCloud()
            MedicationManager(context).syncAllToCloud()
            HealthDataManager(context).syncLatestToCloud()
        }
        prefs.edit().putBoolean("backlog_$u", true).apply()
    }

    // ===================================================================
    // Community baseline — anonymous, aggregate-only.
    //
    // Every user's readings roll up into ONE shared doc `stats/vitals`, so the
    // Health Trends screen can show "you vs. everyone". No individual reading and
    // no identity is stored here — only running count / sum / min / max per metric.
    // Metric keys: "hr" (BPM), "sys" (systolic), "sugar" (mg/dL), "oxy" (%).
    //
    // The bump runs client-side in a Firestore transaction (works on the free
    // plan). A tampered client could skew the numbers — acceptable for the FYP;
    // a Cloud Function would make it tamper-proof later.
    // ===================================================================

    private val statsDoc get() = Firebase.firestore.collection("stats").document("vitals")

    /** Fold one recorded set of vitals into the shared community aggregate. */
    fun bumpCommunityStats(values: Map<String, Double>) {
        if (values.isEmpty() || uid == null) return
        val db = Firebase.firestore
        db.runTransaction { txn ->
            val snap = txn.get(statsDoc)
            val out = HashMap<String, Any>()
            for ((k, v) in values) {
                val count = (snap.getDouble("${k}Count") ?: 0.0) + 1.0
                val sum = (snap.getDouble("${k}Sum") ?: 0.0) + v
                val min = minOf(snap.getDouble("${k}Min") ?: Double.MAX_VALUE, v)
                val max = maxOf(snap.getDouble("${k}Max") ?: -Double.MAX_VALUE, v)
                out["${k}Count"] = count
                out["${k}Sum"] = sum
                out["${k}Min"] = min
                out["${k}Max"] = max
            }
            out["updatedAt"] = FieldValue.serverTimestamp()
            txn.set(statsDoc, out, SetOptions.merge())
            null
        }
    }

    data class CommunityStat(val avg: Double, val min: Double, val max: Double, val count: Long)

    /** Read the community aggregate. Empty map when signed out / never populated / offline. */
    suspend fun communityStats(): Map<String, CommunityStat> {
        if (uid == null) return emptyMap()
        val snap = runCatching { statsDoc.get().awaitResult() }.getOrNull() ?: return emptyMap()
        fun stat(k: String): CommunityStat? {
            val count = snap.getDouble("${k}Count") ?: return null
            if (count < 1.0) return null
            val sum = snap.getDouble("${k}Sum") ?: return null
            return CommunityStat(
                avg = sum / count,
                min = snap.getDouble("${k}Min") ?: 0.0,
                max = snap.getDouble("${k}Max") ?: 0.0,
                count = count.toLong(),
            )
        }
        return buildMap {
            stat("hr")?.let { put("hr", it) }
            stat("sys")?.let { put("sys", it) }
            stat("sugar")?.let { put("sugar", it) }
            stat("oxy")?.let { put("oxy", it) }
        }
    }
}

// Task<T> -> suspend, without pulling in kotlinx-coroutines-play-services.
internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val e = task.exception
        if (e != null) cont.resumeWithException(e)
        else if (task.isCanceled) cont.cancel()
        else cont.resume(task.result)
    }
}
