package com.fyp.healthcare

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * The user's own health / personal profile.
 *
 * Entered by the user (onboarding + the Edit Profile screen). Not radar data.
 * The Emergency Profile screen will later read the same values.
 *
 * TODO (FUTURE - SQL): sync to the account so the caregiver can see it too.
 */
/** One person to call in an emergency. Shown on the Emergency Profile screen. */
data class EmergencyContact(
    val name: String = "",
    val relation: String = "",   // "Wife", "Son", "Doctor", ...
    val phone: String = "",
) {
    val isBlank: Boolean get() = name.isBlank() && phone.isBlank()
}

data class HealthProfile(
    val name: String = "",
    val bloodType: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val birthDate: String = "",     // ISO "yyyy-MM-dd"
    val sex: String = "",           // "" | "Male" | "Female" | "Other"
    val allergies: List<String> = emptyList(),       // e.g. ["Penicillin", "Seafood"]
    val conditions: List<String> = emptyList(),      // e.g. ["Diabetic", "Hypertension"]
    val emergencyContacts: List<EmergencyContact> = emptyList(),
) {
    /** Age in whole years, or null if the birth date isn't set / valid. */
    val age: Int?
        get() {
            val p = birthDate.split("-")
            if (p.size != 3) return null
            val y = p[0].toIntOrNull() ?: return null
            val m = p[1].toIntOrNull() ?: return null
            val d = p[2].toIntOrNull() ?: return null
            val now = Calendar.getInstance()
            var age = now.get(Calendar.YEAR) - y
            val curM = now.get(Calendar.MONTH) + 1
            val curD = now.get(Calendar.DAY_OF_MONTH)
            if (curM < m || (curM == m && curD < d)) age--
            return age.takeIf { it in 0..130 }
        }

    /** true when the user hasn't filled in any of the health fields yet */
    val isBlank: Boolean
        get() = listOf(bloodType, heightCm, weightKg, birthDate, sex).all { it.isBlank() }
}

/** "1958-03-12" -> "12 Mar 1958"; "" -> "" */
fun formatBirthDate(iso: String): String {
    val p = iso.split("-")
    if (p.size != 3) return ""
    return runCatching {
        val cal = Calendar.getInstance().apply {
            set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
        }
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(cal.time)
    }.getOrDefault("")
}

class ProfileManager(context: Context) {

    private val prefs = context.getSharedPreferences(scopedPrefsName("profile"), Context.MODE_PRIVATE)

    /** Has the user completed the first-run setup? (name + birth date are the required minimum) */
    fun isOnboarded(): Boolean = get().let { it.name.isNotBlank() && it.birthDate.isNotBlank() }

    fun get(): HealthProfile = HealthProfile(
        name = prefs.getString(K_NAME, "").orEmpty(),
        bloodType = prefs.getString(K_BLOOD, "").orEmpty(),
        heightCm = prefs.getString(K_HEIGHT, "").orEmpty(),
        weightKg = prefs.getString(K_WEIGHT, "").orEmpty(),
        birthDate = prefs.getString(K_BIRTH, "").orEmpty(),
        sex = prefs.getString(K_SEX, "").orEmpty(),
        allergies = splitTags(prefs.getString(K_ALLERGIES, "").orEmpty()),
        conditions = splitTags(prefs.getString(K_CONDITIONS, "").orEmpty()),
        emergencyContacts = parseContacts(prefs.getString(K_CONTACTS, "").orEmpty()),
    )

    fun save(p: HealthProfile) {
        val contacts = p.emergencyContacts
            .map { EmergencyContact(it.name.trim(), it.relation.trim(), it.phone.trim()) }
            .filterNot { it.isBlank }
        val clean = p.copy(emergencyContacts = contacts)
        writeLocal(clean)
        syncToCloud(clean)
    }

    /** Overwrite the local profile from a cloud copy (hydration). No re-push. */
    fun hydrateLocal(p: HealthProfile) = writeLocal(p)

    private fun writeLocal(p: HealthProfile) {
        prefs.edit()
            .putString(K_NAME, p.name.trim())
            .putString(K_BLOOD, p.bloodType.trim())
            .putString(K_HEIGHT, p.heightCm.trim())
            .putString(K_WEIGHT, p.weightKg.trim())
            .putString(K_BIRTH, p.birthDate.trim())
            .putString(K_SEX, p.sex.trim())
            .putString(K_ALLERGIES, joinTags(p.allergies))
            .putString(K_CONDITIONS, joinTags(p.conditions))
            .putString(K_CONTACTS, serializeContacts(p.emergencyContacts))
            .apply()
    }

    /** Mirror the profile fields onto users/{uid}. */
    fun syncToCloud(p: HealthProfile = get()) {
        Cloud.userDoc?.set(
            mapOf(
                "name" to p.name.trim(),
                "birthDate" to p.birthDate.trim(),
                "sex" to p.sex.trim(),
                "heightCm" to p.heightCm.trim(),
                "weightKg" to p.weightKg.trim(),
                "bloodType" to p.bloodType.trim(),
                "allergies" to p.allergies,
                "conditions" to p.conditions,
                "emergencyContacts" to p.emergencyContacts.map {
                    mapOf("name" to it.name, "relation" to it.relation, "phone" to it.phone)
                },
                "profileUpdatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        )
    }

    private companion object {
        const val K_NAME = "name"
        const val K_BLOOD = "blood_type"
        const val K_HEIGHT = "height_cm"
        const val K_WEIGHT = "weight_kg"
        const val K_BIRTH = "birth_date"
        const val K_SEX = "sex"
        const val K_ALLERGIES = "allergies"
        const val K_CONDITIONS = "conditions"
        const val K_CONTACTS = "emergency_contacts_v1"
    }
}

// ----- tag lists (allergies / conditions) are stored as one comma-separated string -----

fun splitTags(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

fun joinTags(tags: List<String>): String =
    tags.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

// ----- emergency contacts are stored as a small JSON array -----

private fun serializeContacts(contacts: List<EmergencyContact>): String {
    val arr = JSONArray()
    contacts.forEach { c ->
        arr.put(JSONObject().apply {
            put("name", c.name)
            put("relation", c.relation)
            put("phone", c.phone)
        })
    }
    return arr.toString()
}

private fun parseContacts(raw: String): List<EmergencyContact> = runCatching {
    val arr = JSONArray(raw.ifBlank { "[]" })
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        EmergencyContact(
            name = o.optString("name"),
            relation = o.optString("relation"),
            phone = o.optString("phone"),
        )
    }
}.getOrDefault(emptyList())

/** "Ahmad Rizal Hassan" -> "AR" */
fun profileInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
