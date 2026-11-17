package com.srhj.nota.print

import com.srhj.nota.data.InfoToko
import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.data.subtotal
import com.srhj.nota.util.RingkasanLaporan
import com.srhj.nota.util.formatAngka
import com.srhj.nota.util.formatTanggal
import com.srhj.nota.util.formatTanggalJam

/** Penyusun teks struk dengan lebar karakter tetap (32 untuk 58mm, 48 untuk 80mm). */
object StrukTeks {

    fun garis(lebar: Int, c: Char = '-') = c.toString().repeat(lebar)

    /** Teks kiri & kanan dalam satu baris; jika tidak muat, kiri dipotong. */
    fun kiriKanan(kiri: String, kanan: String, lebar: Int): String {
        val ruangKiri = (lebar - kanan.length - 1).coerceAtLeast(0)
        val k = if (kiri.length > ruangKiri) kiri.take(ruangKiri) else kiri
        return k + " ".repeat((lebar - k.length - kanan.length).coerceAtLeast(1)) + kanan
    }

    /** Pecah teks panjang menjadi beberapa baris per kata. */
    fun bungkus(teks: String, lebar: Int): List<String> {
        val hasil = mutableListOf<String>()
        for (paragraf in teks.split('\n')) {
            var baris = StringBuilder()
            for (kata0 in paragraf.split(' ').filter { it.isNotEmpty() }) {
                var kata = kata0
                while (kata.length > lebar) {
                    if (baris.isNotEmpty()) { hasil += baris.toString(); baris = StringBuilder() }
                    hasil += kata.take(lebar); kata = kata.drop(lebar)
                }
                if (baris.isEmpty()) baris.append(kata)
                else if (baris.length + 1 + kata.length <= lebar) baris.append(' ').append(kata)
                else { hasil += baris.toString(); baris = StringBuilder(kata) }
            }
            hasil += baris.toString()
        }
        return hasil
    }

    fun kepala(p: EscPos, toko: InfoToko) {
        val w = toko.karakterPerBaris
        p.rataTengah().tebal(true)
        bungkus(toko.namaToko.uppercase(), w).forEach { p.baris(it) }
        p.tebal(false)
        if (toko.alamat.isNotBlank()) bungkus(toko.alamat, w).forEach { p.baris(it) }
        if (toko.telepon.isNotBlank()) p.baris("Telp/WA: ${toko.telepon}")
        p.rataKiri().baris(garis(w, '='))
    }

    /**
     * @param ttd gambar tanda tangan (lebar, tinggi, fungsi piksel hitam), boleh null.
     */
    fun nota(
        nota: NotaLengkap,
        toko: InfoToko,
        ttd: Triple<Int, Int, (Int, Int) -> Boolean>? = null,
    ): ByteArray {
        val w = toko.karakterPerBaris
        val n = nota.nota
        val p = EscPos().init()
        kepala(p, toko)
        p.rataTengah().tebal(true).baris("NOTA PENJUALAN").tebal(false).rataKiri()
        p.baris(kiriKanan("No", n.nomor, w))
        p.baris(kiriKanan("Tanggal", formatTanggalJam(n.tanggal), w))
        bungkus("Pelanggan: ${n.pelanggan}", w).forEach { p.baris(it) }
        p.baris(garis(w))
        for (item in nota.items) {
            bungkus(item.nama, w).forEach { p.baris(it) }
            p.baris(kiriKanan("  ${item.jumlah} x ${formatAngka(item.harga)}", formatAngka(item.subtotal()), w))
        }
        p.baris(garis(w))
        p.tebal(true).baris(kiriKanan("TOTAL", "Rp " + formatAngka(n.total), w)).tebal(false)
        p.baris(kiriKanan("Jumlah barang", nota.items.sumOf { it.jumlah }.toString(), w))
        if (n.catatan.isNotBlank()) {
            p.baris(garis(w))
            bungkus("Catatan: ${n.catatan}", w).forEach { p.baris(it) }
        }
        p.baris(garis(w))
        p.rataTengah().baris("Penerima,")
        if (ttd != null) {
            p.gambar(ttd.first, ttd.second, ttd.third)
        } else {
            p.baris().baris().baris()
        }
        p.baris("( ${n.namaPenerima.ifBlank { "................" }} )")
        p.baris()
        if (toko.catatanKaki.isNotBlank()) bungkus(toko.catatanKaki, w).forEach { p.baris(it) }
        p.rataKiri().feed(3).potong()
        return p.bytes()
    }

    fun laporan(
        judulPeriode: String,
        daftar: List<NotaLengkap>,
        ringkasan: RingkasanLaporan,
        toko: InfoToko,
    ): ByteArray {
        val w = toko.karakterPerBaris
        val p = EscPos().init()
        kepala(p, toko)
        p.rataTengah().tebal(true).baris("LAPORAN PENJUALAN").tebal(false)
        bungkus(judulPeriode, w).forEach { p.baris(it) }
        p.rataKiri().baris(garis(w))
        p.baris(kiriKanan("Jumlah nota", ringkasan.jumlahNota.toString(), w))
        p.baris(kiriKanan("Barang terjual", ringkasan.totalBarang.toString(), w))
        p.tebal(true).baris(kiriKanan("TOTAL OMZET", "Rp " + formatAngka(ringkasan.totalOmzet), w)).tebal(false)
        p.baris(garis(w))
        p.tebal(true).baris("RINCIAN NOTA").tebal(false)
        for (n in daftar) {
            p.baris(kiriKanan(n.nota.nomor, formatAngka(n.nota.total), w))
            p.baris("  " + formatTanggal(n.nota.tanggal) + " " + n.nota.pelanggan.take(w - 13))
        }
        if (ringkasan.barang.isNotEmpty()) {
            p.baris(garis(w))
            p.tebal(true).baris("REKAP BARANG").tebal(false)
            for (b in ringkasan.barang) {
                bungkus(b.nama, w).forEach { p.baris(it) }
                p.baris(kiriKanan("  ${b.jumlah} pcs", formatAngka(b.omzet), w))
            }
        }
        p.baris(garis(w))
        p.rataTengah().baris("Dicetak: " + formatTanggalJam(System.currentTimeMillis()))
        p.rataKiri().feed(3).potong()
        return p.bytes()
    }

    fun tesCetak(toko: InfoToko): ByteArray {
        val w = toko.karakterPerBaris
        val p = EscPos().init()
        kepala(p, toko)
        p.rataTengah().baris("TES PRINTER BERHASIL").baris("Kertas ${toko.lebarKertasMm}mm / $w karakter")
        p.rataKiri().baris("1234567890".repeat(5).take(w))
        p.feed(3).potong()
        return p.bytes()
    }
}
