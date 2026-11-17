package com.srhj.nota.print

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/** Pembangun perintah ESC/POS untuk printer thermal (58mm / 80mm). */
class EscPos {
    private val out = ByteArrayOutputStream()
    private val charset: Charset = Charsets.ISO_8859_1

    fun bytes(): ByteArray = out.toByteArray()

    private fun raw(vararg b: Int) = apply { b.forEach { out.write(it) } }

    fun init() = raw(0x1B, 0x40)

    fun rataKiri() = raw(0x1B, 0x61, 0)
    fun rataTengah() = raw(0x1B, 0x61, 1)
    fun rataKanan() = raw(0x1B, 0x61, 2)

    fun tebal(aktif: Boolean) = raw(0x1B, 0x45, if (aktif) 1 else 0)

    /** Ukuran huruf ganda (lebar & tinggi). */
    fun besar(aktif: Boolean) = raw(0x1D, 0x21, if (aktif) 0x11 else 0x00)

    fun teks(s: String) = apply { out.write(keAscii(s).toByteArray(charset)) }

    fun baris(s: String = "") = teks(s).raw(0x0A)

    fun feed(n: Int) = raw(0x1B, 0x64, n.coerceIn(0, 255))

    /** Potong kertas (diabaikan oleh printer tanpa pemotong). */
    fun potong() = raw(0x1D, 0x56, 0x42, 0x00)

    /**
     * Cetak gambar hitam-putih dengan perintah raster GS v 0.
     * @param hitam fungsi (x, y) -> true bila piksel dicetak hitam.
     */
    fun gambar(lebar: Int, tinggi: Int, hitam: (Int, Int) -> Boolean) = apply {
        require(lebar > 0 && tinggi > 0)
        val bytePerBaris = (lebar + 7) / 8
        raw(0x1D, 0x76, 0x30, 0x00)
        raw(bytePerBaris and 0xFF, (bytePerBaris shr 8) and 0xFF)
        raw(tinggi and 0xFF, (tinggi shr 8) and 0xFF)
        for (y in 0 until tinggi) {
            for (bx in 0 until bytePerBaris) {
                var b = 0
                for (bit in 0 until 8) {
                    val x = bx * 8 + bit
                    if (x < lebar && hitam(x, y)) b = b or (0x80 shr bit)
                }
                out.write(b)
            }
        }
        raw(0x0A)
    }

    companion object {
        /** Printer thermal umumnya hanya mendukung ASCII; ganti karakter lain agar tidak jadi simbol aneh. */
        fun keAscii(s: String): String = buildString {
            for (c in s) {
                append(
                    when {
                        c == '\n' || c.code in 32..126 -> c
                        c == '–' || c == '—' -> '-'
                        c == '‘' || c == '’' -> '\''
                        c == '“' || c == '”' -> '"'
                        c == ' ' -> ' '
                        else -> '?'
                    },
                )
            }
        }
    }
}
