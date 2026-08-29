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
 * A distinct icon per consumption route — used on the Medication Reminder cards and
 * the "How it's taken" selector so each style of medicine reads at a glance.
 */
val MedRoute.icon: ImageVector
    get() = when (this) {
        MedRoute.ORAL -> Icons.Filled.Medication          // pill bottle
        MedRoute.TOPICAL -> Icons.Filled.Sanitizer         // tube / pump
        MedRoute.INJECTABLE -> Icons.Filled.Vaccines       // syringe
        MedRoute.INHALED -> Icons.Filled.Air               // breath
        MedRoute.SUBLINGUAL -> Icons.Filled.Face           // under the tongue
        MedRoute.NASAL -> Icons.Filled.WaterDrop           // spray / mist
        MedRoute.RECTAL -> Icons.Filled.Science            // suppository formulation
        MedRoute.OPHTHALMIC -> Icons.Filled.Visibility     // eye / ear
        MedRoute.TRANSDERMAL -> Icons.Filled.Healing       // patch
        MedRoute.OTHER -> Icons.Filled.MedicalServices
    }
