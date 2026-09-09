package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.AppIconBadge
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossyFieldColors
import com.fyp.healthcare.ui.theme.glossyFieldWell
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val PlaceholderGray: Color @Composable get() = themed(Color(0xFFA6ACB8), Color(0xFF6A7079))
private val HeaderGray: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))

/** One row the [CatalogPickerScreen] can show. [section] is the "A".."Z"/"#" bucket. */
data class CatalogPickerItem(val name: String, val subtitle: String, val section: String)

private sealed interface CatalogRow {
    data class Section(val letter: String) : CatalogRow
    data class Item(val entry: CatalogPickerItem) : CatalogRow
}

/**
 * A generic searchable A-Z picker: search box, Contacts-style A-Z fast-scroll index with a
 * letter bubble, an always-available "add something not listed" inline field (works
 * offline), and rows that grey out when already chosen. Backs the medication / allergy /
 * condition pickers - see [AllergyPickerScreen], [ConditionPickerScreen].
 *
 * @param search       returns the entries matching a query ("" = everything), already sorted.
 * @param refresh      best-effort online catalogue refresh; returns true when the list changed.
 * @param commitCustom persist a user-typed name so it's remembered next time.
 * @param onPick       fires with the chosen (or newly typed) name.
 */
@Composable
fun CatalogPickerScreen(
    title: String,
    searchHint: String,
    addNotListedTitle: String,
    addNotListedSubtitle: String,
    addFieldHint: String,
    addButtonLabel: String,
    rowIcon: ImageVector,
    alreadyPicked: List<String>,
    search: (query: String) -> List<CatalogPickerItem>,
    refresh: suspend () -> Boolean,
    commitCustom: (String) -> Unit,
    onPick: (name: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var catalogVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (refresh()) catalogVersion++ }

    var query by remember { mutableStateOf("") }
    val results = remember(query, catalogVersion) { search(query) }
    val pickedSet = remember(alreadyPicked) { alreadyPicked.map { it.lowercase() }.toSet() }

    val listState = rememberLazyListState()
    val leadingItems = 1

    val rows = remember(results) {
        buildList {
            results.map { it.section }.distinct().forEach { letter ->
                val inSection = results.filter { it.section == letter }
                if (inSection.isNotEmpty()) {
                    add(CatalogRow.Section(letter))
                    inSection.forEach { add(CatalogRow.Item(it)) }
                }
            }
        }
    }
    val indexOfLetter = remember(rows) {
        buildMap {
            rows.forEachIndexed { i, row ->
                if (row is CatalogRow.Section) put(row.letter, i + leadingItems)
            }
        }
    }
    val letters = remember(rows) { rows.filterIsInstance<CatalogRow.Section>().map { it.letter } }

    val currentLetter by remember(rows) {
        derivedStateOf {
            val rowIdx = listState.firstVisibleItemIndex - leadingItems
            when {
                letters.isEmpty() -> null
                rowIdx < 0 -> letters.first()
                else -> when (val row = rows.getOrNull(rowIdx.coerceIn(0, rows.lastIndex))) {
                    is CatalogRow.Section -> row.letter
                    is CatalogRow.Item -> row.entry.section
                    else -> letters.first()
                }
            }
        }
    }

    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var barActive by remember { mutableStateOf(false) }
    var pickedLetter by remember { mutableStateOf<String?>(null) }

    var bubbleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(barActive, listState.isScrollInProgress) {
        if (barActive || listState.isScrollInProgress) {
            bubbleVisible = true
        } else {
            delay(650)
            bubbleVisible = false
        }
    }
    val bubbleLetter = pickedLetter ?: currentLetter

    fun jumpTo(y: Float) {
        if (letters.isEmpty() || barHeightPx <= 0f) return
        val idx = ((y / barHeightPx) * letters.size).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[idx]
        pickedLetter = letter
        indexOfLetter[letter]?.let { target -> scope.launch { listState.scrollToItem(target) } }
    }

    var addOpen by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    fun commit() {
        val name = custom.trim()
        if (name.isEmpty()) return
        commitCustom(name)
        onPick(name)
    }

    Column(modifier = Modifier.fillMaxSize().appBackground()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(BrandBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .glossyFieldWell(RoundedCornerShape(14.dp)),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = TextDark),
            placeholder = { Text(searchHint, color = PlaceholderGray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = LabelGray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = LabelGray)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(14.dp),
            colors = glossyFieldColors(accent = BrandBlue),
        )

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(end = 22.dp, bottom = 24.dp),
            ) {
                item(key = "__manual__") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandBlue.copy(alpha = 0.10f))
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                addOpen = !addOpen
                                if (addOpen && custom.isBlank()) custom = query.trim()
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIconBadge(Icons.Filled.Add, BrandBlue, size = 36.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(addNotListedTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                                Text(addNotListedSubtitle, fontSize = 12.sp, color = LabelGray)
                            }
                        }
                        AnimatedVisibility(visible = addOpen) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = custom,
                                    onValueChange = { custom = it },
                                    modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(12.dp)),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = TextDark),
                                    placeholder = { Text(addFieldHint, color = PlaceholderGray) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = glossyFieldColors(accent = BrandBlue),
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (custom.isBlank()) BrandBlue.copy(alpha = 0.25f) else BrandBlue)
                                        .clickable(enabled = custom.isNotBlank()) { commit() }
                                        .padding(vertical = 11.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(addButtonLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (rows.isEmpty()) {
                    item(key = "__empty__") {
                        Text(
                            "No match for \"$query\" — use \"$addNotListedTitle\" above.",
                            fontSize = 13.sp,
                            color = LabelGray,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                itemsIndexed(
                    rows,
                    key = { _, row -> if (row is CatalogRow.Section) "h_${row.letter}" else (row as CatalogRow.Item).entry.name },
                ) { _, row ->
                    when (row) {
                        is CatalogRow.Section -> Text(
                            row.letter,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabelGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HeaderGray)
                                .padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
                        )
                        is CatalogRow.Item -> {
                            val taken = row.entry.name.lowercase() in pickedSet
                            EntryRow(row.entry, rowIcon, taken) {
                                if (!taken) onPick(row.entry.name)
                            }
                        }
                    }
                }
            }

            if (letters.size > 1) {
                val barBg by animateColorAsState(
                    if (barActive) BrandBlue.copy(alpha = 0.10f) else Color.Transparent,
                    label = "barBg",
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .wrapContentHeight()
                        .padding(end = 2.dp)
                        .width(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(barBg)
                        .onSizeChanged { barHeightPx = it.height.toFloat() }
                        .pointerInput(letters, barHeightPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                barActive = true
                                jumpTo(down.position.y)
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            jumpTo(change.position.y)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                                barActive = false
                                pickedLetter = null
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    letters.forEach { letter ->
                        val on = letter == (pickedLetter ?: currentLetter)
                        val letterScale by animateFloatAsState(
                            targetValue = if (on) 1.7f else 1f,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
                            label = "letterScale",
                        )
                        val letterColor by animateColorAsState(
                            if (on) BrandBlue else LabelGray,
                            label = "letterColor",
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().height(17.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                letter,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = letterColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.scale(letterScale),
                            )
                        }
                    }
                }
            }

            val bubbleAlpha by animateFloatAsState(
                targetValue = if (bubbleVisible && bubbleLetter != null) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                label = "bubbleAlpha",
            )
            if (bubbleAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = (30 + 24 * (1f - bubbleAlpha)).dp)
                        .alpha(bubbleAlpha)
                        .scale(0.7f + 0.3f * bubbleAlpha)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BrandBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = bubbleLetter.orEmpty(),
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.5f)) togetherWith
                                (fadeOut() + scaleOut(targetScale = 0.5f))
                        },
                        label = "bubbleLetter",
                    ) { shown ->
                        Text(shown, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: CatalogPickerItem, icon: ImageVector, taken: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glossySurface(RoundedCornerShape(12.dp), CardWhite)
            .clickable(enabled = !taken, onClick = onClick)
            .padding(14.dp)
            .then(if (taken) Modifier.alpha(0.55f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            if (entry.subtitle.isNotBlank()) {
                Text(entry.subtitle, fontSize = 12.sp, color = LabelGray)
            }
        }
        if (taken) {
            Icon(Icons.Filled.Check, contentDescription = "Already added", tint = LabelGray, modifier = Modifier.size(18.dp))
        }
    }
}
