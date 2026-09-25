package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.AppIconBadge
import com.fyp.healthcare.ui.theme.CheckRow
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyBadge
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.themed
import com.fyp.healthcare.ui.theme.vitalStatusColor
import com.fyp.healthcare.ui.theme.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val Hairline: Color @Composable get() = themed(Color(0xFFE5E8EE), Color(0xFF2A2C33))
private val SoftWell: Color @Composable get() = themed(Color(0xFFF3F5F9), Color(0xFF232429))

// The same accent each vital uses on Home, so a heart-rate row is recognisably the heart-rate
// colour in both places.
private val HeartTint = Color(0xFF2E9E6B)
private val PressureTint = Color(0xFFD32F2F)
private val OxygenTint = Color(0xFF2A6DE1)
private val SugarTint = Color(0xFFEE7B2E)
private val TempTint = Color(0xFF6C5CE7)

/** Days shown before the pager turns. Ten is a full screen's worth on a phone. */
private const val DAYS_PER_PAGE = 10

/** The vitals that can be filtered to, each with the accent it uses on this screen and on Home. */
private val VitalChoices = listOf(
    "Heart rate" to HeartTint,
    "Blood pressure" to PressureTint,
    "Oxygen" to OxygenTint,
)

/** In severity order, so the boxes read Good through to Critical rather than alphabetically. */
private val StatusWords = listOf("Good", "Normal", "Low", "High", "Critical")

