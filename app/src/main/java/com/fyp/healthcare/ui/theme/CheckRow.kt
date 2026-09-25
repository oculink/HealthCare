package com.fyp.healthcare.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A checkbox with its label, where the whole row is the tap target rather than just the small
 * box - the box alone is 20dp of a 48dp row, and asking an older person to hit it precisely is
 * asking for frustration.
 *
 * [tint] colours the box itself, so a tickbox for heart rate is green and one for blood pressure
 * is red: the list stays recognisable without reading every line.
 */
@Composable
fun CheckRow(
    label: String,
    tint: Color,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onChecked(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = tint, checkmarkColor = Color.White),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC)),
        )
    }
}

/** Adds or removes [value] from the list, so each box toggles independently of the others. */
fun List<String>.with(value: String, on: Boolean): List<String> =
    if (on) this + value else this - value
