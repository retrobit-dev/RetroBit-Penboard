package dev.retrobit.penboard.util

import kotlin.math.roundToInt

/**
 * Ukuran dan susunan papan Penboard (murni Kotlin, mudah diuji).
 *
 * Modelnya:
 * - **Tombol samping selalu persegi** (panjang = lebar): tinggi tiap baris tombol
 *   selalu sama dengan sisi tombolnya.
 * - **Area tulis mengikuti tombol**: tinggi papan = `baris x sisi tombol` +
 *   jarak antar baris, jadi papan dan tombol selalu sejajar atas-bawah.
 * - Papan (panel) bisa diubah lebarnya pengguna; lebar area tulis adalah sisa
 *   setelah kolom tombol.
 */
object GridMath {

    const val DEFAULT_COLUMNS = 3
    const val MIN_COLUMNS = 1
    const val MAX_COLUMNS = 6

    const val MIN_ROWS = 1
    const val MAX_ROWS = 6

    /** Baris area tulis: bawaan 3 baris, bisa ditambah sampai 6. */
    const val MIN_LINES = 1
    const val MAX_LINES = 6
    const val DEFAULT_LINES = 3

    /** Jumlah baris bawaan. */
    const val ROWS = 3

    /** Batas ukuran tombol (dp) saat diatur manual. */
    const val MIN_CELL_DP = 32
    const val MAX_CELL_DP = 160

    /** Sisi tombol terkecil yang masih nyaman ditekan (dp). */
    const val MIN_TOUCH_DP = 34f

    /** Jarak antar tombol (dp) — dipakai pemanggil untuk menghitung spacingPx. */
    const val SPACING_DP = 3f

    /** Bagian lebar yang paling banyak dipakai kolom tombol saat ukurannya otomatis. */
    const val MAX_WIDTH_FRACTION = 0.34f

    /**
     * Bagian lebar panel (isi, tanpa bingkai) yang **dijamin** untuk area tulis.
     *
     * Naikkan angkanya bila area tulis terasa sempit: kolom tombol mengambil sisa
     * ruang, dan ukuran tombol otomatis ikut mengecil supaya tetap muat. Nilai ini
     * juga yang mencegah tombol samping terpotong saat ukuran tombol diatur manual.
     */
    const val MIN_BOARD_WIDTH_FRACTION = 0.40f
    const val MAX_BOARD_WIDTH_FRACTION = 0.80f
    const val DEFAULT_BOARD_WIDTH_FRACTION = 0.60f

    /** Lebar tombol bawah: 1x (persegi, sama dengan tombol samping) sampai 4x. */
    const val MIN_WIDTH_MULTIPLIER = 1f
    const val MAX_WIDTH_MULTIPLIER = 4f

    /** Tinggi tombol bawah dibanding tombol samping: 0,6x sampai 2x. */
    const val MIN_HEIGHT_MULTIPLIER = 0.6f
    const val MAX_HEIGHT_MULTIPLIER = 2f

    fun columnsOr(value: Int): Int = value.coerceIn(MIN_COLUMNS, MAX_COLUMNS)

    /** Jumlah baris area tulis (1-6). Baris tombol samping selalu mengikuti angka ini. */
    fun linesOr(value: Int): Int = value.coerceIn(MIN_LINES, MAX_LINES)

    /**
     * Tinggi area tulis (px) untuk [lines] baris. Satu baris = satu jarak garis panduan,
     * jadi area tulis bawaan 3 baris tingginya 3 x jarak baris.
     */
    fun boardHeightForLines(lines: Int, lineGapPx: Int): Int =
        linesOr(lines) * lineGapPx.coerceAtLeast(1)

    /**
     * Jarak baris yang **benar-benar dipakai** setelah ruang keyboard diperhitungkan.
     *
     * Kalau layar tidak cukup untuk seluruh baris, jarak barisnya dipangkas (tidak pernah
     * di bawah 1 px) supaya papan tetap muat dan tidak ada yang terpotong. [availablePx] 0
     * berarti tinggi belum diketahui, jadi jarak yang diminta dipakai apa adanya.
     */
    fun fitLineGapPx(requestedPx: Int, availablePx: Int, lines: Int, fixedPx: Int): Int {
        val safeLines = linesOr(lines)
        if (availablePx <= 0) return requestedPx
        val room = availablePx - fixedPx
        if (room <= 0) return 1
        return minOf(requestedPx, room / safeLines).coerceAtLeast(1)
    }

