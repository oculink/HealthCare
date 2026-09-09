package com.fyp.healthcare

import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.glossyChip
import com.fyp.healthcare.ui.theme.glossyBadge
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fyp.healthcare.ui.theme.themed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Nearby Clinics: an in-app map of hospitals, clinics and pharmacies near the user.
 *
 * Map: an embedded WebView running Leaflet (from cdnjs) over free OpenStreetMap tiles.
 * Places: [NearbyClinics] (Overpass API). No Google Maps SDK, no API key, no billing.
 * Location: the framework [LocationManager] (last-known fix, else one fresh update).
 *
 * Reached from the Home "Nearby Clinics" quick action. Turn-by-turn navigation is handed
 * off to whatever maps app the phone has via a geo: intent.
 */

private val BrandBlue = Color(0xFF2A6DE1)
private val CardWhite: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val ScreenBg: Color @Composable get() = themed(Color(0xFFEFF1F6), Color(0xFF121316))
private val TextDark: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))

private fun kindColor(kind: String) = when (kind) {
    "hospital" -> Color(0xFFD32F2F)
    "pharmacy" -> Color(0xFF2E9E6B)
    "doctors" -> Color(0xFF7B4BD6)
    else -> BrandBlue
}

private fun kindLabel(kind: String) = kind.replaceFirstChar { it.uppercase() }

