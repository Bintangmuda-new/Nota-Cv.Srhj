package com.srhj.nota.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

data class PerangkatBluetooth(val nama: String, val alamat: String)

/** Koneksi ke printer thermal Bluetooth (profil SPP / serial). */
class PrinterBluetooth(private val context: Context) {

    private val adapter get() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun didukung(): Boolean = adapter != null
    fun aktif(): Boolean = adapter?.isEnabled == true

    fun punyaIzin(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    /** Daftar perangkat yang sudah di-pairing di menu Bluetooth HP. */
    @SuppressLint("MissingPermission")
    fun perangkatTerpasang(): List<PerangkatBluetooth> {
        if (!punyaIzin()) throw SecurityException("Izin Bluetooth belum diberikan")
        val a = adapter ?: return emptyList()
        return a.bondedDevices.orEmpty()
            .map { PerangkatBluetooth(it.name ?: it.address, it.address) }
            .sortedBy { it.nama.lowercase() }
    }

    @SuppressLint("MissingPermission")
    suspend fun cetak(alamat: String, data: ByteArray) = withContext(Dispatchers.IO) {
        if (alamat.isBlank()) throw IOException("Printer belum dipilih. Buka menu Pengaturan.")
        if (!punyaIzin()) throw SecurityException("Izin Bluetooth belum diberikan")
        val a = adapter ?: throw IOException("HP ini tidak mendukung Bluetooth")
        if (!a.isEnabled) throw IOException("Bluetooth belum dinyalakan")
        val device = a.getRemoteDevice(alamat)
        a.cancelDiscovery()
        var socket: BluetoothSocket? = null
        try {
            socket = try {
                device.createRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
            } catch (e: IOException) {
                runCatching { socket?.close() }
                device.createInsecureRfcommSocketToServiceRecord(SPP_UUID).also { it.connect() }
            }
            val out = socket!!.outputStream
            // Kirim bertahap agar buffer printer murah tidak meluap.
            data.toList().chunked(512).forEach {
                out.write(it.toByteArray())
                out.flush()
                delay(20)
            }
            delay(300)
        } catch (e: IOException) {
            throw IOException("Gagal terhubung ke printer. Pastikan printer menyala & dekat. (${e.message})", e)
        } finally {
            runCatching { socket?.close() }
        }
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /**
         * Muat PNG tanda tangan dan ubah jadi gambar hitam-putih untuk printer.
         * Lebar maksimal 60% kertas agar proporsional.
         */
        fun muatTtdUntukPrinter(path: String?, lebarKertasPx: Int): Triple<Int, Int, (Int, Int) -> Boolean>? {
            if (path.isNullOrBlank() || !File(path).exists()) return null
            val asli = BitmapFactory.decodeFile(path) ?: return null
            val lebarTarget = (lebarKertasPx * 0.6).toInt()
            val skala = lebarTarget.toFloat() / asli.width
            val tinggi = (asli.height * skala).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createScaledBitmap(asli, lebarTarget, tinggi, true)
            val piksel = IntArray(bmp.width * bmp.height)
            bmp.getPixels(piksel, 0, bmp.width, 0, 0, bmp.width, bmp.height)
            val w = bmp.width
            val h = bmp.height
            return Triple(w, h) { x, y ->
                val c = piksel[y * w + x]
                Color.alpha(c) > 128 && (Color.red(c) + Color.green(c) + Color.blue(c)) / 3 < 128
            }
        }
    }
}
