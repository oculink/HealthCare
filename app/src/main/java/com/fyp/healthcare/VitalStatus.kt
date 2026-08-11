package com.fyp.healthcare

// Simplified adult average ranges.
// TODO: verify/cite these ranges with proper medical sources for your final report.
object VitalStatus {

    // Resting heart rate: average adult is 60-100 BPM
    fun heartRate(bpm: Int): String = when {
        bpm < 40 || bpm > 130 -> "Critical"
        bpm < 60 -> "Low"
        bpm <= 90 -> "Good"        // optimal zone
        bpm <= 100 -> "Normal"     // acceptable zone
        else -> "High"
    }

    // Blood pressure: average adult is around 120/80
    fun bloodPressure(sys: Int, dia: Int): String = when {
        sys >= 180 || dia >= 120 || sys <= 70 || dia <= 40 -> "Critical"
        sys >= 130 || dia >= 85 -> "High"
        sys < 90 || dia < 60 -> "Low"
        sys >= 120 || dia >= 80 -> "Normal"
        else -> "Good"
    }

    // Oxygen (SpO2): average adult is 95-100%
    fun oxygen(spo2: Int): String = when {
        spo2 < 90 -> "Critical"
        spo2 < 95 -> "Low"
        else -> "Good"
    }
}