private fun kindIcon(kind: String): androidx.compose.ui.graphics.vector.ImageVector = when (kind) {
    "hospital" -> Icons.Filled.LocalHospital
    "pharmacy" -> Icons.Filled.LocalPharmacy
    "doctors" -> Icons.Filled.MedicalServices
    else -> Icons.Filled.HealthAndSafety
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NearbyClinicsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var origin by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var mapCenter by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var places by remember { mutableStateOf<List<NearbyClinics.Place>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var slowSearch by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var radiusKm by remember { mutableIntStateOf(3) }
    var manualPin by remember { mutableStateOf(false) }
    var pendingTap by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var addressField by remember { mutableStateOf("") }
    var addrEditing by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<NearbyClinics.Suggestion>>(emptyList()) }
    var kindFilter by remember { mutableStateOf("all") }
    val focusManager = LocalFocusManager.current

    val shown = remember(places, kindFilter) {
        if (kindFilter == "all") places else places.filter { it.kind == kindFilter }
    }

    val map = remember {
        LeafletMap(
            onMoveEnd = { lat, lon -> mapCenter = lat to lon },
            onMapTap = { lat, lon -> pendingTap = lat to lon },
        )
    }
    DisposableEffect(Unit) { onDispose { map.destroy() } }

    LaunchedEffect(shown) { map.setPlaces(shown) }

    var mountMap by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mountMap = true }

    // Drag-handle-resizable map with three snap points:
    //   0            hidden  (swipe the handle all the way up)
    //   mapNormal    default
    //   mapExpanded  big map, only "Your location" stays below it
    val density = LocalDensity.current
    val mapNormal = 260.dp
    val sheetOverlap = 20.dp
    var rootPx by remember { mutableIntStateOf(0) }
    var headerPx by remember { mutableIntStateOf(0) }
    var handlePx by remember { mutableIntStateOf(0) }
    var addressPx by remember { mutableIntStateOf(0) }
    val mapSlot = with(density) {
        (rootPx - headerPx - handlePx - addressPx).coerceAtLeast(0).toDp()
    }
    val mapExpanded = mapSlot.coerceAtLeast(mapNormal)
    var mapHeight by remember { mutableStateOf(mapNormal) }
    val dragHandle = rememberDraggableState { deltaPx ->
        val delta = with(density) { deltaPx.toDp() }
        mapHeight = (mapHeight + delta).coerceIn(0.dp, mapExpanded)
    }
    // Results section (Range + filters + list) height:
    //  - map hidden/small  -> grows to fill the space the map gave up (no grey gap)
    //  - map past normal    -> keeps its natural size so its rows slide off the edge
    //    intact rather than collapsing
    val resultsHeight = if (rootPx == 0) 320.dp
        else maxOf(mapSlot - mapHeight, mapExpanded - mapNormal).coerceAtLeast(0.dp)

    fun arrange(list: List<NearbyClinics.Place>, fromCache: Boolean): List<NearbyClinics.Place> {
        val o = origin
        val trimmed = if (fromCache && o != null) {
            list.filter { NearbyClinics.distanceMeters(o.first, o.second, it.lat, it.lon) <= radiusKm * 1000.0 }
        } else list
        return o?.let { org ->
            trimmed.sortedBy { NearbyClinics.distanceMeters(org.first, org.second, it.lat, it.lon) }
        } ?: trimmed
    }

    fun runSearch(around: Pair<Double, Double>) {
        loading = true
        slowSearch = false
        status = null
        val slowHint = scope.launch { delay(7000); slowSearch = true }
        scope.launch {
            val res = NearbyClinics.search(
                context, around.first, around.second, radiusKm * 1000,
                onPartial = { partial -> places = arrange(partial, fromCache = false) },
            )
            slowHint.cancel()
            slowSearch = false
            val sorted = arrange(res.places, res.fromCache)
            places = sorted
            loading = false
            status = when {
                sorted.isEmpty() && res.error != null -> "Couldn't reach the places service. Check your connection and retry."
                sorted.isEmpty() -> "No clinics or pharmacies mapped within $radiusKm km."
                res.fromCache -> "Showing last saved results (service unavailable)."
                else -> null
            }
        }
    }

    fun locateAndSearch() {
        loading = true
        status = null
        scope.launch {
            val loc = currentLocation(context)
            if (loc == null) {
                loading = false
                status = "Couldn't get your location. Turn on Location and try again."
                return@launch
            }
            manualPin = false
            addrEditing = false
            suggestions = emptyList()
            focusManager.clearFocus()
            origin = loc
            mapCenter = loc
            map.setUser(loc.first, loc.second)
            map.centerOn(loc.first, loc.second, 14)
            address = null
            scope.launch { address = NearbyClinics.reverseGeocode(loc.first, loc.second) }
            runSearch(loc)
        }
    }

    fun goTo(lat: Double, lon: Double, label: String?) {
        manualPin = true
        addrEditing = false
        suggestions = emptyList()
        focusManager.clearFocus()
        places = emptyList()
        selectedId = null
        origin = lat to lon
        mapCenter = lat to lon
        map.setUser(lat, lon)
        map.centerOn(lat, lon, 14)
        address = label
        if (label != null) addressField = label
        else scope.launch { address = NearbyClinics.reverseGeocode(lat, lon)?.also { addressField = it } }
        runSearch(lat to lon)
    }

    LaunchedEffect(pendingTap) {
        val t = pendingTap ?: return@LaunchedEffect
        goTo(t.first, t.second, null)
    }

    LaunchedEffect(address, addrEditing) {
        if (!addrEditing) addressField = address ?: ""
    }

    LaunchedEffect(addressField, addrEditing) {
        if (!addrEditing || addressField.trim().length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        suggestions = NearbyClinics.suggestAddresses(addressField)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.any { it }
        if (granted) locateAndSearch()
        else status = "Location permission is needed to find clinics near you."
    }

    LaunchedEffect(Unit) {
        if (granted) locateAndSearch()
        else permLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
            .onSizeChanged { rootPx = it.height },
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(BrandBlue)
                .statusBarsPadding()
                .height(56.dp)
                .onSizeChanged { headerPx = it.height },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Nearby Clinics",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { if (granted) locateAndSearch() }) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Recenter", tint = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((mapHeight.coerceIn(0.dp, mapExpanded) + sheetOverlap))
                .background(Color(0xFFDDE3EA))
                .clipToBounds()
        ) {
            if (mountMap) {
                AndroidView(
                    factory = { ctx -> map.createView(ctx) },
                    modifier = Modifier.fillMaxSize(),
                    update = { map.refreshSize() },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading map…", color = LabelGray, fontSize = 13.sp)
                }
            }
            if (loading) {
                CircularProgressIndicator(
                    color = BrandBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(28.dp),
                )
            }
        }

        // Handle + address + results, as one block that is never squished by the
        // parent Column (unbounded). As the map grows this block is pushed down and
        // its lower part (the results section) slides off the bottom edge intact,
        // while the handle + address stay pinned just under the map.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .offset(y = -sheetOverlap)
                .shadow(10.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .appBackground(),
        ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { handlePx = it.height }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = dragHandle,
                    onDragStopped = { velocity ->
                        val h = mapHeight
                        val target = when {
                            velocity < -700f -> if (h > mapNormal) mapNormal else 0.dp
                            velocity > 700f -> if (h < mapNormal) mapNormal else mapExpanded
                            else -> listOf(0.dp, mapNormal, mapExpanded)
                                .minBy { kotlin.math.abs((it - h).value) }
                        }
                        animate(
                            initialValue = mapHeight.value,
                            targetValue = target.value,
                        ) { v, _ -> mapHeight = v.dp }
                    },
                )
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(themed(Color(0xFFCBD2DC), Color(0xFF454750))),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { addressPx = it.height }
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                if (manualPin) "Picked location" else "Your location",
                color = LabelGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
            OutlinedTextField(
                value = addressField,
                onValueChange = { addressField = it; addrEditing = true },
                singleLine = true,
                placeholder = { Text("Search an address…", fontSize = 13.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                leadingIcon = {
                    Icon(
                        if (manualPin) Icons.Filled.Place else Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (addrEditing || addressField.isNotEmpty()) {
                        IconButton(onClick = {
                            addressField = ""
                            addrEditing = true
                            suggestions = emptyList()
                        }) { Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp)) }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
            suggestions.forEach { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { goTo(s.lat, s.lon, s.label) }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = LabelGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        s.label,
                        color = TextDark,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            if (!addrEditing) {
                Text(
                    "Tap the map, or edit the address above",
                    color = LabelGray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
        }

        // The controls (Range + category filter) scroll together with the results as
        // one list. Fixed height (its natural, normal-mode size) so it slides off the
        // bottom intact as the map is enlarged.
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(resultsHeight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Range", color = LabelGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(12.dp))
                        Slider(
                            value = radiusKm.toFloat(),
                            onValueChange = { radiusKm = it.roundToInt().coerceIn(1, 250) },
                            valueRange = 1f..250f,
                            enabled = !loading,
                            onValueChangeFinished = { (origin ?: mapCenter)?.let { runSearch(it) } },
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(BrandBlue)
                                        .border(3.dp, Color.White, CircleShape),
                                )
                            },
                            track = { sliderState ->
                                androidx.compose.material3.SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(6.dp),
                                    thumbTrackGapSize = 0.dp,
                                    drawStopIndicator = null,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        activeTrackColor = BrandBlue,
                                        inactiveTrackColor = BrandBlue.copy(alpha = 0.22f),
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "$radiusKm km",
                            color = TextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(64.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 76.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("1 km", color = LabelGray, fontSize = 10.sp)
                        Text("250 km", color = LabelGray, fontSize = 10.sp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(themed(Color(0xFFE9ECF1), Color(0xFF24252B)))
                        .padding(3.dp),
                ) {
                    listOf(
                        "all" to "All",
                        "clinic" to "Clinic",
                        "pharmacy" to "Pharmacy",
                        "hospital" to "Hospital",
                        "doctors" to "Doctors",
                    ).forEach { (key, label) ->
                        val selected = kindFilter == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glossyChip(selected, BrandBlue, RoundedCornerShape(8.dp))
                                .clickable { kindFilter = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (selected) Color.White else TextDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            status?.let { msg ->
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = LabelGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        if (!loading) {
                            Text(
                                "Retry",
                                color = BrandBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (granted) locateAndSearch()
                                    else permLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (shown.isNotEmpty()) {
                item {
                    Column {
                        if (loading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = BrandBlue,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Still searching for more…", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            "${shown.size} ${if (shown.size == 1) "place" else "places"}" +
                                (if (kindFilter != "all") " · ${kindLabel(kindFilter)}" else "") +
                                " within $radiusKm km",
                            color = LabelGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (loading && shown.isEmpty()) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrandBlue)
                            Spacer(Modifier.width(10.dp))
                            Text("Searching nearby…", color = LabelGray, fontSize = 12.sp)
                        }
                        if (slowSearch) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "The map service is slow right now — hang on…",
                                color = LabelGray,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
            if (!loading && shown.isEmpty() && places.isNotEmpty()) {
                item {
                    Text(
                        "No ${kindLabel(kindFilter).lowercase()} within $radiusKm km — try a wider range or category.",
                        color = LabelGray,
                        fontSize = 12.sp,
                    )
                }
            }
            items(shown, key = { it.id }) { place ->
                ClinicRow(
                    place = place,
                    distanceM = origin?.let {
                        NearbyClinics.distanceMeters(it.first, it.second, place.lat, place.lon)
                    },
                    expanded = selectedId == place.id,
                    onClick = {
                        selectedId = if (selectedId == place.id) null else place.id
                        map.centerOn(place.lat, place.lon, 17)
                    },
                    onDirections = {
                        val label = Uri.encode(place.name)
                        val geo = Uri.parse("geo:${place.lat},${place.lon}?q=${place.lat},${place.lon}($label)")
                        val intent = Intent(Intent.ACTION_VIEW, geo)
                        runCatching { context.startActivity(intent) }.onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.openstreetmap.org/?mlat=${place.lat}&mlon=${place.lon}#map=17/${place.lat}/${place.lon}"),
                                    )
                                )
                            }
                        }
                    },
                    onCall = { number ->
                        val digits = number.filter { it.isDigit() || it == '+' }
                        if (digits.isNotBlank()) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
                            }
                        }
                    },
                )
            }
        }
        }
    }
}

@Composable
private fun ClinicRow(
    place: NearbyClinics.Place,
    distanceM: Double?,
    expanded: Boolean,
    onClick: () -> Unit,
    onDirections: () -> Unit,
    onCall: (String) -> Unit,
) {
    val accent = kindColor(place.kind)
    val open = remember(place.hours) { NearbyClinics.openNow(place.hours) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(16.dp), CardWhite)
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.fyp.healthcare.ui.theme.AppIconBadge(kindIcon(place.kind), accent, size = 48.dp)
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    place.name,
                    color = TextDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(kindLabel(place.kind), color = LabelGray, fontSize = 12.sp)
                distanceM?.let {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = Color(0xFFE23539),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(formatDistance(it), color = LabelGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            open?.let { isOpen ->
                val c = if (isOpen) Color(0xFF1FA971) else Color(0xFFE23539)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.copy(alpha = 0.13f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(c))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isOpen) "Open" else "Closed", color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            place.hours?.let {
                Text("Hours: $it", color = LabelGray, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
            }
            place.address?.let {
                Text(it, color = LabelGray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(Icons.Filled.Directions, "Directions", BrandBlue, onDirections)
                place.phone?.let { num ->
                    PillButton(Icons.Filled.Call, "Call", Color(0xFF1FA971)) { onCall(num) }
                }
            }
        }
    }
}

@Composable
private fun PillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .glossyBadge(color, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDistance(m: Double): String =
    if (m < 950) "${m.toInt()} m" else String.format("%.1f km", m / 1000.0)

// ---------------------------------------------------------------------------
// Leaflet-in-a-WebView map controller
// ---------------------------------------------------------------------------

internal class LeafletMap(
    private val onMoveEnd: (Double, Double) -> Unit,
    private val onMapTap: (Double, Double) -> Unit,
) {
    private var web: WebView? = null
    private var ready = false
    private val pending = ArrayDeque<String>()

    @SuppressLint("SetJavaScriptEnabled")
    fun createView(context: Context): WebView {
        web?.let { return it }
        val w = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.userAgentString = (settings.userAgentString ?: "") + " CareApp/0.9 (FYP)"
            setBackgroundColor(0xFFDDE3EA.toInt())
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    ready = true
                    view?.evaluateJavascript("if(window.map){map.invalidateSize(true);}", null)
                    while (pending.isNotEmpty()) view?.evaluateJavascript(pending.removeFirst(), null)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    android.util.Log.w(
                        "NearbyMap",
                        "resource error ${error?.errorCode} ${error?.description} @ ${request?.url}",
                    )
                }
            }
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(m: android.webkit.ConsoleMessage): Boolean {
                    android.util.Log.i("NearbyMap", "console: ${m.message()} @${m.lineNumber()}")
                    return true
                }
            }
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onMoveEnd(lat: Double, lon: Double) = this@LeafletMap.onMoveEnd(lat, lon)

                @JavascriptInterface
                fun onMapTap(lat: Double, lon: Double) = this@LeafletMap.onMapTap(lat, lon)

                @JavascriptInterface
                fun onMapReady() { /* page-load callback; JS queue already flushed onPageFinished */ }
            }, "Android")
            loadUrl("file:///android_asset/nearby_map.html")
        }
        runCatching { WebView.setWebContentsDebuggingEnabled(true) }
        web = w
        return w
    }

    private fun run(js: String) {
        val w = web
        if (ready && w != null) w.evaluateJavascript(js, null) else pending.addLast(js)
    }

    fun refreshSize() = run("if(window.map){map.invalidateSize(true);}")

    fun setUser(lat: Double, lon: Double) = run("setUser($lat,$lon);")

    fun centerOn(lat: Double, lon: Double, zoom: Int) = run("centerOn($lat,$lon,$zoom);")

    fun setPlaces(places: List<NearbyClinics.Place>) {
        val arr = JSONArray()
        places.forEach { p ->
            arr.put(JSONObject().apply {
                put("lat", p.lat); put("lon", p.lon)
                put("name", p.name); put("color", colorHex(p.kind))
            })
        }
        run("setPlaces($arr);")
    }

    fun destroy() {
        runCatching {
            web?.apply {
                stopLoading()
                removeJavascriptInterface("Android")
                destroy()
            }
        }
        web = null
        ready = false
    }

    private fun colorHex(kind: String) = when (kind) {
        "hospital" -> "#D32F2F"
        "pharmacy" -> "#2E9E6B"
        "doctors" -> "#7B4BD6"
        else -> "#2A6DE1"
    }
}

// ---------------------------------------------------------------------------
// Location - framework LocationManager, no Play Services dependency
// ---------------------------------------------------------------------------

@SuppressLint("MissingPermission")
internal suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
    providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { return it.latitude to it.longitude }

    val provider = when {
        runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ->
            LocationManager.GPS_PROVIDER
        runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) ->
            LocationManager.NETWORK_PROVIDER
        else -> return null
    }
    return suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                runCatching { lm.removeUpdates(this) }
                if (cont.isActive) cont.resume(location.latitude to location.longitude)
            }

            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        runCatching {
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }.onFailure { if (cont.isActive) cont.resume(null) }
        cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
    }
}
