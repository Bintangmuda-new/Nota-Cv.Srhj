package com.srhj.nota

import com.srhj.nota.data.InfoToko
import com.srhj.nota.data.ItemNota
import com.srhj.nota.data.Nota
import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.print.EscPos
import com.srhj.nota.print.StrukTeks
import com.srhj.nota.util.NomorNota
import com.srhj.nota.util.buatCsvLaporan
import com.srhj.nota.util.formatAngka
import com.srhj.nota.util.formatRupiah
import com.srhj.nota.util.hitungRingkasan
import com.srhj.nota.util.normalisasiNomorWa
import com.srhj.nota.util.parseAngka
import com.srhj.nota.util.namaFileAman
import com.srhj.nota.ui.laporan.JenisPeriode
import com.srhj.nota.ui.laporan.rentangPeriode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LogikaTest {

    private fun contohNota(id: Long = 1, nomor: String = "SRHJ-261117-001", total: Long? = null): NotaLengkap {
        val items = listOf(
            ItemNota(id = 1, notaId = id, nama = "Klakson Denso 12V", jumlah = 2, harga = 85_000),
            ItemNota(id = 2, notaId = id, nama = "Relay Klakson", jumlah = 3, harga = 15_000),
        )
        return NotaLengkap(
            Nota(id = id, nomor = nomor, tanggal = 1_700_000_000_000, pelanggan = "Bengkel Maju; Jaya",
                namaPenerima = "Budi", total = total ?: items.sumOf { it.jumlah * it.harga }),
            items,
        )
    }

    @Test fun formatAngkaRupiah() {
        assertEquals("0", formatAngka(0))
        assertEquals("999", formatAngka(999))
        assertEquals("1.000", formatAngka(1000))
        assertEquals("1.250.000", formatAngka(1_250_000))
        assertEquals("-15.500", formatAngka(-15_500))
        assertEquals("Rp 215.000", formatRupiah(215_000))
    }

    @Test fun parseAngkaBerbagaiFormat() {
        assertEquals(1_250_000L, parseAngka("1.250.000"))
        assertEquals(1_250_000L, parseAngka("Rp 1.250.000"))
        assertEquals(5L, parseAngka("5"))
        assertNull(parseAngka(""))
        assertNull(parseAngka("abc"))
        assertNull(parseAngka("9".repeat(16)))
    }

    @Test fun nomorWhatsApp() {
        assertEquals("6281234567890", normalisasiNomorWa("0812-3456-7890"))
        assertEquals("6281234567890", normalisasiNomorWa("+62 812 3456 7890"))
        assertEquals("6281234567890", normalisasiNomorWa("81234567890"))
        assertNull(normalisasiNomorWa(""))
        assertNull(normalisasiNomorWa("0812"))
    }

    @Test fun nomorNotaUrutPerHari() {
        val tgl = LocalDate.of(2026, 11, 17)
        assertEquals("SRHJ-261117-001", NomorNota.berikutnya("srhj", tgl, emptyList()))
        assertEquals("SRHJ-261117-004", NomorNota.berikutnya("SRHJ", tgl, listOf("SRHJ-261117-001", "SRHJ-261117-003", "SRHJ-261116-009")))
        assertEquals("NOTA-261117-001", NomorNota.berikutnya("  ", tgl, emptyList()))
        assertEquals("SRHJ-261117-1000", NomorNota.berikutnya("SRHJ", tgl, listOf("SRHJ-261117-999")))
    }

    @Test fun namaFile() {
        assertEquals("SRHJ-261117-001", namaFileAman("SRHJ-261117-001"))
        assertEquals("a_b_c", namaFileAman("a/b\\c"))
        assertEquals("dokumen", namaFileAman("///"))
    }

    @Test fun kiriKananSelaluPasLebar() {
        for (w in listOf(32, 48)) {
            assertEquals(w, StrukTeks.kiriKanan("Total", "Rp 1.000", w).length)
            assertEquals(w, StrukTeks.kiriKanan("Nama barang yang sangat panjang sekali sampai tidak muat", "1.250.000", w).length)
        }
    }

    @Test fun bungkusTeks() {
        val hasil = StrukTeks.bungkus("Klakson keong Denso super keras dua belas volt", 16)
        assertTrue(hasil.all { it.length <= 16 })
        assertEquals("Klakson keong Denso super keras dua belas volt", hasil.joinToString(" "))
        assertTrue(StrukTeks.bungkus("A".repeat(40), 16).all { it.length <= 16 })
    }

    @Test fun strukNotaTidakMelebihiLebarKertas() {
        for (mm in listOf(58, 80)) {
            val toko = InfoToko(lebarKertasMm = mm, telepon = "0812 3456 7890")
            val bytes = StrukTeks.nota(contohNota(), toko)
            val teks = String(bytes, Charsets.ISO_8859_1)
            assertTrue(teks.contains("SRHJ-261117-001"))
            assertTrue(teks.contains("TOTAL"))
            assertTrue(teks.contains("215.000"))
            assertTrue(teks.contains("( Budi )"))
            // Buang perintah ESC/POS lalu cek panjang tiap baris teks
            val baris = teks.replace(Regex("\u001B[@Ead].|\u001B@|\u001D!.|\u001DVB."), "").split('\n')
            val terpanjang = baris.maxOf { l -> l.count { it.code in 32..126 } }
            assertTrue("baris terpanjang $terpanjang > ${toko.karakterPerBaris}", terpanjang <= toko.karakterPerBaris)
        }
    }

    @Test fun escPosGambarRaster() {
        val b = EscPos().gambar(10, 2) { x, _ -> x == 0 || x == 9 }.bytes()
        // GS v 0 m xL xH yL yH
        assertEquals(listOf(0x1D, 0x76, 0x30, 0x00, 2, 0, 2, 0), b.take(8).map { it.toInt() and 0xFF })
        // baris pertama: bit 0 & bit 9 -> 0x80, 0x40
        assertEquals(0x80, b[8].toInt() and 0xFF)
        assertEquals(0x40, b[9].toInt() and 0xFF)
        assertEquals(8 + 4 + 1, b.size) // header + 2 baris x 2 byte + LF
    }

    @Test fun escPosAscii() {
        assertEquals("Rp 1.000 - ok ?", EscPos.keAscii("Rp 1.000 – ok €"))
    }

    @Test fun ringkasanLaporan() {
        val a = contohNota(1, "A")
        val b = NotaLengkap(
            Nota(id = 2, nomor = "B", tanggal = 1_700_000_100_000, pelanggan = "Toko B", total = 170_000),
            listOf(ItemNota(nama = "klakson denso 12v ", jumlah = 2, harga = 85_000)),
        )
        val r = hitungRingkasan(listOf(a, b))
        assertEquals(2, r.jumlahNota)
        assertEquals(385_000, r.totalOmzet)
        assertEquals(7, r.totalBarang)
        assertEquals("Klakson Denso 12V", r.barang.first().nama)
        assertEquals(4, r.barang.first().jumlah)
        assertEquals(340_000, r.barang.first().omzet)
        assertEquals(2, r.barang.size)
    }

    @Test fun csvLaporan() {
        val csv = buatCsvLaporan(listOf(contohNota()))
        assertTrue(csv.startsWith("\uFEFFTanggal;No Nota;"))
        val baris = csv.trimEnd().split("\r\n")
        assertEquals(4, baris.size) // header + 2 item + total
        assertTrue(baris[1].contains("\"Bengkel Maju; Jaya\""))
        assertTrue(baris[1].endsWith(";2;85000;170000"))
        assertEquals(";;;;TOTAL;5;;215000", baris[3])
    }

    @Test fun periodeLaporan() {
        val h = LocalDate.of(2026, 11, 17) // Selasa
        val d = LocalDate.of(2026, 1, 1)
        assertEquals(h to h, rentangPeriode(JenisPeriode.HARI_INI, h, d, d))
        assertEquals(LocalDate.of(2026, 11, 16) to h, rentangPeriode(JenisPeriode.MINGGU_INI, h, d, d))
        assertEquals(LocalDate.of(2026, 11, 1) to h, rentangPeriode(JenisPeriode.BULAN_INI, h, d, d))
        assertEquals(LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31), rentangPeriode(JenisPeriode.BULAN_LALU, h, d, d))
        assertEquals(d to h, rentangPeriode(JenisPeriode.PILIH, h, h, d))
    }
}
