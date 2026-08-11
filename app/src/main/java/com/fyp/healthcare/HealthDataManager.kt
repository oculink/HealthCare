package com.fyp.healthcare

import android.content.Context
import android.content.SharedPreferences

/**
 * FUTURE ROADMAP (HEALTH DATA):
 * - Record data will be uploaded to SQL automatically into the user's account in the future.
 * - This local storage then becomes the OFFLINE CACHE: when offline, the app keeps showing
 *   this local data (the existing null/placeholder state already handles "no data").
 * - Home dashboard reads the user's OWN latest readings from here;
 *   Health Trends will instead read AVERAGES of ALL users from SQL.
 */

class HealthDataManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("health_data", Context.MODE_PRIVATE)

    fun saveReadings(
        bloodPressure: String,
        bloodSugar: String,
        heartRate: String,
        temperature: String,
        oxygen: String,
        weight: String
    ) {
        prefs.edit()
            .putString("blood_pressure", bloodPressure)
            .putString("blood_sugar", bloodSugar)
            .putString("heart_rate", heartRate)
            .putString("temperature", temperature)
            .putString("oxygen", oxygen)
            .putString("weight", weight)
            .apply()
    }

    fun getBloodPressure(): String? = prefs.getString("blood_pressure", null)
    fun getBloodSugar(): String? = prefs.getString("blood_sugar", null)
    fun getHeartRate(): String? = prefs.getString("heart_rate", null)
    fun getTemperature(): String? = prefs.getString("temperature", null)
    fun getOxygen(): String? = prefs.getString("oxygen", null)
    fun getWeight(): String? = prefs.getString("weight", null)
}