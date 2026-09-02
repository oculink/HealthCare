package com.fyp.healthcare

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import java.util.Calendar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    /** The signed-in account, always — used for auth-scoped writes (role, family link). */
    val selfUid: String? get() = Firebase.auth.currentUser?.uid

    val selfDoc: DocumentReference?
        get() = selfUid?.let { Firebase.firestore.collection("users").document(it) }

    /**
     * Whose data the app is currently acting on: the linked patient when a caretaker is
     * in caretaker mode, otherwise the signed-in user. Every manager's cloud mirror
     * hangs off this.
     */
    val targetUid: String? get() = Session.controlledPatientUid ?: selfUid

    /** Back-compat alias — many callers still read `Cloud.uid`. */
    val uid: String? get() = targetUid

    val userDoc: DocumentReference?
        get() = targetUid?.let { Firebase.firestore.collection("users").document(it) }

    /**
     * One-time upload of data that already existed locally before cloud sync (or that was
     * created while offline / signed out). Normal saves mirror themselves, so this only
     * fills the backlog. Runs at most once per account per install.
     */
    fun pushBacklog(context: Context) {
        // Only the account's own local backlog — never run while acting on a patient's data.
        if (Session.isCaretakerMode) return
        val u = selfUid ?: return
        val prefs = context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
        if (prefs.getBoolean("backlog_$u", false)) return

        runCatching {
            ProfileManager(context).syncToCloud()
            ActivityDataManager(context).syncToCloud()
            MedicationManager(context).syncAllToCloud()
            AppointmentManager(context).syncAllToCloud()
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
    // Count + Sum are bumped with FieldValue.increment() — atomic on the server AND
    // offline-safe: the write queues in the on-device cache and flushes on reconnect
    // (a transaction, by contrast, needs a live server round-trip and is simply
    // dropped when offline — which is why the aggregate never appeared on the
    // emulator). Min/Max are a best-effort read-then-merge; a rare race only widens
    // the range slightly. A tampered client could still skew the numbers — acceptable
    // for the FYP; a Cloud Function would make it tamper-proof later.
    // ===================================================================

    private val METRIC_KEYS = listOf("hr", "sys", "sugar", "oxy")

    private val statsDoc get() = Firebase.firestore.collection("stats").document("vitals")
    // One doc per calendar day: stats/vitals/daily/{yyyy-MM-dd} — {k}Count / {k}Sum only.
    // Powers the Health Trends chart (each point = that day's global average).
    private fun dailyDoc(dayId: String) = statsDoc.collection("daily").document(dayId)

    private fun dayId(millis: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** Pull the in-range numeric vitals out of a reading, keyed for the aggregate. */
    fun vitalsOf(r: HealthDataManager.Reading): Map<String, Double> = buildMap {
        r.heartRateBpm?.takeIf { it in 20..250 }?.let { put("hr", it.toDouble()) }
        r.systolic?.takeIf { it in 70..250 }?.let { put("sys", it.toDouble()) }
        r.bloodSugarValue?.takeIf { it in 20f..600f }?.let { put("sugar", it.toDouble()) }
        r.oxygenValue?.takeIf { it in 50f..100f }?.let { put("oxy", it.toDouble()) }
    }

    private fun countSumIncrements(counts: Map<String, Long>, sums: Map<String, Double>): HashMap<String, Any> {
        val out = HashMap<String, Any>()
        for ((k, n) in counts) out["${k}Count"] = FieldValue.increment(n)
        for ((k, s) in sums) out["${k}Sum"] = FieldValue.increment(s)
        return out
    }

    /** Fold one recorded set of vitals into the shared community aggregate (all-time + today). */
    fun bumpCommunityStats(values: Map<String, Double>) {
        if (values.isEmpty() || uid == null) return

        val counts = values.mapValues { 1L }
        val allTime = countSumIncrements(counts, values)
        allTime["updatedAt"] = FieldValue.serverTimestamp()
        statsDoc.set(allTime, SetOptions.merge())
            .addOnFailureListener { e -> android.util.Log.w("Cloud", "community bump failed", e) }

        dailyDoc(dayId()).set(countSumIncrements(counts, values), SetOptions.merge())

        // Min/Max (all-time only): best-effort, non-transactional.
        statsDoc.get().addOnSuccessListener { snap ->
            val mm = HashMap<String, Any>()
            for ((k, v) in values) {
                val curMin = snap.getDouble("${k}Min")
                val curMax = snap.getDouble("${k}Max")
                if (curMin == null || v < curMin) mm["${k}Min"] = v
                if (curMax == null || v > curMax) mm["${k}Max"] = v
            }
            if (mm.isNotEmpty()) statsDoc.set(mm, SetOptions.merge())
        }
    }

    /**
     * One-time: fold this account's already-recorded local history into the community
     * aggregate (both the all-time totals and the per-day docs). Readings taken before this
     * feature shipped were never counted. Guarded by a per-account flag so it runs at most once.
     */
    fun seedCommunityBacklog(context: Context) {
        if (Session.isCaretakerMode) return
        val u = selfUid ?: return
        val prefs = context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
        if (prefs.getBoolean("community_seed_$u", false)) return

        runCatching {
            val history = HealthDataManager(context).history()
            if (history.isEmpty()) return@runCatching

            val allTime = HashMap<String, ArrayList<Double>>()
            val perDay = HashMap<String, HashMap<String, ArrayList<Double>>>()
            history.forEach { r ->
                val d = perDay.getOrPut(dayId(r.timestamp)) { HashMap() }
                vitalsOf(r).forEach { (k, v) ->
                    allTime.getOrPut(k) { ArrayList() }.add(v)
                    d.getOrPut(k) { ArrayList() }.add(v)
                }
            }
            if (allTime.isEmpty()) return@runCatching

            val out = countSumIncrements(
                allTime.mapValues { it.value.size.toLong() },
                allTime.mapValues { it.value.sum() },
            )
            out["updatedAt"] = FieldValue.serverTimestamp()
            statsDoc.set(out, SetOptions.merge())

            perDay.forEach { (d, metrics) ->
                dailyDoc(d).set(
                    countSumIncrements(
                        metrics.mapValues { it.value.size.toLong() },
                        metrics.mapValues { it.value.sum() },
                    ),
                    SetOptions.merge(),
                )
            }

            statsDoc.get().addOnSuccessListener { snap ->
                val mm = HashMap<String, Any>()
                for ((k, vs) in allTime) {
                    mm["${k}Min"] = minOf(vs.min(), snap.getDouble("${k}Min") ?: Double.MAX_VALUE)
                    mm["${k}Max"] = maxOf(vs.max(), snap.getDouble("${k}Max") ?: -Double.MAX_VALUE)
                }
                statsDoc.set(mm, SetOptions.merge())
            }
        }
        prefs.edit().putBoolean("community_seed_$u", true).apply()
    }

    data class CommunityStat(val avg: Double, val min: Double, val max: Double, val count: Long)

    private fun parseCommunity(snap: DocumentSnapshot): Map<String, CommunityStat> {
        fun stat(k: String): CommunityStat? {
            val count = snap.getDouble("${k}Count") ?: return null
            if (count < 1.0) return null
            val sum = snap.getDouble("${k}Sum") ?: return null
            return CommunityStat(
                avg = sum / count,
                min = snap.getDouble("${k}Min") ?: (sum / count),
                max = snap.getDouble("${k}Max") ?: (sum / count),
                count = count.toLong(),
            )
        }
        return buildMap { METRIC_KEYS.forEach { k -> stat(k)?.let { put(k, it) } } }
    }

    /** Read the all-time community aggregate once (SERVER, then CACHE). */
    suspend fun communityStats(): Map<String, CommunityStat> {
        if (uid == null) return emptyMap()
        val snap = runCatching { statsDoc.get(Source.SERVER).awaitResult() }.getOrNull()
            ?: runCatching { statsDoc.get(Source.CACHE).awaitResult() }.getOrNull()
            ?: return emptyMap()
        return parseCommunity(snap)
    }

    /** Live all-time community aggregate — emits now and on every server-side change. */
    fun communityStatsFlow(): Flow<Map<String, CommunityStat>> = callbackFlow {
        if (uid == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }
        val reg = statsDoc.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.w("Cloud", "community listener error", err)
                return@addSnapshotListener
            }
            trySend(if (snap != null && snap.exists()) parseCommunity(snap) else emptyMap())
        }
        awaitClose { reg.remove() }
    }

    /** One day's global average per metric. */
    data class CommunityDay(val dayId: String, val avg: Map<String, Double>)

    /**
     * Live per-day global averages for the calendar range [startDayId]..[endDayId] (inclusive,
     * ids are "yyyy-MM-dd" which sort chronologically). Powers the Health Trends chart.
     */
    fun communityDailyFlow(startDayId: String, endDayId: String): Flow<List<CommunityDay>> = callbackFlow {
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = statsDoc.collection("daily")
            .orderBy(FieldPath.documentId())
            .startAt(startDayId)
            .endAt(endDayId)
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                android.util.Log.w("Cloud", "community daily listener error", err)
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            trySend(
                snap.documents.map { d ->
                    val avg = HashMap<String, Double>()
                    METRIC_KEYS.forEach { k ->
                        val c = d.getDouble("${k}Count") ?: 0.0
                        val s = d.getDouble("${k}Sum") ?: 0.0
                        if (c >= 1.0) avg[k] = s / c
                    }
                    CommunityDay(d.id, avg)
                }
            )
        }
        awaitClose { reg.remove() }
    }

    fun dayIdFor(millis: Long): String = dayId(millis)
}

/**
 * Read a document, retrying the SERVER fetch a few times before falling back to the local
 * cache. Right after sign-in the Firestore SDK can fire its first request before it has
 * picked up the fresh auth token, so a lone `get()` comes back PERMISSION_DENIED (the rules
 * require `signedIn()`). The token propagates within ~1s — spaced retries ride over it.
 */
internal suspend fun DocumentReference.getFresh(attempts: Int = 4): DocumentSnapshot? {
    runCatching { get(Source.SERVER).awaitResult() }.getOrNull()?.let { return it }
    // A returning user has the doc cached — fast path, no waiting.
    runCatching { get(Source.CACHE).awaitResult() }.getOrNull()?.let { return it }
    // Nothing cached and the server read failed: most likely the post-sign-in token race.
    repeat(attempts) { i ->
        delay(250L + 250L * i)   // 250, 500, 750, 1000
        runCatching { get(Source.SERVER).awaitResult() }.getOrNull()?.let { return it }
    }
    return null
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