    fun rowsOr(value: Int): Int = value.coerceIn(MIN_ROWS, MAX_ROWS)

    /** Jumlah tombol yang bisa dipasang pada susunan [columns] x [rows]. */
    fun slots(columns: Int, rows: Int): Int = columnsOr(columns) * rowsOr(rows)

    /**
     * Sisi tombol (px) yang benar-benar dipakai.
     *
     * Diambil yang paling ketat antara:
     *  - **tinggi**: papan dibagi jumlah baris (dikurangi jarak antar baris),
     *  - **lebar**: sisa lebar setelah lebar minimum area tulis ([minBoardWidthPx]),
     *    atau batas [MAX_WIDTH_FRACTION] bila [minBoardWidthPx] nol,
     *  - **ukuran pilihan pengguna** ([cellDp] > 0),
     * lalu dinaikkan ke [minCellPx] selama masih muat di tinggi papan.
     */
    fun cellSizePx(
        boardHeightPx: Int,
        availableWidthPx: Int,
        spacingPx: Int = 0,
        minCellPx: Int = 0,
        rows: Int = ROWS,
        columns: Int = DEFAULT_COLUMNS,
        cellDp: Int = 0,
        density: Float = 1f,
        minBoardWidthPx: Int = 0
    ): Int {
        val safeRows = rowsOr(rows)
        val safeColumns = columnsOr(columns)
        val fromHeight =
            ((boardHeightPx - spacingPx * (safeRows - 1)) / safeRows).coerceAtLeast(1)

        val budget = if (minBoardWidthPx > 0) {
            (availableWidthPx - minBoardWidthPx - spacingPx).coerceAtLeast(1)
        } else {
            (availableWidthPx * MAX_WIDTH_FRACTION).toInt()
        }
        val fromWidth =
            ((budget - spacingPx * (safeColumns - 1)) / safeColumns).coerceAtLeast(1)

        val explicit = if (cellDp > 0) (cellDp * density).toInt() else 0
        val desired = if (explicit > 0) explicit else fromWidth
        val fitting = minOf(desired, fromWidth, fromHeight).coerceAtLeast(1)

        // Ukuran minimum hanya dipakai selama masih muat di tinggi papan.
        return maxOf(fitting, minOf(minCellPx, fromHeight)).coerceAtLeast(1)
    }

    /** Tinggi area tulis (px) = tinggi tombol di semua baris + jarak antar baris. */
    fun boardHeightPx(rows: Int, cellPx: Int, spacingPx: Int = 0): Int {
        val safeRows = rowsOr(rows)
        return safeRows * cellPx + spacingPx * (safeRows - 1)
    }

    /** Lebar kolom tombol (px) untuk [columns] tombol selebar [cellPx]. */
    fun gridWidthPx(columns: Int, cellPx: Int, spacingPx: Int = 0): Int {
        val safeColumns = columnsOr(columns)
        return safeColumns * cellPx + spacingPx * (safeColumns - 1)
    }

    /** Lebar area tulis (px): sisa lebar panel setelah kolom tombol dan jarak tepinya. */
    fun boardWidthPx(
        panelWidthPx: Int,
        paddingPx: Int,
        gridWidthPx: Int,
        spacingPx: Int = 0
    ): Int = (panelWidthPx - 2 * paddingPx - gridWidthPx - spacingPx).coerceAtLeast(1)

    /** Pengaturan lebar area tulis dipangkas ke rentang 40%-80%. */
    fun boardFractionOr(value: Float): Float =
        if (value.isNaN()) DEFAULT_BOARD_WIDTH_FRACTION
        else value.coerceIn(MIN_BOARD_WIDTH_FRACTION, MAX_BOARD_WIDTH_FRACTION)

