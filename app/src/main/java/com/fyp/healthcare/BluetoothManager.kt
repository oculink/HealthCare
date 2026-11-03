package com.fyp.healthcare

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothManager(private val context: Context) {

    private val adapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    // Socket dikongsi supaya kekal walaupun skrin ditutup & dibuka semula.
    private var socket: BluetoothSocket?
        get() = sharedSocket
        set(value) { sharedSocket = value }

    companion object {
        @Volatile
        private var sharedSocket: BluetoothSocket? = null

        private const val DEVICE_NAME = "HealthCare_Radar"

        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    fun isBluetoothSupported(): Boolean = adapter != null

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        if (!hasBluetoothPermission()) return false
        return adapter?.isEnabled == true
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    private fun hasBluetoothPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (adapter == null) {
                return@withContext Result.failure(
                    Exception("Bluetooth isn't supported")
                )
            }

            if (!hasBluetoothPermission()) {
                return@withContext Result.failure(
                    SecurityException("Bluetooth permission is required")
                )
            }

            if (!adapter.isEnabled) {
                return@withContext Result.failure(
                    Exception("Please turn on Bluetooth")
                )
            }

            // Jika sambungan masih aktif, gunakan sambungan sedia ada.
            if (socket?.isConnected == true) {
                return@withContext Result.success(
                    "Connected to $DEVICE_NAME"
                )
            }

            val device: BluetoothDevice =
                adapter.bondedDevices.firstOrNull {
                    it.name == DEVICE_NAME
                } ?: return@withContext Result.failure(
                    Exception("ESP32 isn't paired yet. Pair HealthCare_Radar first.")
                )

            try {
                socket?.close()
            } catch (_: IOException) {
            }
            socket = null

            val newSocket =
                device.createRfcommSocketToServiceRecord(SPP_UUID)

            newSocket.connect()
            socket = newSocket

            Result.success("Connected to $DEVICE_NAME")

        } catch (e: SecurityException) {
            Result.failure(
                Exception("Bluetooth permission was not granted")
            )
        } catch (e: Exception) {
            try {
                socket?.close()
            } catch (_: IOException) {
            }
            socket = null

            Result.failure(
                Exception("Couldn't connect Bluetooth: ${e.message}")
            )
        }
    }

    suspend fun sendMessage(message: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (!hasBluetoothPermission()) {
                    return@withContext Result.failure(
                        SecurityException("Bluetooth permission is required")
                    )
                }

                val currentSocket = socket
                    ?: return@withContext Result.failure(
                        Exception("Bluetooth isn't connected")
                    )

                if (!currentSocket.isConnected) {
                    return@withContext Result.failure(
                        Exception("Bluetooth was disconnected")
                    )
                }

                currentSocket.outputStream.write(
                    "$message\n".toByteArray(Charsets.UTF_8)
                )
                currentSocket.outputStream.flush()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun inputStream(): java.io.InputStream? = socket?.inputStream

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        socket = null
    }
}