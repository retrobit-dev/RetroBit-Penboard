package dev.retrobit.penboard.util

/**
 * Ukuran tombol grid.
 *
 * Grid dipasang di kanan papan tulis: **3 kolom tetap**, jumlah baris bisa 1 sampai
 * [MAX_ROWS] (jadi 3-18 tombol). Ukurannya diambil dari tinggi papan dibagi jumlah
 * baris, tetapi lebarnya tidak pernah melebihi [MAX_WIDTH_FRACTION] layar supaya papan
 * tulis tetap lebar di kiri.
 */
object GridMath {

    const val COLUMNS = 3
    const val MIN_ROWS = 1
    const val MAX_ROWS = 6

    /** Jumlah baris bawaan. */
    const val ROWS = 3

    const val MAX_WIDTH_FRACTION = 0.34f

    /** Jarak antar tombol (dp) — dipakai pemanggil untuk menghitung spacingPx. */
    const val SPACING_DP = 3f

    /**
     * Sisi tombol (px) = sekecil mungkin antara:
     *  - tinggi papan dibagi [rows] baris (dikurangi jarak antar baris),
     *  - batas lebar [MAX_WIDTH_FRACTION] layar.
     * Bila hasilnya di bawah [minCellPx], ukuran dinaikkan selama masih muat di tinggi papan.
     */
    fun cellSizePx(
        boardHeightPx: Int,
        availableWidthPx: Int,
        spacingPx: Int = 0,
        minCellPx: Int = 0,
        rows: Int = ROWS
    ): Int {
        val safeRows = rows.coerceIn(MIN_ROWS, MAX_ROWS)
        val usableHeight = (boardHeightPx - spacingPx * (safeRows - 1)).coerceAtLeast(1)
        var cell = usableHeight / safeRows

        val budget = (availableWidthPx * MAX_WIDTH_FRACTION).toInt()
        val maxByWidth = (budget - spacingPx * (COLUMNS - 1)) / COLUMNS
        if (maxByWidth in 1 until cell) cell = maxByWidth

        if (minCellPx > 0 && cell < minCellPx) {
            cell = minOf(minCellPx, usableHeight / safeRows)
        }
        return if (cell < 1) 1 else cell
    }

    /**
     * Berapa tombol yang muat dalam **satu baris** di bawah papan tulis.
     *
     * Tombol bawah memakai ukuran yang sama dengan tombol grid ([cellSizePx]), jadi
     * jumlahnya dihitung dari lebar papan tulis yang tersisa. Minimal satu tombol.
     */
    fun columnsFor(availableWidthPx: Int, cellPx: Int, spacingPx: Int = 0): Int {
        if (cellPx <= 0) return 1
        val perRow = (availableWidthPx + spacingPx) / (cellPx + spacingPx)
        return if (perRow < 1) 1 else perRow
    }

    /** Jumlah baris yang dipakai [count] tombol bawah bila satu baris memuat [columns] tombol. */
    fun wrapRows(count: Int, columns: Int): Int {
        val cols = if (columns < 1) 1 else columns
        return if (count <= 0) 0 else (count + cols - 1) / cols
    }

    /** Jumlah baris grid yang perlu ditampilkan untuk [count] tombol. */
    fun rowsFor(count: Int): Int = when {
        count <= 0 -> 1
        count > COLUMNS * MAX_ROWS -> MAX_ROWS
        else -> (count + COLUMNS - 1) / COLUMNS
    }
}
