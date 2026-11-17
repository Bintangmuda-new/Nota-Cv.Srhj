package com.srhj.nota.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.srhj.nota.data.InfoToko
import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.data.subtotal
import com.srhj.nota.util.RingkasanLaporan
import com.srhj.nota.util.formatAngka
import com.srhj.nota.util.formatRupiah
import com.srhj.nota.util.formatTanggal
import com.srhj.nota.util.formatTanggalJam
import java.io.File

/** Tujuan gambar per halaman: PDF asli (aplikasi) atau Bitmap (pengujian / pratinjau). */
interface PenulisHalaman {
    fun mulai(lebar: Int, tinggi: Int, nomor: Int): Canvas
    fun selesai()
}

class PenulisPdf : PenulisHalaman {
    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    override fun mulai(lebar: Int, tinggi: Int, nomor: Int): Canvas =
        doc.startPage(PdfDocument.PageInfo.Builder(lebar, tinggi, nomor).create()).also { page = it }.canvas
    override fun selesai() { page?.let(doc::finishPage); page = null }
    fun tulis(tujuan: File): File {
        tujuan.parentFile?.mkdirs()
        try { tujuan.outputStream().use { doc.writeTo(it) } } finally { doc.close() }
        return tujuan
    }
}

/** Menggambar halaman ke Bitmap (skala 2x) — dipakai untuk uji visual. */
class PenulisBitmap(private val skala: Float = 2f) : PenulisHalaman {
    val halaman = mutableListOf<Bitmap>()
    override fun mulai(lebar: Int, tinggi: Int, nomor: Int): Canvas {
        val bmp = Bitmap.createBitmap((lebar * skala).toInt(), (tinggi * skala).toInt(), Bitmap.Config.ARGB_8888)
        halaman += bmp
        return Canvas(bmp).apply { drawColor(Color.WHITE); scale(skala, skala) }
    }
    override fun selesai() {}
}

/**
 * Membuat file PDF:
 *  - Nota  : ukuran A5 (148 x 210 mm), rapi untuk dikirim via WhatsApp & dicetak.
 *  - Laporan: ukuran A4, tabel rincian nota + rekap barang, otomatis berganti halaman.
 */
object PdfGenerator {
    private const val A5_W = 420
    private const val A5_H = 595
    private const val A4_W = 595
    private const val A4_H = 842
    private val BIRU = Color.rgb(13, 71, 161)
    private val ABU = Color.rgb(240, 243, 248)
    private val GARIS = Color.rgb(200, 205, 215)

