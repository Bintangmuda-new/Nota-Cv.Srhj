package com.srhj.nota.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.srhj.nota.NotaApp
import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.pdf.PdfGenerator
import com.srhj.nota.print.PrinterBluetooth
import com.srhj.nota.print.StrukTeks
import com.srhj.nota.share.Berkas
import com.srhj.nota.util.RingkasanLaporan
import com.srhj.nota.util.buatCsvLaporan
import com.srhj.nota.util.formatRupiah
import com.srhj.nota.util.formatTanggalJam
import com.srhj.nota.util.namaFileAman
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val MIME_PDF = "application/pdf"
const val MIME_CSV = "text/csv"

/** Data laporan yang sedang ditampilkan. */
data class DataLaporan(val judulPeriode: String, val namaFile: String, val daftar: List<NotaLengkap>, val ringkasan: RingkasanLaporan)

/** Aksi cetak / simpan / kirim WA untuk nota & laporan. Pesan hasil dikirim ke [pesan] (snackbar). */
class AksiDokumen(
    private val app: NotaApp,
    private val scope: CoroutineScope,
    private val mintaIzinLaluJalankan: (() -> Unit) -> Unit,
    private val pesan: (String) -> Unit,
) {
    private val toko get() = app.pengaturan.info.value

    private fun jalankan(sukses: String?, blok: suspend () -> String?) {
        scope.launch {
            try {
                val hasil = blok()
                (hasil ?: sukses)?.let(pesan)
            } catch (e: Exception) {
                pesan(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    // ---------- Nota ----------
    private suspend fun pdfNota(n: NotaLengkap): File = withContext(Dispatchers.IO) {
        PdfGenerator.buatNota(n, toko, File(Berkas.folderNota(app), "Nota_${namaFileAman(n.nota.nomor)}.pdf"))
    }

    fun cetakNota(n: NotaLengkap) = mintaIzinLaluJalankan {
        jalankan("Nota ${n.nota.nomor} dikirim ke printer") {
            val t = toko
            val data = withContext(Dispatchers.Default) {
                StrukTeks.nota(n, t, PrinterBluetooth.muatTtdUntukPrinter(n.nota.ttdPath, t.lebarPixelPrinter))
            }
            app.printer.cetak(t.printerAlamat, data)
            null
        }
    }

    fun simpanNota(n: NotaLengkap) = jalankan(null) {
        val f = pdfNota(n)
        "PDF tersimpan di " + withContext(Dispatchers.IO) { Berkas.simpanKeDownload(app, f, MIME_PDF) }
    }

    fun kirimNota(n: NotaLengkap) = jalankan(null) {
        val f = pdfNota(n)
        val teks = buildString {
            append("*NOTA ${n.nota.nomor}*\n")
            append("Tanggal: ${formatTanggalJam(n.nota.tanggal)}\n")
            append("Kepada: ${n.nota.pelanggan}\n")
            append("Total: *${formatRupiah(n.nota.total)}*\n\n")
            append("Terima kasih.\n${toko.namaToko}")
        }
        Berkas.kirimWhatsApp(app, f, MIME_PDF, teks, n.nota.teleponPelanggan)
    }

    // ---------- Laporan ----------
    private suspend fun pdfLaporan(d: DataLaporan): File = withContext(Dispatchers.IO) {
        PdfGenerator.buatLaporan(d.judulPeriode, d.daftar, d.ringkasan, toko, File(Berkas.folderLaporan(app), "${d.namaFile}.pdf"))
    }

    private suspend fun csvLaporan(d: DataLaporan): File = withContext(Dispatchers.IO) {
        File(Berkas.folderLaporan(app), "${d.namaFile}.csv").apply { writeText(buatCsvLaporan(d.daftar), Charsets.UTF_8) }
    }

    fun cetakLaporan(d: DataLaporan) = mintaIzinLaluJalankan {
        jalankan("Laporan dikirim ke printer") {
            val t = toko
            app.printer.cetak(t.printerAlamat, StrukTeks.laporan(d.judulPeriode, d.daftar, d.ringkasan, t))
            null
        }
    }

    fun simpanLaporanPdf(d: DataLaporan) = jalankan(null) {
        val f = pdfLaporan(d)
        "PDF tersimpan di " + withContext(Dispatchers.IO) { Berkas.simpanKeDownload(app, f, MIME_PDF) }
    }

    fun simpanLaporanExcel(d: DataLaporan) = jalankan(null) {
        val f = csvLaporan(d)
        "File Excel (CSV) tersimpan di " + withContext(Dispatchers.IO) { Berkas.simpanKeDownload(app, f, MIME_CSV) }
    }

    fun kirimLaporan(d: DataLaporan) = jalankan(null) {
        val f = pdfLaporan(d)
        val teks = "*LAPORAN PENJUALAN*\n${d.judulPeriode}\nJumlah nota: ${d.ringkasan.jumlahNota}\n" +
            "Total omzet: *${formatRupiah(d.ringkasan.totalOmzet)}*\n\n${toko.namaToko}"
        Berkas.kirimWhatsApp(app, f, MIME_PDF, teks, null)
    }

    fun tesCetak() = mintaIzinLaluJalankan {
        jalankan("Tes cetak dikirim ke printer") {
            app.printer.cetak(toko.printerAlamat, StrukTeks.tesCetak(toko)); null
        }
    }
}

/**
 * Membungkus aksi yang butuh Bluetooth: minta izin BLUETOOTH_CONNECT (Android 12+) dulu bila perlu.
 */
@Composable
fun rememberIzinBluetooth(pesan: (String) -> Unit): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val app = context.applicationContext as NotaApp
    val tertunda = remember { arrayOfNulls<() -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val aksi = tertunda[0]
        tertunda[0] = null
        if (ok) aksi?.invoke() else pesan("Izin Bluetooth ditolak. Aktifkan di Pengaturan HP > Aplikasi > Nota SRHJ.")
    }
    return remember {
        { aksi: () -> Unit ->
            if (app.printer.punyaIzin()) aksi()
            else {
                tertunda[0] = aksi
                launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }
}

@Composable
fun rememberAksiDokumen(pesan: (String) -> Unit): AksiDokumen {
    val app = LocalContext.current.applicationContext as NotaApp
    val scope = rememberCoroutineScope()
    val izin = rememberIzinBluetooth(pesan)
    return remember(app, scope) { AksiDokumen(app, scope, izin, pesan) }
}
