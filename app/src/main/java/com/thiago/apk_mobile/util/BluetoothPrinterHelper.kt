package com.thiago.apk_mobile.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
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
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            Log.d("BluetoothPrinterHelper", "Creating RFCOMM socket")
            socket = device.createRfcommSocketToServiceRecord(sppUuid)

            bluetoothAdapter?.cancelDiscovery()
            
            Log.d("BluetoothPrinterHelper", "Connecting to socket...")
            socket.connect()
            Log.d("BluetoothPrinterHelper", "Socket connected")

            outputStream = socket.outputStream
            
            Log.d("BluetoothPrinterHelper", "Writing to output stream...")
            outputStream.write(text.toByteArray(charset("CP437")))
            outputStream.flush()
            Log.d("BluetoothPrinterHelper", "Write successful")

            delay(500)

            Result.success(Unit)
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "IOException during printing", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("BluetoothPrinterHelper", "Exception during printing", e)
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
                Log.d("BluetoothPrinterHelper", "Output stream closed")
            } catch (e: IOException) {
                Log.e("BluetoothPrinterHelper", "Error closing output stream", e)
            }
            try {
                socket?.close()
                Log.d("BluetoothPrinterHelper", "Socket closed")
            } catch (e: IOException) {
                Log.e("BluetoothPrinterHelper", "Error closing socket", e)
            }
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
