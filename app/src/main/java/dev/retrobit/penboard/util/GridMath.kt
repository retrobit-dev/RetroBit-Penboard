package dev.retrobit.penboard.util

/**
 * Ukuran tombol grid 3x3.
 *
 * Grid dipasang di kanan papan tulis; ukurannya diambil dari tinggi papan supaya
 * tiga baris tombol sejajar dengan papan, tetapi lebarnya tidak pernah melebihi
 * [MAX_WIDTH_FRACTION] layar agar papan tulis tetap lebar di kiri.
 */
object GridMath {

    const val COLUMNS = 3
    const val ROWS = 3
    const val MAX_WIDTH_FRACTION = 0.34f

    /** Jarak antar tombol (dp) — dipakai pemanggil untuk menghitung [spacingPx]. */
    const val SPACING_DP = 3f

    /**
     * Sisi tombol (px) = sekecil mungkin antara:
     *  - tinggi papan dibagi tiga baris (dikurangi jarak antar baris),
     *  - batas lebar [MAX_WIDTH_FRACTION] layar.
     * Bila hasilnya di bawah [minCellPx], ukuran dinaikkan selama masih muat di tinggi papan.
     */
    fun cellSizePx(
        boardHeightPx: Int,
        availableWidthPx: Int,
        spacingPx: Int = 0,
        minCellPx: Int = 0
    ): Int {
        val usableHeight = (boardHeightPx - spacingPx * (ROWS - 1)).coerceAtLeast(1)
        var cell = usableHeight / ROWS

        val budget = (availableWidthPx * MAX_WIDTH_FRACTION).toInt()
        val maxByWidth = (budget - spacingPx * (COLUMNS - 1)) / COLUMNS
        if (maxByWidth in 1 until cell) cell = maxByWidth

        if (minCellPx > 0 && cell < minCellPx) {
            cell = minOf(minCellPx, usableHeight / ROWS)
        }
        return if (cell < 1) 1 else cell
    }

    /** Jumlah baris grid yang perlu ditampilkan untuk [count] tombol. */
    fun rowsFor(count: Int): Int = when {
        count <= 0 -> 1
        count > COLUMNS * ROWS -> ROWS
        else -> (count + COLUMNS - 1) / COLUMNS
    }
}