    /**
     * Lebar area tulis yang **dijamin** (px) dari bagian panel yang bisa dipakai.
     *
     * Kolom tombol tidak boleh lebih sempit dari [gridMinWidthPx] (semua tombol
     * selebar [minCell][MIN_TOUCH_DP]), jadi permintaan yang terlalu besar dipangkas
     * dulu. Dengan begitu area tulis tidak pernah menekan kolom tombol sampai
     * terpotong di tepi panel.
     */
    fun boardWidthFor(
        usableWidthPx: Int,
        fraction: Float,
        minBoardWidthPx: Int,
        gridMinWidthPx: Int,
        spacingPx: Int = 0
    ): Int {
        val usable = usableWidthPx.coerceAtLeast(1)
        val maxBoard = (usable - spacingPx - gridMinWidthPx).coerceAtLeast(1)
        val lower = minOf(minBoardWidthPx.coerceAtLeast(1), maxBoard)
        val wanted = (usable * boardFractionOr(fraction)).toInt().coerceAtLeast(1)
        return wanted.coerceIn(lower, maxBoard)
    }

    /**
     * Sisi tombol terbesar (px) yang masih menyisakan [boardWidthPx] untuk area tulis.
     *
     * Dipakai mengatur ukuran tombol manual: permintaan pengguna yang terlalu besar
     * dipangkas ke angka ini supaya **seluruh kolom tombol tetap terlihat**.
     */
    fun maxCellByWidthPx(
        usableWidthPx: Int,
        boardWidthPx: Int,
        columns: Int,
        spacingPx: Int = 0
    ): Int {
        val safeColumns = columnsOr(columns)
        val room = (usableWidthPx - boardWidthPx - spacingPx).coerceAtLeast(1)
        return ((room - spacingPx * (safeColumns - 1)) / safeColumns).coerceAtLeast(1)
    }

    /**
     * Berapa tombol yang muat dalam **satu baris** di bawah papan tulis.
     *
     * Tombol bawah memakai tinggi yang sama dengan tombol samping, jadi jumlahnya
     * dihitung dari lebar papan tulis yang tersisa (bukan dari lebar layar).
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
    fun rowsFor(count: Int, columns: Int = DEFAULT_COLUMNS): Int {
        val cols = columnsOr(columns)
        return when {
            count <= 0 -> 1
            count > cols * MAX_ROWS -> MAX_ROWS
            else -> (count + cols - 1) / cols
        }
    }

    /** Lebar satu tombol bawah (px): sisi tombol samping x pengali lebarnya. */
    fun buttonWidthPx(cellPx: Int, widthMultiplier: Float): Int =
        (cellPx * widthMultiplier.coerceIn(MIN_WIDTH_MULTIPLIER, MAX_WIDTH_MULTIPLIER))
            .roundToInt()
            .coerceAtLeast(1)

    /** Tinggi satu tombol bawah (px): sisi tombol samping x pengali tingginya. */
    fun buttonHeightPx(cellPx: Int, heightMultiplier: Float): Int =
        (cellPx * heightMultiplier.coerceIn(MIN_HEIGHT_MULTIPLIER, MAX_HEIGHT_MULTIPLIER))
            .roundToInt()
            .coerceAtLeast(1)

    /**
    /**
     * Jarak kiri tiap tombol bawah pada **satu baris** supaya baris itu memenuhi
     * seluruh lebar panel (tombol tetap sebesar yang dipilih; sisa ruang dibagi rata
     * menjadi jarak antar tombol).
     *
     * Indeks pertama selalu 0 (menempel di tepi kiri). Baris berisi satu tombol tidak
     * punya sisa ruang untuk dibagi, jadi tombolnya tetap di kiri.
     */
    fun bottomRowGaps(
        widthsPx: List<Int>,
        availableWidthPx: Int,
        spacingPx: Int = 0
    ): List<Int> {
        if (widthsPx.isEmpty()) return emptyList()
        val used = widthsPx.sum() + spacingPx * (widthsPx.size - 1)
        val gaps = widthsPx.size - 1
        val extra = if (gaps > 0) {
            (availableWidthPx - used).coerceAtLeast(0) / gaps
        } else {
            0
        }
        return widthsPx.mapIndexed { index, _ -> if (index == 0) 0 else spacingPx + extra }
    }

