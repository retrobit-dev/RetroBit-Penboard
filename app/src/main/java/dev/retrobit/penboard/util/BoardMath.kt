package dev.retrobit.penboard.util

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Hitungan geometri papan tulis. Semua fungsi murni (tanpa Android) supaya
 * perilaku papan — tinggi baris, geser otomatis, dan geser tampilan — bisa diuji.
 */
object BoardMath {

    const val MIN_VISIBLE_LINES = 3

    /** Berapa baris tulis yang tampil untuk tinggi tertentu. */
    fun visibleLines(availablePx: Float, desiredGapPx: Float): Int {
        if (availablePx <= 0f || desiredGapPx <= 0f) return MIN_VISIBLE_LINES
        return max(MIN_VISIBLE_LINES, (availablePx / desiredGapPx).roundToInt())
    }

    /** Jarak baris sebenarnya (dibulatkan agar pas mengisi tinggi papan). */
    fun gapPx(availablePx: Float, lines: Int): Float = if (lines <= 0) 0f else availablePx / lines

    /** Indeks baris dari sebuah koordinat konten. */
    fun lineIndexOf(contentY: Float, gapPx: Float): Int {
        if (gapPx <= 0f) return 0
        return floor(contentY / gapPx).toInt().coerceAtLeast(0)
    }

    /** Koordinat konten paling atas yang terlihat (0 = paling awal). */
    fun scrollY(contentShiftPx: Float, viewOffsetPx: Float): Float = max(0f, contentShiftPx - viewOffsetPx)

    /** Ubah koordinat konten menjadi koordinat layar. */
    fun toViewY(contentY: Float, contentShiftPx: Float, viewOffsetPx: Float, insetV: Float): Float =
        contentY - scrollY(contentShiftPx, viewOffsetPx) + insetV

    /** Ubah koordinat layar menjadi koordinat konten. */
    fun toContentY(viewY: Float, contentShiftPx: Float, viewOffsetPx: Float, insetV: Float): Float =
        viewY - insetV + scrollY(contentShiftPx, viewOffsetPx)

    /** Batas geser tampilan: dari baris terbaru (0) sampai awal konten. */
    fun clampViewOffset(offsetPx: Float, contentShiftPx: Float): Float =
        offsetPx.coerceIn(0f, max(0f, contentShiftPx))

    /** Berapa baris papan perlu naik agar selalu tersisa satu baris kosong di bawah. */
    fun autoShiftLines(writtenLine: Int, visibleLines: Int): Int {
        if (visibleLines <= 0) return 0
        val lastFreeLine = max(0, visibleLines - 2)
        return (writtenLine.coerceAtLeast(0) - lastFreeLine).coerceAtLeast(0)
    }
}
