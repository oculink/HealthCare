package com.fyp.healthcare

object VitalStatus {

    fun heartRate(bpm: Int): String = when {
        bpm < 40 || bpm > 130 -> "Critical"
        bpm < 60 -> "Low"
        bpm <= 90 -> "Good"
        bpm <= 100 -> "Normal"
        else -> "High"
    }

    fun bloodPressure(sys: Int, dia: Int): String = when {
        sys >= 180 || dia >= 120 || sys <= 70 || dia <= 40 -> "Critical"
        sys >= 130 || dia >= 85 -> "High"
        sys < 90 || dia < 60 -> "Low"
        sys >= 120 || dia >= 80 -> "Normal"
        else -> "Good"
    }

    fun oxygen(spo2: Int): String = when {
        spo2 < 90 -> "Critical"
        spo2 < 95 -> "Low"
        else -> "Good"
    }
}