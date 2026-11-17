package com.srhj.nota.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.io.File

/** Satu goresan tanda tangan berupa titik-titik (x, y) dalam piksel kanvas. */
typealias Goresan = List<Pair<Float, Float>>

object TandaTangan {
    /**
     * Gambar goresan ke PNG latar putih, dipotong rapat ke area yang tercoret.
     * @return file PNG, atau null bila tidak ada goresan.
     */
    fun simpan(context: Context, goresan: List<Goresan>, ketebalan: Float = 6f): File? {
        val titik = goresan.flatten()
        if (titik.size < 2) return null
        val pad = 16f
        val minX = titik.minOf { it.first } - pad
        val minY = titik.minOf { it.second } - pad
        val maxX = titik.maxOf { it.first } + pad
        val maxY = titik.maxOf { it.second } + pad
        val w = (maxX - minX).toInt().coerceIn(1, 2000)
        val h = (maxY - minY).toInt().coerceIn(1, 1000)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = ketebalan
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        for (g in goresan) {
            if (g.isEmpty()) continue
            val path = Path()
            path.moveTo(g[0].first - minX, g[0].second - minY)
            if (g.size == 1) path.lineTo(g[0].first - minX + 0.1f, g[0].second - minY)
            for (i in 1 until g.size) path.lineTo(g[i].first - minX, g[i].second - minY)
            canvas.drawPath(path, paint)
        }
        val dir = File(context.filesDir, "ttd").apply { mkdirs() }
        val file = File(dir, "ttd_${System.currentTimeMillis()}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }
}
