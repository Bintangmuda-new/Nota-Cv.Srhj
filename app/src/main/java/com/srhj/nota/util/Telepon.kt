package com.srhj.nota.util

/**
 * Normalisasi nomor HP Indonesia ke format internasional tanpa '+', untuk WhatsApp.
 * "0812-3456-789" -> "628123456789", "+62 812..." -> "62812...", "812..." -> "62812...".
 * Mengembalikan null jika nomor terlalu pendek/tidak valid.
 */
fun normalisasiNomorWa(input: String): String? {
    var d = input.filter { it.isDigit() }
    if (d.isEmpty()) return null
    d = when {
        d.startsWith("62") -> d
        d.startsWith("0") -> "62" + d.drop(1)
        d.startsWith("8") -> "62$d"
        else -> d
    }
    return if (d.length in 10..15) d else null
}
