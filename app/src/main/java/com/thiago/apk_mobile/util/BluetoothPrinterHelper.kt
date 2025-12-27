package com.thiago.apk_mobile.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterHelper(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            return emptyList()
        }
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun printText(deviceAddress: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            return@withContext Result.failure(SecurityException("Bluetooth permission not granted."))
        }

        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        if (device == null) {
            return@withContext Result.failure(IllegalArgumentException("Device not found"))
        }

        val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        try {
            // Pequeño retraso para dar tiempo al stack de Bluetooth a asentarse
            delay(100)

            device.createRfcommSocketToServiceRecord(sppUuid).use { socket ->
                socket.connect()
                socket.outputStream.use { outputStream ->
                    outputStream.write(text.toByteArray(charset("CP437")))
                    outputStream.flush()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Para versiones antiguas, el permiso se da en la instalación
        }
    }
}
