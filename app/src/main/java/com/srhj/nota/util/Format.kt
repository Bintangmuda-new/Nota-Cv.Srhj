package com.srhj.nota.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_ID = Locale.forLanguageTag("id-ID")
private val FMT_TANGGAL = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_ID)
private val FMT_TANGGAL_JAM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE_ID)
private val FMT_TANGGAL_PANJANG = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE_ID)

/** 1250000 -> "1.250.000" (pemisah ribuan titik, gaya Indonesia). */
fun formatAngka(nilai: Long): String {
    val negatif = nilai < 0
    val s = kotlin.math.abs(nilai).toString()
    val sb = StringBuilder()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(c)
    }
    return if (negatif) "-$sb" else sb.toString()
}

/** 1250000 -> "Rp 1.250.000". */
fun formatRupiah(nilai: Long): String = "Rp " + formatAngka(nilai)

/** "1.250.000" / "1250000" / "Rp 1.250.000" -> 1250000. Mengembalikan null bila kosong/tidak valid. */
fun parseAngka(teks: String): Long? {
    val digit = teks.filter { it.isDigit() }
    if (digit.isEmpty() || digit.length > 15) return null
    return digit.toLong()
}

fun keLocalDate(millis: Long, zona: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

fun formatTanggal(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zona).format(FMT_TANGGAL)

fun formatTanggalJam(millis: Long, zona: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zona).format(FMT_TANGGAL_JAM)

fun formatTanggalPanjang(tanggal: LocalDate): String = tanggal.format(FMT_TANGGAL_PANJANG)

fun awalHari(tanggal: LocalDate, zona: ZoneId = ZoneId.systemDefault()): Long =
    tanggal.atStartOfDay(zona).toInstant().toEpochMilli()

fun akhirHari(tanggal: LocalDate, zona: ZoneId = ZoneId.systemDefault()): Long =
    tanggal.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1

/** Nama file aman: hanya huruf, angka, '-', '_'. */
fun namaFileAman(teks: String): String =
    teks.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "dokumen" }
