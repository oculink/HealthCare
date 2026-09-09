package com.fyp.healthcare

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Sanitizer
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A distinct icon per consumption route - used on the Medication Reminder cards and
 * the "How it's taken" selector so each style of medicine reads at a glance.
 */
val MedRoute.icon: ImageVector
    get() = when (this) {
        MedRoute.ORAL -> Icons.Filled.Medication
        MedRoute.TOPICAL -> Icons.Filled.Sanitizer
        MedRoute.INJECTABLE -> Icons.Filled.Vaccines
        MedRoute.INHALED -> Icons.Filled.Air
        MedRoute.SUBLINGUAL -> Icons.Filled.Face
        MedRoute.NASAL -> Icons.Filled.WaterDrop
        MedRoute.RECTAL -> Icons.Filled.Science
        MedRoute.OPHTHALMIC -> Icons.Filled.Visibility
        MedRoute.TRANSDERMAL -> Icons.Filled.Healing
        MedRoute.OTHER -> Icons.Filled.MedicalServices
    }
