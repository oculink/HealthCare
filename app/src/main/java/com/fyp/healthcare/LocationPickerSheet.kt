package com.fyp.healthcare

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fyp.healthcare.ui.theme.GlossyButton
import com.fyp.healthcare.ui.theme.appBackground
import com.fyp.healthcare.ui.theme.glossyFieldColors
import com.fyp.healthcare.ui.theme.glossyFieldWell
import com.fyp.healthcare.ui.theme.glossyTopBar
import com.fyp.healthcare.ui.theme.themed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PickBlue = Color(0xFF2A6DE1)
private val PickCard: Color @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val PickText: Color @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val PickLabel: Color @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val PickPlaceholder: Color @Composable get() = themed(Color(0xFFA6ACB8), Color(0xFF6A7079))

/**
 * Full-screen location picker, styled like Nearby Clinics: a Leaflet map you can tap to
 * drop a pin, and an address box at the bottom with type-ahead suggestions (Photon). Used
 * as an overlay from [AddAppointmentScreen] — not a nav route (same pattern as the catalog
 * pickers). Confirming hands back the chosen address string plus its coordinates (or nulls
 * if the user just typed free text).
 *
 * The address box sits above the keyboard (`imePadding`); the map shrinks to make room.
 */
@Composable
fun LocationPickerSheet(
    initialAddress: String,
    initialLat: Double?,
    initialLng: Double?,
    onDismiss: () -> Unit,
    onConfirm: (address: String, lat: Double?, lng: Double?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var addressField by remember { mutableStateOf(initialAddress) }
    var pickedLat by remember { mutableStateOf(initialLat) }
    var pickedLng by remember { mutableStateOf(initialLng) }
    var editing by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<NearbyClinics.Suggestion>>(emptyList()) }
    var pendingTap by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var resolving by remember { mutableStateOf(false) }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val map = remember {
        LeafletMap(
            onMoveEnd = { _, _ -> },
            onMapTap = { lat, lon -> pendingTap = lat to lon },
        )
    }
    DisposableEffect(Unit) { onDispose { map.destroy() } }

    var mountMap by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mountMap = true }

    fun setPin(lat: Double, lon: Double, label: String?) {
        pickedLat = lat
        pickedLng = lon
        editing = false
        suggestions = emptyList()
        focusManager.clearFocus()
        map.setUser(lat, lon)
        map.centerOn(lat, lon, 15)
        if (label != null) {
            addressField = label
        } else {
            resolving = true
            scope.launch {
                val a = NearbyClinics.reverseGeocode(lat, lon)
                resolving = false
                if (a != null) addressField = a
            }
        }
    }

    fun locateMe() {
        resolving = true
        scope.launch {
            val loc = currentLocation(context)
            resolving = false
            if (loc != null) setPin(loc.first, loc.second, null)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.any { it }
        if (granted) locateMe()
    }

    // Centre on wherever we start from: an existing pin, else the phone's location.
    LaunchedEffect(Unit) {
        val la = initialLat
        val lo = initialLng
        if (la != null && lo != null) {
            map.setUser(la, lo)
            map.centerOn(la, lo, 15)
        } else if (granted) {
            locateMe()
        }
    }

    // Map taps drop a pin + reverse-geocode.
    LaunchedEffect(pendingTap) {
        val t = pendingTap ?: return@LaunchedEffect
        setPin(t.first, t.second, null)
    }

    // Debounced type-ahead.
    LaunchedEffect(addressField, editing) {
        if (!editing || addressField.trim().length < 3) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        suggestions = NearbyClinics.suggestAddresses(addressField)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground(),
    ) {

        // ===== Top bar =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glossyTopBar(PickBlue)
                .statusBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                "Choose location",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                if (granted) locateMe()
                else permLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }) {
                Icon(Icons.Filled.MyLocation, contentDescription = "My location", tint = Color.White)
            }
        }

        // ===== Map (fills the space the address panel doesn't take) =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFDDE3EA)),
        ) {
            if (mountMap) {
                AndroidView(
                    factory = { ctx -> map.createView(ctx) },
                    modifier = Modifier.fillMaxSize(),
                    update = { map.refreshSize() },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading map…", color = PickLabel, fontSize = 13.sp)
                }
            }
            if (resolving) {
                CircularProgressIndicator(
                    color = PickBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(26.dp),
                )
            }
            Text(
                "Tap the map to drop a pin",
                color = PickLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(themed(Color(0xCCFFFFFF), Color(0xCC1C1D22)), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // ===== Address box + suggestions + confirm — pinned above the keyboard =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(themed(Color(0xFFF4F6FA), Color(0xFF15161A)))
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "Address",
                color = PickLabel,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
            )
            OutlinedTextField(
                value = addressField,
                onValueChange = { addressField = it; editing = true },
                singleLine = true,
                placeholder = { Text("Type an address…", color = PickPlaceholder, fontSize = 14.sp) },
                textStyle = TextStyle(fontSize = 15.sp, color = PickText),
                leadingIcon = {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = PickBlue, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (addressField.isNotEmpty()) {
                        IconButton(onClick = {
                            addressField = ""
                            editing = true
                            suggestions = emptyList()
                            pickedLat = null
                            pickedLng = null
                        }) { Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp)) }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                colors = glossyFieldColors(accent = PickBlue),
                modifier = Modifier.fillMaxWidth().glossyFieldWell(RoundedCornerShape(12.dp)),
            )

            if (suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 176.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    suggestions.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { setPin(s.lat, s.lon, s.label) }
                                .padding(horizontal = 6.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = PickLabel, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                s.label,
                                color = PickText,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else if (!editing) {
                Text(
                    "Tap the map, search above, or use your current location",
                    color = PickLabel,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 3.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            GlossyButton(
                onClick = { onConfirm(addressField.trim(), pickedLat, pickedLng) },
                enabled = addressField.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                color = PickBlue,
            ) {
                Text("Use this address", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
