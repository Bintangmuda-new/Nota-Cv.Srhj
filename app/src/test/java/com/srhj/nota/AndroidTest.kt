package com.srhj.nota

import android.content.Context
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.srhj.nota.data.InfoToko
import com.srhj.nota.data.ItemNota
import com.srhj.nota.data.Nota
import com.srhj.nota.data.NotaDatabase
import com.srhj.nota.pdf.PdfGenerator
import com.srhj.nota.pdf.PenulisBitmap
import com.srhj.nota.print.PrinterBluetooth
import com.srhj.nota.print.StrukTeks
import com.srhj.nota.util.TandaTangan
import com.srhj.nota.util.hitungRingkasan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/** Keluaran (PDF, PNG) disalin ke sini agar bisa diperiksa secara visual. */
val FOLDER_HASIL = File(System.getProperty("user.dir"), "build/hasil-uji").apply { mkdirs() }

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AndroidTest {
    private lateinit var ctx: Context
    private lateinit var db: NotaDatabase

    @Before fun siapkan() {
        ctx = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, NotaDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tutup() = db.close()

    private fun ttdContoh(): File {
        val g1 = (0..60).map { i -> (20f + i * 6f) to (80f + 40f * sin(i / 6.0).toFloat()) }
        val g2 = (0..40).map { i -> (200f + 50f * cos(i / 6.0).toFloat()) to (60f + i * 2f) }
        return TandaTangan.simpan(ctx, listOf(g1, g2))!!
    }

    @Test fun simpanUbahHapusNota() = runBlocking {
        val dao = db.notaDao()
        val id = dao.simpan(
            Nota(nomor = "SRHJ-261117-001", tanggal = 1000, pelanggan = "A"),
            listOf(ItemNota(nama = "Klakson", jumlah = 2, harga = 50_000), ItemNota(nama = "Relay", jumlah = 1, harga = 15_000)),
        )
        var n = dao.ambil(id)!!
        assertEquals(115_000, n.nota.total)
        assertEquals(2, n.items.size)

        // Ubah: item diganti seluruhnya, total dihitung ulang
        dao.simpan(n.nota.copy(pelanggan = "B"), listOf(ItemNota(nama = "Kabel", jumlah = 3, harga = 10_000)))
        n = dao.ambil(id)!!
        assertEquals("B", n.nota.pelanggan)
        assertEquals(30_000, n.nota.total)
        assertEquals(listOf("Kabel"), n.items.map { it.nama })

        dao.simpan(Nota(nomor = "SRHJ-261117-002", tanggal = 5000, pelanggan = "C"), listOf(ItemNota(nama = "X", jumlah = 1, harga = 1)))
        assertEquals(listOf("SRHJ-261117-001", "SRHJ-261117-002"), dao.nomorDenganAwalan("SRHJ-261117-").sorted())
        assertEquals(1, dao.rentang(0, 2000).first().size)
        assertEquals(2, dao.semua().first().size)
        assertEquals("SRHJ-261117-002", dao.semua().first().first().nota.nomor) // terbaru di atas

        // Hapus: item ikut terhapus (cascade)
        dao.hapus(n.nota)
        assertNull(dao.ambil(id))
        val sisa = db.query("SELECT COUNT(*) FROM item_nota", null).use { it.moveToFirst(); it.getInt(0) }
        assertEquals(1, sisa)
    }

    @Test fun tandaTanganTersimpanDanBisaDicetak() {
        val f = ttdContoh()
        assertTrue(f.exists())
        val bmp = BitmapFactory.decodeFile(f.absolutePath)
        assertNotNull(bmp)
        assertTrue(bmp.width > 100 && bmp.height > 50)
        f.copyTo(File(FOLDER_HASIL, "ttd.png"), overwrite = true)

        val raster = PrinterBluetooth.muatTtdUntukPrinter(f.absolutePath, 384)!!
        assertEquals((384 * 0.6).toInt(), raster.first)
        var hitam = 0
        for (y in 0 until raster.second) for (x in 0 until raster.first) if (raster.third(x, y)) hitam++
        assertTrue("tanda tangan harus punya piksel hitam", hitam > 50)

        val struk = StrukTeks.nota(contoh(f.absolutePath), InfoToko(), raster)
        File(FOLDER_HASIL, "struk58.bin").writeBytes(struk)
        assertTrue(struk.size > 1000)
        assertNull(TandaTangan.simpan(ctx, emptyList()))
    }

    private fun contoh(ttd: String?, jumlahItem: Int = 4) = com.srhj.nota.data.NotaLengkap(
        Nota(id = 1, nomor = "SRHJ-261117-001", tanggal = 1_763_366_400_000, pelanggan = "Bengkel Maju Jaya",
            teleponPelanggan = "0812 3456 7890", namaPenerima = "Budi Santoso", ttdPath = ttd,
            catatan = "Barang sudah dicek dan diterima dalam keadaan baik",
            total = 0),
        (1..jumlahItem).map { i ->
            ItemNota(id = i.toLong(), notaId = 1, nama = listOf("Klakson Denso 12V", "Relay Klakson 4 Pin", "Klakson Keong Bosch Super Tone", "Kabel Set")[i % 4] + if (i > 4) " #$i" else "",
                jumlah = i % 5 + 1, harga = (i % 4 + 1) * 37_500L)
        },
    ).let { it.copy(nota = it.nota.copy(total = it.items.sumOf { x -> x.jumlah * x.harga })) }

    private fun simpanHalaman(nama: String, p: PenulisBitmap) = p.halaman.forEachIndexed { i, b ->
        File(FOLDER_HASIL, "${nama}_hal${i + 1}.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Robolectric tidak mendukung PdfDocument asli, jadi isi halaman PDF digambar ke Bitmap untuk diperiksa. */
    @Test fun halamanPdfNotaDanLaporan() {
        val toko = InfoToko(alamat = "Jl. Gajah Mada No. 10, Jember", telepon = "0812 3456 7890")
        val ttd = ttdContoh().absolutePath

        val nota = PenulisBitmap().also { PdfGenerator.gambarNota(contoh(ttd), toko, it) }
        assertEquals(1, nota.halaman.size)
        simpanHalaman("nota", nota)

        val panjang = PenulisBitmap().also { PdfGenerator.gambarNota(contoh(ttd, 30), toko, it) }
        assertTrue("nota 30 barang harus > 1 halaman", panjang.halaman.size > 1)
        simpanHalaman("nota_panjang", panjang)

        val daftar = (1..45).map { i -> contoh(null, i % 6 + 1).let { it.copy(nota = it.nota.copy(id = i.toLong(), nomor = "SRHJ-2611%02d-%03d".format(i % 17 + 1, i))) } }
        val lap = PenulisBitmap().also { PdfGenerator.gambarLaporan("1 November 2026 - 17 November 2026", daftar, hitungRingkasan(daftar), toko, it) }
        assertTrue(lap.halaman.size >= 2)
        simpanHalaman("laporan", lap)

        val kosong = PenulisBitmap().also { PdfGenerator.gambarLaporan("17 November 2026", emptyList(), hitungRingkasan(emptyList()), toko, it) }
        assertEquals(1, kosong.halaman.size)
        simpanHalaman("laporan_kosong", kosong)
    }
}
