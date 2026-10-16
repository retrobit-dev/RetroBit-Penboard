package dev.retrobit.penboard.util

import kotlin.math.floor

/**
 * Matematika papan tulis: pemetaan tinggi tulisan ke indeks baris panduan dan
 * ketebalan goresan dari tekanan stylus. Murni Kotlin supaya bisa diuji tanpa Android.
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

    /**
     * Berapa kali "enter" dipakai supaya teks muncul di baris tempat pengguna menulis.
     *
     * Baris pertama (indeks 0) = teks masuk **di posisi kursor**, baris kedua = **1 enter**
     * (satu baris di bawah kursor), baris ketiga = **2 enter**, dan seterusnya.
     * Dibatasi [maxOffset] supaya tulisan nyasar tidak mengirim deretan enter panjang.
     */
    fun newlineOffset(lineIndex: Int, maxOffset: Int = MAX_LINE_OFFSET): Int =
        lineIndex.coerceIn(0, maxOffset.coerceAtLeast(0))

    /**
     * Nilai tengah (median) dari kumpulan koordinat y goresan.
     *
     * Median dipakai karena ia tidak terpengaruh ekor goresan panjang (mis. ekor huruf
     * di baris paling bawah), jadi baris tempat pengguna menulis bisa ditebak stabil.
     * Larik kosong dianggap 0.
     */
    fun medianY(values: FloatArray): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.copyOf()
        sorted.sort()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2f
        }
    }

    /** Batas jumlah baris yang bisa digeser oleh baris tulisan. */
    const val MAX_LINE_OFFSET = 8

    /**
     * Faktor ketebalan goresan dari tekanan stylus (0..1).
     * Dibatasi 0,75x sampai 1,25x supaya goresan tetap terbaca mesin pengenalan:
     * terlalu tipis atau terlalu tebal membuat tulisan sulit dikenali.
     */
    fun pressureScale(pressure: Float): Float = 0.75f + 0.5f * pressure.coerceIn(0f, 1f)
}
