package com.srhj.nota.share

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.srhj.nota.util.normalisasiNomorWa
import java.io.File

/**
 * Penyimpanan & berbagi file.
 *  - File dibuat dulu di penyimpanan internal (files/dokumen/...), lalu
 *  - "Simpan" menyalin ke folder Download/NotaSRHJ agar mudah ditemukan di File Manager.
 *  - "Kirim WA" membuka WhatsApp Business (atau WhatsApp biasa bila Business tidak ada).
 */
object Berkas {
    const val WA_BISNIS = "com.whatsapp.w4b"
    const val WA_BIASA = "com.whatsapp"
    const val FOLDER_PUBLIK = "NotaSRHJ"

    fun folderNota(context: Context) = File(context.filesDir, "dokumen/nota").apply { mkdirs() }
    fun folderLaporan(context: Context) = File(context.filesDir, "dokumen/laporan").apply { mkdirs() }

    private fun terpasang(context: Context, paket: String): Boolean = try {
        context.packageManager.getPackageInfo(paket, 0); true
    } catch (e: PackageManager.NameNotFoundException) { false }

    /** Salin file ke Download/NotaSRHJ. Mengembalikan lokasi yang bisa ditampilkan ke pengguna. */
    fun simpanKeDownload(context: Context, file: File, mime: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER_PUBLIK)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Tidak bisa membuat file di folder Download")
            resolver.openOutputStream(uri).use { out -> file.inputStream().use { it.copyTo(out!!) } }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return "Download/$FOLDER_PUBLIK/${file.name}"
        } else {
            // Android 8-9: simpan di folder Documents milik aplikasi (tanpa izin tambahan).
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), FOLDER_PUBLIK).apply { mkdirs() }
            val dst = File(dir, file.name)
            file.copyTo(dst, overwrite = true)
            return dst.absolutePath
        }
    }

    /**
     * Kirim file lewat WhatsApp Business. Bila nomor pelanggan diisi, langsung membuka chat pelanggan itu.
     * @return pesan untuk pengguna jika WhatsApp tidak ditemukan dan dipakai pilihan aplikasi lain.
     */
    fun kirimWhatsApp(context: Context, file: File, mime: String, pesan: String, nomorTujuan: String?): String? {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        fun intentDasar() = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, pesan)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val nomor = nomorTujuan?.let { normalisasiNomorWa(it) }
        for (paket in listOf(WA_BISNIS, WA_BIASA)) {
            if (!terpasang(context, paket)) continue
            val intent = intentDasar().setPackage(paket)
            if (nomor != null) intent.putExtra("jid", "$nomor@s.whatsapp.net")
            try {
                context.startActivity(intent)
                return if (paket == WA_BIASA) "WhatsApp Business tidak ditemukan, memakai WhatsApp biasa" else null
            } catch (_: ActivityNotFoundException) { /* coba berikutnya */ }
        }
        val chooser = Intent.createChooser(intentDasar(), "Bagikan lewat").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        return "WhatsApp tidak terpasang, silakan pilih aplikasi lain"
    }
}
