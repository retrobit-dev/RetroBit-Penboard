package dev.retrobit.penboard.util

import kotlin.math.max
import kotlin.math.min

/**
 * Ukuran tombol untuk grid 3x3 (tata letak "papan di kiri, tombol di kanan").
 *
 * Tujuannya: papan tulis memakai lebar sebanyak mungkin di kiri, sedangkan grid tombol
 * di kanan tinggal seperlunya saja (tidak lebih dari [MAX_WIDTH_FRACTION] lebar layar).
 * Fungsi murni tanpa Android agar bisa diuji.
 */
object GridMath {

    const val COLUMNS = 3
    const val ROWS = 3

    /** Bagian lebar layar yang boleh dipakai grid tombol. */
    const val MAX_WIDTH_FRACTION = 0.34f

    /**
     * Ukuran satu tombol grid (piksel).
     *
     * @param boardHeightPx tinggi papan tulis (grid dibuat sepadan tinggi papan)
     * @param availableWidthPx lebar layar yang tersedia
     * @param rows jumlah baris grid yang dipakai (1..3)
     * @param spacingPx jarak antar tombol
     * @param minCellPx ukuran terkecil agar tombol tetap bisa ditekan
     */
    fun cellSizePx(
        boardHeightPx: Int,
        availableWidthPx: Int,
        rows: Int = ROWS,
        spacingPx: Int = 0,
        minCellPx: Int = 1
    ): Int {
        if (boardHeightPx <= 0) return max(1, minCellPx)
        val rowCount = rows.coerceIn(1, ROWS)
        val byHeight = (boardHeightPx - spacingPx * (rowCount - 1)) / rowCount
        val gridBudget = (availableWidthPx * MAX_WIDTH_FRACTION).toInt()
        val byWidth = (gridBudget - spacingPx * (COLUMNS - 1)) / COLUMNS
        val chosen = min(byHeight, max(byWidth, minCellPx))
        return max(1, min(chosen, byHeight))
    }

    /** Berapa baris grid yang terpakai untuk sejumlah tombol. */
    fun rowsFor(buttonCount: Int): Int = ((buttonCount.coerceIn(1, COLUMNS * ROWS) + COLUMNS - 1) / COLUMNS)
        .coerceIn(1, ROWS)
}
