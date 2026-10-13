package dev.retrobit.penboard.util

import kotlin.math.floor

/**
 * Matematika papan tulis: pemetaan tinggi tulisan ke indeks baris panduan.
 * Murni Kotlin supaya bisa diuji tanpa Android.
 */
object BoardMath {

    /**
     * Indeks baris panduan untuk koordinat y.
     * [lines] <= 0 berarti tidak dibatasi (dipakai saat tinggi tampilan belum diketahui).
     */
    fun lineIndexOf(y: Float, lineGapPx: Float, lines: Int = 0): Int {
        if (lineGapPx <= 0f) return 0
        val index = floor(y / lineGapPx).toInt()
        if (lines <= 0) return if (index < 0) 0 else index
        return index.coerceIn(0, lines - 1)
    }

    /** Jumlah baris panduan yang muat pada tinggi tertentu (minimal 1). */
    fun lineCount(heightPx: Int, lineGapPx: Float): Int {
        if (lineGapPx <= 0f) return 1
        val count = floor(heightPx / lineGapPx).toInt()
        return if (count < 1) 1 else count
    }
}