    private fun paint(ukuran: Float, tebal: Boolean = false, warna: Int = Color.BLACK) = Paint().apply {
        isAntiAlias = true
        textSize = ukuran
        color = warna
        typeface = Typeface.create(Typeface.SANS_SERIF, if (tebal) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Potong teks dengan "…" agar muat di lebar tertentu. */
    private fun muat(teks: String, p: Paint, lebar: Float): String {
        if (p.measureText(teks) <= lebar) return teks
        var t = teks
        while (t.isNotEmpty() && p.measureText("$t…") > lebar) t = t.dropLast(1)
        return "$t…"
    }

    private fun Canvas.kanan(teks: String, xKanan: Float, y: Float, p: Paint) =
        drawText(teks, xKanan - p.measureText(teks), y, p)

    private fun Canvas.tengah(teks: String, x: Float, y: Float, p: Paint) =
        drawText(teks, x - p.measureText(teks) / 2, y, p)

    /** Kepala dokumen: nama toko, alamat, telepon, judul. Mengembalikan posisi y berikutnya. */
    private fun Canvas.kepala(toko: InfoToko, lebarHal: Int, margin: Float, judul: String): Float {
        drawRect(0f, 0f, lebarHal.toFloat(), 6f, Paint().apply { color = BIRU })
        var y = margin + 14f
        drawText(muat(toko.namaToko.uppercase(), paint(15f, true, BIRU), lebarHal - 2 * margin - 130), margin, y, paint(15f, true, BIRU))
        kanan(judul, lebarHal - margin, y, paint(13f, true))
        val kecil = paint(8.5f, warna = Color.DKGRAY)
        if (toko.alamat.isNotBlank()) { y += 13f; drawText(muat(toko.alamat, kecil, lebarHal - 2 * margin), margin, y, kecil) }
        if (toko.telepon.isNotBlank()) { y += 11f; drawText("Telp/WA: ${toko.telepon}", margin, y, kecil) }
        y += 10f
        drawLine(margin, y, lebarHal - margin, y, Paint().apply { color = BIRU; strokeWidth = 1.2f })
        return y + 16f
    }

    fun buatNota(nota: NotaLengkap, toko: InfoToko, tujuan: File): File =
        PenulisPdf().also { gambarNota(nota, toko, it) }.tulis(tujuan)

    fun gambarNota(nota: NotaLengkap, toko: InfoToko, doc: PenulisHalaman) {
        val m = 28f
        val kanan = A5_W - m
        val n = nota.nota
        val isi = paint(9f)
        val isiTebal = paint(9f, true)
        val label = paint(8f, warna = Color.DKGRAY)
        val kolNo = m + 4
        val kolNama = m + 22
        val kolQty = kanan - 170
        val kolHarga = kanan - 85
        val tinggiBaris = 16f

        var halNo = 1
        var c = doc.mulai(A5_W, A5_H, halNo)
        var y = c.kepala(toko, A5_W, m, "NOTA")

        // Info nota
        c.drawText("No. Nota", m, y, label); c.drawText(n.nomor, m + 60, y, isiTebal)
        c.drawText("Kepada", A5_W / 2f + 10, y, label)
        c.drawText(muat(n.pelanggan, isiTebal, kanan - (A5_W / 2f + 60)), A5_W / 2f + 60, y, isiTebal)
        y += 13f
        c.drawText("Tanggal", m, y, label); c.drawText(formatTanggalJam(n.tanggal), m + 60, y, isi)
        if (n.teleponPelanggan.isNotBlank()) {
            c.drawText("Telp/WA", A5_W / 2f + 10, y, label); c.drawText(n.teleponPelanggan, A5_W / 2f + 60, y, isi)
        }
        y += 18f

        fun kepalaTabel() {
            c.drawRect(m, y - 11f, kanan, y + 5f, Paint().apply { color = BIRU })
            val putih = paint(8.5f, true, Color.WHITE)
            c.drawText("No", kolNo, y, putih)
            c.drawText("Nama Barang", kolNama, y, putih)
            c.kanan("Jml", kolQty + 20, y, putih)
            c.kanan("Harga", kolHarga + 30, y, putih)
            c.kanan("Subtotal", kanan - 4, y, putih)
            y += tinggiBaris
        }
        kepalaTabel()

        nota.items.forEachIndexed { i, item ->
            if (y > A5_H - 150) {
                doc.selesai()
                halNo++
                c = doc.mulai(A5_W, A5_H, halNo)
                y = c.kepala(toko, A5_W, m, "NOTA")
                c.drawText("${n.nomor} (lanjutan)", m, y, label)
                y += 16f
                kepalaTabel()
            }
            if (i % 2 == 1) c.drawRect(m, y - 11f, kanan, y + 5f, Paint().apply { color = ABU })
            c.drawText("${i + 1}", kolNo, y, isi)
            c.drawText(muat(item.nama, isi, kolQty - kolNama - 8), kolNama, y, isi)
            c.kanan(item.jumlah.toString(), kolQty + 20, y, isi)
            c.kanan(formatAngka(item.harga), kolHarga + 30, y, isi)
            c.kanan(formatAngka(item.subtotal()), kanan - 4, y, isi)
            y += tinggiBaris
        }
        c.drawLine(m, y - 9f, kanan, y - 9f, Paint().apply { color = GARIS })
        y += 6f
        val totalP = paint(11f, true, BIRU)
        c.drawText("TOTAL", kolHarga - 40, y, totalP)
        c.kanan(formatRupiah(n.total), kanan - 4, y, totalP)
        y += 13f
        c.drawText("Jumlah barang: ${nota.items.sumOf { it.jumlah }}", kolHarga - 40, y, label)

        if (n.catatan.isNotBlank()) {
            y += 18f
            c.drawText("Catatan:", m, y, label)
            y += 11f
            c.drawText(muat(n.catatan, isi, kanan - m), m, y, isi)
        }

        // Tanda tangan
        val yTtd = maxOf(y + 26f, A5_H - 130f)
        val xTtd = kanan - 120f
        c.tengah("Penerima,", xTtd + 60, yTtd, isi)
        c.tengah("Hormat kami,", m + 60, yTtd, isi)
        val ttd = n.ttdPath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it) }
        if (ttd != null) {
            val maxW = 120f
            val maxH = 55f
            val s = minOf(maxW / ttd.width, maxH / ttd.height)
            val w = ttd.width * s
            val h = ttd.height * s
            val x0 = xTtd + (120 - w) / 2
            c.drawBitmap(ttd, null, RectF(x0, yTtd + 6, x0 + w, yTtd + 6 + h), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val yNama = yTtd + 72f
        c.tengah("( ${n.namaPenerima.ifBlank { "                    " }} )", xTtd + 60, yNama, isiTebal)
        c.tengah("( ${toko.namaToko} )".let { muat(it, isiTebal, 140f) }, m + 60, yNama, isiTebal)

        if (toko.catatanKaki.isNotBlank()) {
            c.tengah(muat(toko.catatanKaki, label, A5_W - 2 * m), A5_W / 2f, A5_H - 22f, label)
        }
        doc.selesai()
    }

    fun buatLaporan(
        judulPeriode: String,
        daftar: List<NotaLengkap>,
        ringkasan: RingkasanLaporan,
        toko: InfoToko,
        tujuan: File,
    ): File = PenulisPdf().also { gambarLaporan(judulPeriode, daftar, ringkasan, toko, it) }.tulis(tujuan)

    fun gambarLaporan(
        judulPeriode: String,
        daftar: List<NotaLengkap>,
        ringkasan: RingkasanLaporan,
        toko: InfoToko,
        doc: PenulisHalaman,
    ) {
        val m = 36f
        val kanan = A4_W - m
        val isi = paint(9f)
        val isiTebal = paint(9f, true)
        val label = paint(8.5f, warna = Color.DKGRAY)
        val tb = 16f
        var halNo = 0
        lateinit var c: Canvas
        var y = 0f

        fun halamanBaru() {
            if (halNo > 0) doc.selesai()
            halNo++
            c = doc.mulai(A4_W, A4_H, halNo)
            y = c.kepala(toko, A4_W, m, "LAPORAN PENJUALAN")
            c.kanan("Hal. $halNo", kanan, A4_H - 20f, label)
            c.drawText("Dicetak: ${formatTanggalJam(System.currentTimeMillis())}", m, A4_H - 20f, label)
        }
        fun cekRuang(perlu: Float) { if (y + perlu > A4_H - 40f) halamanBaru() }

        halamanBaru()
        c.drawText("Periode: $judulPeriode", m, y, paint(10f, true))
        y += 20f

        // Kotak ringkasan
        val kotakW = (kanan - m - 20) / 3
        listOf(
            "Jumlah Nota" to ringkasan.jumlahNota.toString(),
            "Barang Terjual" to formatAngka(ringkasan.totalBarang.toLong()),
            "Total Omzet" to formatRupiah(ringkasan.totalOmzet),
        ).forEachIndexed { i, (l, v) ->
            val x = m + i * (kotakW + 10)
            c.drawRoundRect(RectF(x, y, x + kotakW, y + 44), 6f, 6f, Paint().apply { color = ABU })
            c.drawText(l, x + 10, y + 16, label)
            c.drawText(v, x + 10, y + 34, paint(13f, true, BIRU))
        }
        y += 64f

        fun judulBagian(t: String) { cekRuang(40f); c.drawText(t, m, y, paint(11f, true, BIRU)); y += 14f }

        // Rincian nota
        judulBagian("Rincian Nota")
        val kNo = m + 4; val kTgl = m + 26; val kNomor = m + 120; val kPel = m + 240; val kJml = kanan - 110
        fun kepalaTabelNota() {
            c.drawRect(m, y - 11f, kanan, y + 5f, Paint().apply { color = BIRU })
            val p = paint(8.5f, true, Color.WHITE)
            c.drawText("No", kNo, y, p); c.drawText("Tanggal", kTgl, y, p); c.drawText("No. Nota", kNomor, y, p)
            c.drawText("Pelanggan", kPel, y, p); c.kanan("Brg", kJml, y, p); c.kanan("Total", kanan - 4, y, p)
            y += tb
        }
        kepalaTabelNota()
        if (daftar.isEmpty()) { c.drawText("Tidak ada nota pada periode ini.", kTgl, y, label); y += tb }
        daftar.forEachIndexed { i, n ->
            if (y + tb > A4_H - 40f) { halamanBaru(); kepalaTabelNota() }
            if (i % 2 == 1) c.drawRect(m, y - 11f, kanan, y + 5f, Paint().apply { color = ABU })
            c.drawText("${i + 1}", kNo, y, isi)
            c.drawText(formatTanggal(n.nota.tanggal), kTgl, y, isi)
            c.drawText(muat(n.nota.nomor, isi, kPel - kNomor - 6), kNomor, y, isi)
            c.drawText(muat(n.nota.pelanggan, isi, kJml - kPel - 40), kPel, y, isi)
            c.kanan(n.items.sumOf { it.jumlah }.toString(), kJml, y, isi)
            c.kanan(formatAngka(n.nota.total), kanan - 4, y, isi)
            y += tb
        }
        cekRuang(tb)
        c.drawLine(m, y - 9f, kanan, y - 9f, Paint().apply { color = GARIS })
        y += 4f
        c.drawText("TOTAL", kPel, y, isiTebal)
        c.kanan(ringkasan.totalBarang.toString(), kJml, y, isiTebal)
        c.kanan(formatRupiah(ringkasan.totalOmzet), kanan - 4, y, isiTebal)
        y += 28f

        // Rekap barang
        judulBagian("Rekap Barang Terjual")
        val bNama = m + 26; val bJml = kanan - 150
        fun kepalaTabelBarang() {
            c.drawRect(m, y - 11f, kanan, y + 5f, Paint().apply { color = BIRU })
            val p = paint(8.5f, true, Color.WHITE)
            c.drawText("No", kNo, y, p); c.drawText("Nama Barang", bNama, y, p)
            c.kanan("Jumlah", bJml, y, p); c.kanan("Omzet", kanan - 4, y, p)
            y += tb
        }
        kepalaTabelBarang()
        if (ringkasan.barang.isEmpty()) { c.drawText("-", bNama, y, label); y += tb }
        ringkasan.barang.forEachIndexed { i, b ->
            if (y + tb > A4_H - 40f) { halamanBaru(); kepalaTabelBarang() }
            if (i % 2 == 1) c.drawRect(m, y - 11f, kanan, y + 5f, Paint().apply { color = ABU })
            c.drawText("${i + 1}", kNo, y, isi)
            c.drawText(muat(b.nama, isi, bJml - bNama - 50), bNama, y, isi)
            c.kanan(b.jumlah.toString(), bJml, y, isi)
            c.kanan(formatAngka(b.omzet), kanan - 4, y, isi)
            y += tb
        }
        doc.selesai()
    }
}
