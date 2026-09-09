package com.fyp.healthcare

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * "Choose Allergen" - a thin wrapper over [CatalogPickerScreen] backed by [AllergyCatalog]
 * (bundled list  plus  online refresh  plus  the user's own past additions). [onPick] fires with the
 * chosen allergen name; [alreadyPicked] are names already on the profile.
 */
@Composable
fun AllergyPickerScreen(
    alreadyPicked: List<String>,
    onPick: (name: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    CatalogPickerScreen(
        title = "Choose Allergen",
        searchHint = "Search foods, pollens, drugs…",
        addNotListedTitle = "Add an allergy not listed",
        addNotListedSubtitle = "Type your own — works offline, saved for next time",
        addFieldHint = "e.g. Kiwi, Amoxicillin, Cat dander",
        addButtonLabel = "Add this allergy",
        rowIcon = Icons.Filled.Warning,
        alreadyPicked = alreadyPicked,
        search = { query ->
            AllergyCatalog.search(context, query).map {
                CatalogPickerItem(it.name, it.note.ifBlank { it.category.label }, it.section)
            }
        },
        refresh = { AllergyCatalogRemote.refresh(context) },
        commitCustom = { AllergyCatalog.addCustomAllergy(context, it) },
        onPick = onPick,
        onBackClick = onBackClick,
    )
}