/**
 * Every reading the person has recorded, newest first, grouped by day and paged ten days at a
 * time. The page controls stay pinned to the bottom of the screen rather than sitting after the
 * last card, so turning the page never means scrolling to find them.
 *
 * The filter at the top right narrows the list to one vital, one status, or one day - or any
 * combination. Status choices are read from the person's own data rather than hard-coded, so a
 * filter that would return nothing is never offered.
 *
 * Values are shown as they were typed rather than reformatted from the parsed number, so a
 * reading of "96.5" never arrives as "96" on the way past a status calculation. The status word
 * appears only where the app has a range to judge against - heart rate, blood pressure, oxygen -
 * and sugar and temperature are left unlabelled rather than given a verdict they have no
 * threshold for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHistoryScreen(
    healthData: HealthDataManager,
    onBackClick: () -> Unit,
) {
    val dataVersion = Session.dataVersion
    val readings = remember(dataVersion) { healthData.history().sortedByDescending { it.timestamp } }

    var filter by remember { mutableStateOf(HistoryFilter()) }
    var page by remember { mutableStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }

    // Clears the narrowing and starts back at the newest page, so "Show everything" never
    // leaves the person stranded on page 4 of a list that has just grown.
    val clearFilter = {
        filter = HistoryFilter()
        page = 0
    }

    val days = remember(readings, filter) {
        readings.filter { filter.matches(it) }
            .groupBy { dayKey(it.timestamp) }
            .toList()
            .sortedByDescending { (_, list) -> list.maxOf { it.timestamp } }
    }
    val pageCount = max(1, (days.size + DAYS_PER_PAGE - 1) / DAYS_PER_PAGE)
    val currentPage = page.coerceIn(0, pageCount - 1)
    val visibleDays = days.drop(currentPage * DAYS_PER_PAGE).take(DAYS_PER_PAGE)
    val shownReadings = visibleDays.sumOf { it.second.size }
    val keptVitals = filter.vitals.takeIf { it.isNotEmpty() }?.toSet()

    // Turning the page or changing the filter starts the list at the top again, otherwise you
    // land somewhere in the middle of the new page.
    val listScroll = rememberScrollState()
    LaunchedEffect(currentPage, filter) { listScroll.scrollTo(0) }

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
                "Reading History",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showFilters = true }) {
                Icon(
                    Icons.Filled.FilterAlt,
                    contentDescription = if (filter.active) "Filters on: ${filter.summary()}" else "Filter readings",
                    tint = if (filter.active) Color.White else Color.White.copy(alpha = 0.65f),
                )
            }
        }

        if (filter.active) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftWell)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    filter.summary(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = clearFilter) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (readings.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing recorded yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Readings you enter on the Record Data page will appear here, newest first.",
                    fontSize = 13.sp,
                    color = LabelGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                )
            }
        } else if (days.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No readings match this", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Try a different day or status.",
                    fontSize = 13.sp,
                    color = LabelGray,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                GlossyButton(onClick = clearFilter, color = BrandBlue) {
                    Text("Show everything")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(listScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "$shownReadings reading${if (shownReadings == 1) "" else "s"} shown · " +
                        "${days.size} day${if (days.size == 1) "" else "s"} matched" +
                        if (pageCount > 1) " · page ${currentPage + 1} of $pageCount" else "",
                    fontSize = 13.sp,
                    color = LabelGray,
                )

                visibleDays.forEach { (key, list) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            dayLabel(key),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                        )
                        list.forEachIndexed { i, reading ->
                            if (i > 0) {
                                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                            }
                            ReadingBlock(reading, keptVitals)
                        }
                    }
                }
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardWhite)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlossyButton(
                        onClick = { page = (currentPage - 1).coerceAtLeast(0) },
                        modifier = Modifier.weight(1f),
                        color = BrandBlue,
                        enabled = currentPage > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text("Newer")
                    }
                    Text(
                        "${currentPage + 1} / $pageCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(52.dp),
                    )
                    GlossyButton(
                        onClick = { page = (currentPage + 1).coerceAtMost(pageCount - 1) },
                        modifier = Modifier.weight(1f),
                        color = BrandBlue,
                        enabled = currentPage < pageCount - 1,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        Text("Older")
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardWhite,
        ) {
            FilterSheet(
                readings = readings,
                filter = filter,
                onFilter = { filter = it; page = 0 },
                onPickDate = { pickingDate = true },
            )
        }
    }

    if (pickingDate) {
        // Preselects the day already filtered to, so reopening the picker shows where you are
        // rather than starting blank and letting someone pick a second day by accident.
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = filter.day?.let { dayToUtcMillis(it) },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { filter = filter.copy(day = utcDayKey(it)) }
                    page = 0
                    pickingDate = false
                }) { Text("Use this day", color = BrandBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text("Cancel", color = LabelGray) }
            },
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }
}

@Composable
private fun FilterSheet(
    readings: List<HealthDataManager.Reading>,
    filter: HistoryFilter,
    onFilter: (HistoryFilter) -> Unit,
    onPickDate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Filter readings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)

        Text("Show only these", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LabelGray)
        Text(
            "Leave them all unticked to see every measurement.",
            fontSize = 12.sp,
            color = LabelGray,
            lineHeight = 16.sp,
        )
        VitalChoices.forEach { (label, tint) ->
            CheckRow(label, tint, label in filter.vitals) { on ->
                onFilter(filter.copy(vitals = filter.vitals.with(label, on)))
            }
        }

        val kept = filter.vitals.takeIf { it.isNotEmpty() }?.toSet()
        val statuses = remember(readings, filter.vitals) {
            val present = readings.flatMap { statusesOf(it, kept) }.toSet()
            StatusWords.filter { it in present }
        }
        if (statuses.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Only when it is", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LabelGray)
            Text(
                "Leave them all unticked for any condition.",
                fontSize = 12.sp,
                color = LabelGray,
                lineHeight = 16.sp,
            )
            statuses.forEach { s ->
                CheckRow(s, vitalStatusColor(s), s in filter.statuses) { on ->
                    onFilter(filter.copy(statuses = filter.statuses.with(s, on)))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("On one day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LabelGray)
        GlossyButton(
            onClick = onPickDate,
            modifier = Modifier.fillMaxWidth(),
            color = BrandBlue,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(filter.day?.let { "Reading on ${formatDay(it)}" } ?: "Choose a day")
        }
        if (filter.day != null) {
            TextButton(onClick = { onFilter(filter.copy(day = null)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Any day", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (filter.active) {
            Spacer(Modifier.height(6.dp))
            GlossyButton(
                onClick = { onFilter(HistoryFilter()) },
                modifier = Modifier.fillMaxWidth(),
                color = LabelGray,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text("Show everything again")
            }
        }
    }
}

@Composable
private fun ReadingBlock(reading: HealthDataManager.Reading, onlyVitals: Set<String>?) {
    val lines = metricLines(reading, onlyVitals)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            formatTime(reading.timestamp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LabelGray,
        )
        if (lines.isEmpty()) {
            Text("Saved with no values", fontSize = 13.sp, color = LabelGray)
        }
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconBadge(line.icon, line.tint, size = 26.dp, iconSize = 15.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    line.label,
                    fontSize = 13.sp,
                    color = LabelGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    line.value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                )
                // A dash stands in where there is no verdict, so the values in the rows above
                // and below still line up in one column instead of stepping in and out.
                Spacer(Modifier.width(8.dp))
                val status = line.status
                if (status != null) {
                    val c = vitalStatusColor(status)
                    Text(
                        status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = c,
                        modifier = Modifier
                            .glossyBadge(c, RoundedCornerShape(50.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                } else {
                    Text("—", fontSize = 10.sp, color = LabelGray)
                }
            }
        }
    }
}

/** One displayed measurement, with its status word when the app has a range to judge it by. */
private class MetricLine(
    val icon: ImageVector,
    val tint: Color,
    val label: String,
    val value: String,
    val status: String?,
)

