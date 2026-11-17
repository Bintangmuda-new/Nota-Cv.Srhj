package com.srhj.nota.util

import com.srhj.nota.data.NotaLengkap
import com.srhj.nota.data.subtotal

data class BarangTerjual(val nama: String, val jumlah: Int, val omzet: Long)

data class RingkasanLaporan(
    val jumlahNota: Int,
    val totalOmzet: Long,
    val totalBarang: Int,
    /** Diurutkan dari omzet terbesar. */
    val barang: List<BarangTerjual>,
)

fun hitungRingkasan(daftar: List<NotaLengkap>): RingkasanLaporan {
    val semuaItem = daftar.flatMap { it.items }
    val perBarang = semuaItem
        .groupBy { it.nama.trim().lowercase() }
        .map { (_, items) ->
            BarangTerjual(
                nama = items.first().nama.trim(),
                jumlah = items.sumOf { it.jumlah },
                omzet = items.sumOf { it.subtotal() },
            )
        }
        .sortedWith(compareByDescending<BarangTerjual> { it.omzet }.thenBy { it.nama })
    return RingkasanLaporan(
        jumlahNota = daftar.size,
        totalOmzet = daftar.sumOf { it.nota.total },
        totalBarang = semuaItem.sumOf { it.jumlah },
        barang = perBarang,
    )
}

/**
 * CSV laporan untuk dibuka di Excel / Google Sheets.
 * Memakai pemisah ';' (standar Excel berbahasa Indonesia) dan BOM UTF-8.
 */
private const val BOM_UTF8 = '\uFEFF'

fun buatCsvLaporan(daftar: List<NotaLengkap>): String {
    fun sel(s: String): String =
        if (s.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    val sb = StringBuilder().append(BOM_UTF8)
    sb.append("Tanggal;No Nota;Pelanggan;Penerima;Barang;Jumlah;Harga;Subtotal\r\n")
    for (n in daftar) {
        for (item in n.items) {
            sb.append(
                listOf(
                    formatTanggalJam(n.nota.tanggal),
                    n.nota.nomor,
                    n.nota.pelanggan,
                    n.nota.namaPenerima,
                    item.nama,
                    item.jumlah.toString(),
                    item.harga.toString(),
                    item.subtotal().toString(),
                ).joinToString(";") { sel(it) },
            ).append("\r\n")
        }
    }
    val r = hitungRingkasan(daftar)
    sb.append(";;;;TOTAL;${r.totalBarang};;${r.totalOmzet}\r\n")
    return sb.toString()
}
