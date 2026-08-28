package com.fyp.healthcare

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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
data class HealthProfile(
    val name: String = "",
    val bloodType: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val birthDate: String = "",     // ISO "yyyy-MM-dd"
    val sex: String = "",           // "" | "Male" | "Female" | "Other"
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

    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    /** Has the user completed the first-run setup? (name + birth date are the required minimum) */
    fun isOnboarded(): Boolean = get().let { it.name.isNotBlank() && it.birthDate.isNotBlank() }

    fun get(): HealthProfile = HealthProfile(
        name = prefs.getString(K_NAME, "").orEmpty(),
        bloodType = prefs.getString(K_BLOOD, "").orEmpty(),
        heightCm = prefs.getString(K_HEIGHT, "").orEmpty(),
        weightKg = prefs.getString(K_WEIGHT, "").orEmpty(),
        birthDate = prefs.getString(K_BIRTH, "").orEmpty(),
        sex = prefs.getString(K_SEX, "").orEmpty(),
    )

    fun save(p: HealthProfile) {
        prefs.edit()
            .putString(K_NAME, p.name.trim())
            .putString(K_BLOOD, p.bloodType.trim())
            .putString(K_HEIGHT, p.heightCm.trim())
            .putString(K_WEIGHT, p.weightKg.trim())
            .putString(K_BIRTH, p.birthDate.trim())
            .putString(K_SEX, p.sex.trim())
            .apply()
        syncToCloud(p)
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
    }
}

/** "Ahmad Rizal Hassan" -> "AR" */
fun profileInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