/**
 * Heart rate, blood pressure and oxygen carry no unit here: the label already names them, those
 * three have only one unit each anywhere in medicine, and dropping them is what keeps the widest
 * row ("Blood pressure  184/126  Critical") inside a 320dp phone without the label wrapping.
 * Sugar and temperature keep theirs, because mg/dL-vs-mmol/L and °C-vs-°F are real differences
 * between the devices people come from.
 */
private fun metricLines(r: HealthDataManager.Reading, onlyVitals: Set<String>?): List<MetricLine> {
    val out = ArrayList<MetricLine>(5)
    fun wanted(label: String) = onlyVitals == null || onlyVitals.contains(label)

    if (wanted("Heart rate")) {
        r.heartRate.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            out.add(
                MetricLine(Icons.Filled.MonitorHeart, HeartTint, "Heart rate", raw,
                    raw.toIntOrNull()?.let { VitalStatus.heartRate(it) })
            )
        }
    }
    if (wanted("Blood pressure")) {
        r.bloodPressure.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            val sys = r.systolic
            val dia = r.diastolic
            out.add(
                MetricLine(Icons.Filled.Bloodtype, PressureTint, "Blood pressure", raw,
                    if (sys != null && dia != null) VitalStatus.bloodPressure(sys, dia) else null)
            )
        }
    }
    if (wanted("Oxygen")) {
        r.oxygen.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            out.add(
                MetricLine(Icons.Filled.Air, OxygenTint, "Oxygen", raw,
                    raw.toFloatOrNull()?.toInt()?.let { VitalStatus.oxygen(it) })
            )
        }
    }
    if (onlyVitals == null) {
        r.bloodSugar.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            out.add(MetricLine(Icons.Filled.Cookie, SugarTint, "Blood sugar", "$raw mg/dL", null))
        }
        r.temperature.trim().takeIf { it.isNotEmpty() }?.let { raw ->
            out.add(MetricLine(Icons.Filled.DeviceThermostat, TempTint, "Temperature", "$raw °C", null))
        }
    }
    return out
}

/** The statuses this reading's judged vitals currently carry - used to offer only real filters. */
private fun statusesOf(r: HealthDataManager.Reading, onlyVitals: Set<String>?): List<String> =
    metricLines(r, onlyVitals).mapNotNull { it.status }

/**
 * What the person has narrowed the list to. Each group is a set of checkboxes, and an empty set
 * means "any" rather than "nothing" - unticking everything is how you get back to the full list,
 * so no combination of boxes can produce an empty screen by accident.
 *
 * The two groups do different jobs, which is worth knowing when reading [matches]: the vitals
 * decide which rows a day shows, while the conditions decide which readings qualify at all.
 */
private data class HistoryFilter(
    val vitals: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val day: String? = null,
) {
    val active: Boolean get() = vitals.isNotEmpty() || statuses.isNotEmpty() || day != null

    /** The vitals to keep rows for, or null to show every measurement the reading holds. */
    private fun keep(): Set<String>? = vitals.takeIf { it.isNotEmpty() }?.toSet()

    fun matches(r: HealthDataManager.Reading): Boolean {
        if (day != null && dayKey(r.timestamp) != day) return false
        val kept = metricLines(r, keep())
        if (vitals.isNotEmpty() && kept.isEmpty()) return false
        if (statuses.isNotEmpty() && kept.mapNotNull { it.status }.none { it in statuses }) return false
        return true
    }

    fun summary(): String {
        val what = if (vitals.isEmpty()) "readings" else "${vitals.joinToString(", ")} readings"
        val cond = if (statuses.isEmpty()) null else "that are " + statuses.joinToString(" or ").lowercase()
        return "Showing $what" + (cond?.let { " $it" } ?: "") +
            (day?.let { " on ${formatDay(it)}" } ?: "")
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun dayFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun dayKey(millis: Long): String = dayFormat().format(Date(millis))

/**
 * A [DatePicker] hands back midnight in UTC, not midnight where the person is, so reading it as
 * a local date would land them on the day before west of Greenwich.
 */
private fun utcDayKey(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

/** The inverse of [utcDayKey]: a local calendar day back to the midnight-UTC instant the picker wants. */
private fun dayToUtcMillis(key: String): Long? =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(key)?.time

/** Today and Yesterday by name; anything older by weekday and date. */
private fun dayLabel(key: String): String {
    val now = System.currentTimeMillis()
    return when (key) {
        dayKey(now) -> "Today"
        dayKey(now - DAY_MS) -> "Yesterday"
        else -> dayFormat().parse(key)?.let {
            SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(it)
        } ?: key
    }
}

private fun formatDay(key: String): String =
    dayFormat().parse(key)?.let {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(it)
    } ?: key

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