    /** Tinggi tiap baris tombol bawah: baris setinggi tombol tertinggi di baris itu
     * (tombolnya sendiri dipusatkan secara vertikal saat dipasang).
     */
    fun bottomRowHeights(
        rows: List<List<Int>>,
        cellPx: Int,
        heightMultipliers: List<Float>
    ): List<Int> = rows.map { indexes ->
        indexes.maxOf { index ->
            buttonHeightPx(cellPx, heightMultipliers.getOrElse(index) { 1f })
        }
    }

    /** Tinggi seluruh bilah tombol bawah (px), termasuk jarak antar barisnya. */
    fun bottomBarHeightPx(rowHeights: List<Int>, spacingPx: Int): Int {
        if (rowHeights.isEmpty()) return 0
        return rowHeights.sum() + spacingPx * (rowHeights.size - 1)
    }

    /**
     * Susunan tombol bawah: indeks tombol untuk setiap baris.
     *
     * Lebar tiap tombol bisa berbeda ([widthMultiplier]), jadi pembungkusannya dihitung
     * berurutan: tombol berikutnya pindah ke baris baru begitu lebar papan tulis tidak
     * cukup lagi. Hasilnya dipakai bersama oleh penghitungan tinggi panel dan pemasangan
     * tombol, sehingga keduanya tidak pernah berbeda (tidak ada tombol yang terpotong).
     */
    fun bottomRowIndexes(
        count: Int,
        cellPx: Int,
        widthMultipliers: List<Float>,
        availableWidthPx: Int,
        spacingPx: Int = 0
    ): List<List<Int>> {
        if (count <= 0) return emptyList()
        val rows = mutableListOf<MutableList<Int>>()
        var current = mutableListOf<Int>()
        var used = 0
        for (index in 0 until count) {
            val width = buttonWidthPx(cellPx, widthMultipliers.getOrElse(index) { 1f })
            val needed = if (current.isEmpty()) width else used + spacingPx + width
            if (current.isNotEmpty() && needed > availableWidthPx) {
                rows.add(current)
                current = mutableListOf()
                used = width
            } else {
                used = needed
            }
            current.add(index)
        }
        if (current.isNotEmpty()) rows.add(current)
        return rows
    }

    /** Jumlah baris tombol bawah untuk [count] tombol dengan lebar masing-masing. */
    fun bottomRows(
        count: Int,
        cellPx: Int,
        widthMultipliers: List<Float>,
        availableWidthPx: Int,
        spacingPx: Int = 0
    ): Int = bottomRowIndexes(count, cellPx, widthMultipliers, availableWidthPx, spacingPx).size

    /**
     * Tinggi papan yang benar-benar muat di layar.
     *
     * [fixedPx] adalah bagian yang tidak bisa mengecil (padding bingkai, bilah judul,
     * dan baris tombol bawah). Kalau [availablePx] belum diketahui (0), tinggi yang
     * diminta dipakai apa adanya.
     */
    fun fitBoardHeightPx(requestedPx: Int, availablePx: Int, fixedPx: Int, minPx: Int = 1): Int {
        if (availablePx <= 0) return requestedPx
        val room = availablePx - fixedPx
        if (room >= requestedPx) return requestedPx
        return room.coerceAtLeast(minOf(minPx, requestedPx))
    }

    /**
     * Posisi papan (mode melayang) yang selalu terlihat penuh: bila digeser ke luar
     * layar, posisinya ditarik kembali ke tepi. [offsetPx] diukur dari pojok kiri-atas
     * area keyboard, [limitPx] adalah ukuran area yang tersedia.
     */
    fun clampOffset(offsetPx: Int, panelSizePx: Int, limitPx: Int): Int {
        val max = (limitPx - panelSizePx).coerceAtLeast(0)
        return offsetPx.coerceIn(0, max)
    }

    /** Ubah jarak geser (px) menjadi bagian layar (0..1) yang aman disimpan. */
    fun toFraction(offsetPx: Int, limitPx: Int): Float =
        if (limitPx <= 0) 0f else (offsetPx.toFloat() / limitPx).coerceIn(0f, 1f)

    /** Ubah bagian layar (0..1) kembali menjadi jarak geser (px). */
    fun fromFraction(fraction: Float, limitPx: Int): Int =
        (fraction.coerceIn(0f, 1f) * limitPx).toInt()
}
