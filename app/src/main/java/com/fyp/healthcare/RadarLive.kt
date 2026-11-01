package com.fyp.healthcare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.fyp.healthcare.ui.theme.glossySurface
import com.fyp.healthcare.ui.theme.themed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// ───────────────────────── Data ─────────────────────────

data class RadarTarget(
    val id: Int,        // 0..2
    val xMm: Int,       // kiri(-) / kanan(+) dari sensor
    val yMm: Int,       // jarak ke hadapan
    val speedCms: Int,  // cm/s
) {
    val distanceM: Float get() = hypot(xMm.toFloat(), yMm.toFloat()) / 1000f
}

/** Baca data LD2450 (format JSON) dari ESP32 melalui Bluetooth. */
object RadarLiveTracker {
    var targets by mutableStateOf<List<RadarTarget>>(emptyList())
        private set
    var trail by mutableStateOf<List<RadarTarget>>(emptyList())
        private set
    var lastFrameMs by mutableStateOf(0L)
        private set
    var listening by mutableStateOf(false)
        private set

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(manager: BluetoothManager) {
        if (job?.isActive == true) return
        val input = manager.inputStream() ?: return
        job = scope.launch {
            listening = true
            try {
                val reader = input.bufferedReader()
                while (isActive) {
                    val line = reader.readLine() ?: break
                    parse(line)
                }
            } catch (_: IOException) {
            } finally {
                listening = false
                targets = emptyList()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        listening = false
        targets = emptyList()
        trail = emptyList()
        FallDetector.reset()
    }

    private fun parse(line: String) {
        val text = line.trim()
        if (!text.startsWith("{")) return
        try {
            val json = JSONObject(text)
            val list = ArrayList<RadarTarget>(3)
            for (i in 1..3) {
                val o = json.optJSONObject("target$i") ?: continue
                val x = o.optInt("x")
                val y = o.optInt("y")
                val s = o.optInt("speed")
                if (x == 0 && y == 0) continue
                list.add(RadarTarget(i - 1, x, y, s))
            }
            val now = System.currentTimeMillis()
            targets = list
            lastFrameMs = now
            trail = if (list.isEmpty()) trail.drop(3) else (trail + list).takeLast(90)
            FallDetector.onFrame(list, now)
        } catch (_: JSONException) {
        }
    }
}

// ───────────────────────── UI ─────────────────────────

private val TargetColors = listOf(
    Color(0xFF2A6DE1), Color(0xFF2E9E6B), Color(0xFFF29900)
)
private val CardWhite: Color
    @Composable get() = themed(Color(0xFFFFFFFF), Color(0xFF1C1D22))
private val TextDark: Color
    @Composable get() = themed(Color(0xFF1B1D23), Color(0xFFE8E9EC))
private val LabelGray: Color
    @Composable get() = themed(Color(0xFF5F6673), Color(0xFF9BA1AC))
private val GridColor: Color
    @Composable get() = themed(Color(0xFFCBD2DE), Color(0xFF3A3D47))

@Composable
fun LiveRadarCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { BluetoothManager(context) }

    var connecting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    val targets = RadarLiveTracker.targets
    val trail = RadarLiveTracker.trail
    val listening = RadarLiveTracker.listening
    val live = listening && now - RadarLiveTracker.lastFrameMs < 2500

    // Jika sudah connect sebelum ini, terus sambung semula bacaan.
    LaunchedEffect(Unit) {
        FallDetector.attach(context.applicationContext)
        if (manager.isConnected()) RadarLiveTracker.start(manager)
        while (true) {
            delay(500)
            now = System.currentTimeMillis()
        }
    }

    fun connectAndListen() {
        scope.launch {
            connecting = true
            message = "Sedang menyambung..."
            manager.connect()
                .onSuccess {
                    RadarLiveTracker.start(manager)
                    message = ""
                }
                .onFailure { message = it.message ?: "Sambungan gagal" }
            connecting = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val ok = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                results[Manifest.permission.BLUETOOTH_CONNECT] == true
        if (ok) connectAndListen()
        else message = "Permission Bluetooth ditolak. Benarkan dalam Settings."
    }

    fun onStartClick() {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ).filter {
                ContextCompat.checkSelfPermission(context, it) !=
                        PackageManager.PERMISSION_GRANTED
            }
        } else emptyList()

        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
        else connectAndListen()
    }

