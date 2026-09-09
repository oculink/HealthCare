package com.fyp.healthcare

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * "Choose Condition" - a thin wrapper over [CatalogPickerScreen] backed by
 * [ConditionCatalog] (bundled NHS Health A-Z subset  plus  online refresh  plus  the user's own
 * past additions). [onPick] fires with the chosen condition name; [alreadyPicked] are
 * names already on the profile.
 */
@Composable
fun ConditionPickerScreen(
    alreadyPicked: List<String>,
    onPick: (name: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    CatalogPickerScreen(
        title = "Choose Condition",
        searchHint = "Search conditions, e.g. asthma, diabetes",
        addNotListedTitle = "Add a condition not listed",
        addNotListedSubtitle = "Type your own — works offline, saved for next time",
        addFieldHint = "e.g. Costochondritis, Sarcoidosis",
        addButtonLabel = "Add this condition",
        rowIcon = Icons.Filled.MonitorHeart,
        alreadyPicked = alreadyPicked,
        search = { query ->
            ConditionCatalog.search(context, query).map {
                CatalogPickerItem(it.name, it.note.ifBlank { it.category.label }, it.section)
            }
        },
        refresh = { ConditionCatalogRemote.refresh(context) },
        commitCustom = { ConditionCatalog.addCustomCondition(context, it) },
        onPick = onPick,
        onBackClick = onBackClick,
    )
}
