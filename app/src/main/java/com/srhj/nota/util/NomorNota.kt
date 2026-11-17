package com.srhj.nota.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Nomor nota otomatis, format: AWALAN-yyMMdd-NNN  (contoh: SRHJ-261117-001).
 * Nomor urut direset setiap hari.
 */
object NomorNota {
    private val FMT = DateTimeFormatter.ofPattern("yyMMdd")

    fun awalanHari(awalan: String, tanggal: LocalDate): String {
        val a = awalan.trim().uppercase().replace(Regex("[^A-Z0-9]"), "").ifEmpty { "NOTA" }
        return "$a-${tanggal.format(FMT)}-"
    }

    /** @param nomorAda nomor-nomor yang sudah dipakai dengan awalan yang sama. */
    fun berikutnya(awalan: String, tanggal: LocalDate, nomorAda: List<String>): String {
        val prefix = awalanHari(awalan, tanggal)
        val maks = nomorAda
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .maxOrNull() ?: 0
        return prefix + (maks + 1).toString().padStart(3, '0')
    }
}