    val gridColor = GridColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glossySurface(RoundedCornerShape(20.dp), CardWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Live Radar Tracking",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark
        )
        Text(
            when {
                !listening -> "Tidak bersambung"
                !live -> "Disambung, menunggu data radar..."
                targets.isEmpty() -> "Tiada sesiapa dikesan"
                else -> "${targets.size} sasaran dikesan"
            },
            fontSize = 12.sp, color = LabelGray
        )

        // Amaran jatuh
        if (FallDetector.state == FallDetector.State.SUSPECTED) {
            val secs = ((FallDetector.alertDeadlineMs - now) / 1000).coerceAtLeast(0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDECEA), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Kemungkinan jatuh dikesan!",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFFB3261E)
                )
                Text(
                    "Amaran akan dihantar dalam ${secs} saat melainkan anda tekan Saya OK.",
                    fontSize = 12.sp, color = Color(0xFF8A1C14)
                )
                Button(
                    onClick = { FallDetector.confirmOk() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Saya OK") }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h - 6.dp.toPx()
            val maxMm = 6000f
            val scale = (cy - 4.dp.toPx()) / maxMm

            // Cincin jarak setiap 1 m
            for (m in 1..6) {
                val r = m * 1000f * scale
                drawArc(
                    color = gridColor,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(1.dp.toPx())
                )
            }
            // Sempadan FOV +-60 darjah
            val edgeX = maxMm * sin(Math.toRadians(60.0)).toFloat() * scale
            val edgeY = maxMm * cos(Math.toRadians(60.0)).toFloat() * scale
            drawLine(gridColor, Offset(cx, cy), Offset(cx - edgeX, cy - edgeY), 1.dp.toPx())
            drawLine(gridColor, Offset(cx, cy), Offset(cx + edgeX, cy - edgeY), 1.dp.toPx())
            drawLine(gridColor, Offset(cx, cy), Offset(cx, cy - maxMm * scale), 1.dp.toPx())

            // Sensor
            drawCircle(Color(0xFFD32F2F), 5.dp.toPx(), Offset(cx, cy))

            // Jejak pergerakan
            trail.forEachIndexed { i, t ->
                val a = (i + 1f) / trail.size * 0.45f
                drawCircle(
                    TargetColors[t.id % 3].copy(alpha = a),
                    3.dp.toPx(),
                    Offset(cx + t.xMm * scale, cy - t.yMm * scale)
                )
            }
            // Sasaran semasa
            if (live) {
                targets.forEach { t ->
                    val c = TargetColors[t.id % 3]
                    val pos = Offset(cx + t.xMm * scale, cy - t.yMm * scale)
                    drawCircle(c.copy(alpha = 0.25f), 16.dp.toPx(), pos)
                    drawCircle(c, 8.dp.toPx(), pos)
                }
            }
        }

        Text("Setiap cincin = 1 meter. Titik merah = sensor.", fontSize = 11.sp, color = LabelGray)

        if (live) {
            targets.forEach { t ->
                Text(
                    "Sasaran ${t.id + 1}: %.1f m  |  %.2f m/s".format(
                        t.distanceM, abs(t.speedCms) / 100f
                    ),
                    fontSize = 13.sp, color = TextDark
                )
            }
        }

        if (message.isNotEmpty()) {
            Text(message, fontSize = 12.sp, color = Color(0xFFD32F2F))
        }

        Spacer(Modifier.height(2.dp))

        if (!listening) {
            Button(
                onClick = { onStartClick() },
                enabled = !connecting,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (connecting) "Connecting..." else "Start Live Tracking") }
        } else {
            Button(
                onClick = {
                    scope.launch {
                        RadarLiveTracker.stop()
                        manager.disconnect()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Stop") }
        }

        Button(
            onClick = { FallDetector.simulate() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Uji amaran jatuh") }
    }
}