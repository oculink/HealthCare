package com.fyp.healthcare

import com.fyp.healthcare.i18n.Text
import com.fyp.healthcare.i18n.tr
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun BluetoothTestScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val bluetoothManager = remember {
        BluetoothManager(context)
    }

    var status by remember {
        mutableStateOf("Checking Bluetooth connection...")
    }

    var connecting by remember {
        mutableStateOf(false)
    }

    // Periksa semula status apabila skrin dibuka.
    LaunchedEffect(bluetoothManager) {
        status = if (bluetoothManager.isConnected()) {
            "Connected to HealthCare_Radar"
        } else {
            "Bluetooth isn't connected"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            results[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            true
        }

        if (granted) {
            connectBluetooth(
                bluetoothManager,
                { status = it },
                { connecting = it },
                scope
            )
        } else {
            status = "Bluetooth permission denied. Allow it in Settings."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = {
                backDispatcher?.onBackPressed()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Back")
        }

        Text(
            text = "ESP32 Bluetooth Test",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(text = "Device: HealthCare_Radar")
        Text(text = "Status: ${tr(status)}")

        Button(
            onClick = {
                if (!bluetoothManager.isBluetoothSupported()) {
                    status = "This phone doesn't support Bluetooth."
                    return@Button
                }

                val permissionsNeeded =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        listOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ).filter {
                            ContextCompat.checkSelfPermission(
                                context,
                                it
                            ) != PackageManager.PERMISSION_GRANTED
                        }
                    } else {
                        emptyList()
                    }

                if (permissionsNeeded.isNotEmpty()) {
                    permissionLauncher.launch(
                        permissionsNeeded.toTypedArray()
                    )
                } else {
                    connectBluetooth(
                        bluetoothManager,
                        { status = it },
                        { connecting = it },
                        scope
                    )
                }
            },
            enabled = !connecting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (connecting) "Connecting..." else "Connect ESP32")
        }

        Button(
            onClick = {
                scope.launch {
                    bluetoothManager.disconnect()
                    status = "Bluetooth disconnected"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Disconnect")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Make sure the ESP32 is on and paired with the phone before you tap Connect."
        )
    }
}

private fun connectBluetooth(
    bluetoothManager: BluetoothManager,
    onStatus: (String) -> Unit,
    onConnecting: (Boolean) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        onConnecting(true)
        onStatus("Connecting...")

        val result = bluetoothManager.connect()

        onStatus(
            result.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "Couldn't connect" }
            )
        )

        onConnecting(false)
    }
}
