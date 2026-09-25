package com.fyp.healthcare

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.healthcare.ui.theme.CheckRow
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.themed
import com.fyp.healthcare.ui.theme.with
import java.util.Calendar

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val SoftWell: Color @Composable get() = themed(Color(0xFFF3F5F9), Color(0xFF232429))

// Matches the Emotional Support tile on Home, so the page and its door agree.
private val SupportRose = Color(0xFFB1537B)

/**
 * A quiet page: one warm photograph at a time with a short kind line under it. It never asks the
 * person how they are feeling and keeps no score - it just offers something pleasant to look at,
 * and a way to get another.
 *
 * Which picture opens is chosen from the date rather than randomly per visit, so the page reads as
 * "today's picture" and turns over tomorrow by itself. Only the buttons move it from there, and
 * the line under the photo is derived from the same index so the two always agree.
 *
 * The settings button picks which kinds of picture appear, as tickboxes. Leaving every box empty
 * is the no-preference state and shows all of them, so there is no way to tick yourself into an
 * empty page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionalSupportScreen(
    onBackClick: () -> Unit,
    onEmergencyClick: () -> Unit,
) {
    val context = LocalContext.current
    val day = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }

    var groups by remember { mutableStateOf(SupportPrefs.groups(context)) }
    // -1 means "today's picture"; anything else is a position the person tapped to.
    var pick by remember { mutableStateOf(-1) }
    var showChoices by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }

    val pool = remember(groups) {
        if (groups.isEmpty()) SupportPhotos else SupportPhotos.filter { it.group in groups }
    }
    val wanted = if (pick >= 0) pick else day
    val index = ((wanted % pool.size) + pool.size) % pool.size
    val photo = pool[index]
    val line = SupportLines[((index * 7 + day) % SupportLines.size + SupportLines.size) % SupportLines.size]

    fun choose(next: List<String>) {
        groups = next
        pick = -1
        SupportPrefs.save(context, next)
    }

    Column(modifier = Modifier.fillMaxSize().appBackground()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Emotional Support",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showChoices = true }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Choose which pictures to see",
                    tint = if (groups.isEmpty()) Color.White.copy(alpha = 0.65f) else Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glossySurface(RoundedCornerShape(22.dp), CardWhite)
                    .padding(12.dp),
            ) {
                Crossfade(targetState = photo, animationSpec = tween(450), label = "supportPhoto") { shown ->
                    Image(
                        // Deliberately decorative: the caption below says exactly this, and
                        // labelling both would have TalkBack read the sentence twice.
                        contentDescription = null,
                        painter = painterResource(id = shown.resId),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    line,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    photo.alt,
                    fontSize = 12.sp,
                    color = LabelGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))

                GlossyButton(
                    onClick = {
                        val base = if (pick >= 0) pick else day
                        // At least one step forward, so a tap always visibly changes the picture.
                        pick = base + 1 + (Math.random() * (pool.size - 1)).toInt()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = SupportRose,
                    enabled = pool.size > 1,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Show me another")
                }
            }

            Text(
                "Photos and the people who shared them",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = BrandBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showCredits = true }
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SoftWell)
                    .padding(14.dp),
            ) {
                Text(
                    "This page is a small lift, not counselling. If things ever feel unsafe or too " +
                        "heavy, the Emergency page keeps the numbers and details that help.",
                    fontSize = 12.sp,
                    color = LabelGray,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Open Emergency page",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmergencyClick() }
                        .padding(vertical = 6.dp),
                )
            }
        }
    }

    if (showChoices) {
        ModalBottomSheet(
            onDismissRequest = { showChoices = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardWhite,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Pictures you would like to see", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    "Tick the kinds you like seeing. Leave them all empty to see every kind.",
                    fontSize = 12.sp,
                    color = LabelGray,
                    lineHeight = 16.sp,
                )
                SupportCategories.forEach { category ->
                    CheckRow(
                        label = category.label,
                        tint = category.tint,
                        checked = category.key in groups,
                        onChecked = { on -> choose(groups.with(category.key, on)) },
                    )
                }
                if (groups.isNotEmpty()) {
                    TextButton(onClick = { choose(emptyList()) }) {
                        Text("Show every kind", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showCredits) {
        AlertDialog(
            onDismissRequest = { showCredits = false },
            containerColor = CardWhite,
            title = { Text("Photo credits", color = TextDark, fontSize = 17.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SupportPhotos.forEach {
                        Text(it.alt, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Text(it.credit, fontSize = 11.sp, color = LabelGray)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCredits = false }) { Text("Close", color = BrandBlue) }
            },
        )
    }
}

/** Remembers which kinds of picture the person prefers, as a comma-separated list. Same store the
 * theme choices live in. An empty list means "no preference", which the screen reads as "all". */
private object SupportPrefs {

    fun groups(context: Context): List<String> =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun save(context: Context, groups: List<String>) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putString(KEY, groups.joinToString(",")).apply()
    }

    private const val KEY = "support_groups"
}
