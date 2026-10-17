package dev.retrobit.penboard.util

import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.PenboardConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridMathTest {

    @Test
    fun `tinggi papan menentukan ukuran tombol`() {
        // Tinggi: (600 - 12) / 3 = 196. Lebar: ((1200 * 0,34) - 12) / 3 = 132.
        // Lebar layar yang lebih ketat, jadi 132 yang dipakai.
        val cell = GridMath.cellSizePx(600, 1200, 6, 0)
        assertEquals(132, cell)
    }

    @Test
    fun `tanpa jarak tombol tetap dibatasi lebar`() {
        assertEquals(136, GridMath.cellSizePx(600, 1200, 0, 0))
    }

    @Test
    fun `ukuran minimum menjaga tombol tetap bisa ditekan`() {
        // Tanpa batas minimum tombolnya hanya 34 px; dengan minimum 120 px dipakai
        // ukuran sebesar mungkin yang masih muat di tinggi papan.
        assertEquals(34, GridMath.cellSizePx(300, 300, 0, 0))
        assertEquals(100, GridMath.cellSizePx(300, 300, 0, 120))
    }

    @Test
    fun `ukuran tombol tidak pernah nol atau negatif`() {
        assertTrue(GridMath.cellSizePx(0, 0, 0, 0) >= 1)
        assertTrue(GridMath.cellSizePx(1, 1, 0, 0) >= 1)
        assertTrue(GridMath.cellSizePx(40, 900, 30, 0) >= 1)
    }

    @Test
    fun `jumlah baris mengikuti tinggi papan dibagi jumlah baris tombol`() {
        // Layar lebar (2000 px): tinggi papan yang menentukan, bukan lebar layar.
        assertEquals(200, GridMath.cellSizePx(600, 2000, 0, 0, rows = 3))
        assertEquals(150, GridMath.cellSizePx(600, 2000, 0, 0, rows = 4))
        assertEquals(100, GridMath.cellSizePx(600, 2000, 0, 0, rows = 6))
        // Satu baris: tombolnya boleh tinggi, tetapi lebar layar tetap membatasi.
        assertEquals(226, GridMath.cellSizePx(600, 2000, 0, 0, rows = 1))
    }

    @Test
    fun `jumlah baris di luar rentang tetap aman`() {
        assertEquals(100, GridMath.cellSizePx(600, 2000, 0, 0, rows = 9))
        assertEquals(226, GridMath.cellSizePx(600, 2000, 0, 0, rows = 0))
    }

    @Test
    fun `ukuran tombol bisa dipilih pengguna dan tetap muat`() {
        // Dipilih 50 dp pada layar xxhdpi (density 2,75) = 137 px, dan masih muat.
        assertEquals(137, GridMath.cellSizePx(900, 2000, 0, 0, rows = 3, cellDp = 50, density = 2.75f))
        // Ukuran yang terlalu besar dipangkas supaya masih muat di tinggi papan.
        assertEquals(300, GridMath.cellSizePx(900, 2000, 0, 0, rows = 3, cellDp = 400, density = 2.75f, minBoardWidthPx = 600))
        // Panel sempit: ukuran besar tetap dipangkas oleh lebar yang tersedia.
        assertEquals(133, GridMath.cellSizePx(900, 700, 0, 0, rows = 3, columns = 3, cellDp = 200, density = 1f, minBoardWidthPx = 300))
    }

    @Test
    fun `lebar papan membatasi ukuran tombol`() {
        // Ruang untuk tombol = 1080 - 300 (area tulis) - 8 = 772; 3 kolom -> (772 - 16) / 3 = 252 px.
        assertEquals(252, GridMath.cellSizePx(900, 1080, 8, 0, rows = 3, columns = 3, minBoardWidthPx = 300))
        // Enam kolom membuat tombolnya lebih kecil, tetapi tidak kurang dari 1 px.
        assertTrue(GridMath.cellSizePx(300, 600, 8, 0, rows = 6, columns = 6, minBoardWidthPx = 400) >= 1)
    }

    @Test
    fun `area tulis selalu setinggi tombol kali jumlah baris`() {
        // Persegi: tinggi papan = baris x sisi tombol + jarak antar baris.
        assertEquals(618, GridMath.boardHeightPx(3, 200, 9))
        assertEquals(400, GridMath.boardHeightPx(1, 400, 6))
        assertEquals(3, GridMath.boardHeightPx(2, 0, 3))
    }

    @Test
    fun `kolom tombol dihitung dari ukuran tombol`() {
        assertEquals(554, GridMath.gridWidthPx(3, 180, 7))
        assertEquals(180, GridMath.gridWidthPx(1, 180, 7))
        // Lebar area tulis = sisa panel setelah kolom tombol.
        assertEquals(500, GridMath.boardWidthPx(1080, 9, 554, 8))
        assertEquals(1, GridMath.boardWidthPx(20, 9, 554, 8))
    }

    @Test
    fun `tombol bawah mengalir sesuai lebar papan tulis`() {
        // Papan 600 px, tombol 100 px, jarak 10 px: (600 + 10) / (100 + 10) = 5 tombol.
        assertEquals(5, GridMath.columnsFor(600, 100, 10))
        // Papan sempit: tetap satu kolom supaya tombol tidak hilang.
        assertEquals(1, GridMath.columnsFor(40, 100, 10))
        assertEquals(1, GridMath.columnsFor(600, 0, 10))
    }

    @Test
    fun `baris tombol bawah dihitung dari jumlah tombol`() {
        assertEquals(0, GridMath.wrapRows(0, 3))
        assertEquals(1, GridMath.wrapRows(3, 3))
        assertEquals(2, GridMath.wrapRows(4, 3))
        assertEquals(3, GridMath.wrapRows(8, 3))
        assertEquals(2, GridMath.wrapRows(8, 5))
        assertEquals(8, GridMath.wrapRows(8, 0))
    }

    @Test
    fun `baris area tulis menentukan tinggi papan`() {
        // Satu baris = satu jarak baris. Bawaan 3 baris, 48 px per baris = 144 px.
        assertEquals(3, GridMath.DEFAULT_LINES)
        assertEquals(144, GridMath.boardHeightForLines(3, 48))
        assertEquals(48, GridMath.boardHeightForLines(1, 48))
        assertEquals(288, GridMath.boardHeightForLines(6, 48))
        // Nilai liar tetap aman.
        assertEquals(48, GridMath.boardHeightForLines(0, 48))
        assertEquals(288, GridMath.boardHeightForLines(99, 48))
        assertEquals(3, GridMath.boardHeightForLines(3, 0))
    }

    @Test
    fun `jarak baris dipangkas bila ruang keyboard tidak cukup`() {
        // Ruang 600 px, fixed 150 px, 3 baris -> sisa 150 px per baris; diminta 80 px.
        assertEquals(80, GridMath.fitLineGapPx(80, 600, 3, 150))
        // Ruang sempit: 300 px, fixed 150 px, 3 baris -> 50 px per baris.
        assertEquals(50, GridMath.fitLineGapPx(80, 300, 3, 150))
        // Tidak pernah nol atau negatif.
        assertTrue(GridMath.fitLineGapPx(80, 100, 3, 150) >= 1)
        assertTrue(GridMath.fitLineGapPx(80, 0, 3, 150) >= 1)
        // Tinggi belum diketahui: jarak yang diminta dipakai apa adanya.
        assertEquals(80, GridMath.fitLineGapPx(80, 0, 3, 0))
    }

    @Test
    fun `tinggi tombol bawah punya pengali sendiri`() {
        // Tinggi dasar = sisi tombol samping (80 px).
        assertEquals(80, GridMath.buttonHeightPx(80, 1f))
        assertEquals(120, GridMath.buttonHeightPx(80, 1.5f))
        assertEquals(48, GridMath.buttonHeightPx(80, 0.6f))
        // Di luar rentang ditarik ke 0,6x-2x.
        assertEquals(48, GridMath.buttonHeightPx(80, 0.1f))
        assertEquals(160, GridMath.buttonHeightPx(80, 9f))
        assertTrue(GridMath.buttonHeightPx(1, 0.6f) >= 1)
    }

    @Test
    fun `baris tombol bawah setinggi tombol tertinggi di barisnya`() {
        val rows = listOf(listOf(0, 1), listOf(2))
        // Tombol ke-1 dua kali lebih tinggi: baris pertama ikut setinggi itu.
        assertEquals(listOf(160, 80), GridMath.bottomRowHeights(rows, 80, listOf(1f, 2f, 1f)))
        // Tanpa tombol: tidak ada baris dan tidak ada tinggi.
        assertEquals(emptyList<Int>(), GridMath.bottomRowHeights(emptyList(), 80, emptyList()))
    }

    @Test
    fun `tinggi bilah tombol bawah menjumlahkan barisnya`() {
        assertEquals(0, GridMath.bottomBarHeightPx(emptyList(), 6))
        assertEquals(80, GridMath.bottomBarHeightPx(listOf(80), 6))
        // Dua baris: 80 + 6 (jarak) + 120.
        assertEquals(206, GridMath.bottomBarHeightPx(listOf(80, 120), 6))
    }

    @Test
    fun `lebar tombol bawah mengikuti pengali`() {
        // Tinggi tombol samping 80 px; lebar = tinggi x pengali.
        assertEquals(80, GridMath.buttonWidthPx(80, 1f))
        assertEquals(160, GridMath.buttonWidthPx(80, 2f))
        assertEquals(200, GridMath.buttonWidthPx(80, 2.5f))
        // Di luar rentang ditarik ke 1x-4x supaya tombol tidak pernah hilang.
        assertEquals(80, GridMath.buttonWidthPx(80, 0.2f))
        assertEquals(320, GridMath.buttonWidthPx(80, 9f))
        assertTrue(GridMath.buttonWidthPx(1, 1f) >= 1)
    }

    @Test
    fun `tombol bawah dibungkus sesuai lebar masing-masing`() {
        // Lima tombol persegi 100 px + jarak 10 px: muat satu baris di papan 600 px.
        assertEquals(
            listOf(listOf(0, 1, 2, 3, 4)),
            GridMath.bottomRowIndexes(5, 100, List(5) { 1f }, 600, 10)
        )
        // Papan sempit 250 px: hanya dua tombol per baris.
        assertEquals(
            listOf(listOf(0, 1), listOf(2, 3), listOf(4)),
            GridMath.bottomRowIndexes(5, 100, List(5) { 1f }, 250, 10)
        )
        // Tombol berlebar 2x tidak lagi muat di baris pertama, jadi pindah baris.
        assertEquals(
            listOf(listOf(0, 1), listOf(2)),
            GridMath.bottomRowIndexes(3, 100, listOf(1f, 1f, 2f), 250, 10)
        )
        // Tanpa tombol: tidak ada baris sama sekali.
        assertEquals(0, GridMath.bottomRows(0, 100, emptyList(), 600, 10))
        assertEquals(2, GridMath.bottomRows(4, 100, List(4) { 1f }, 250, 10))
    }

    @Test
    fun `kolom tombol tidak menghabiskan lebar area tulis`() {
        // Layar 1080 px, padding tepi 3 px, area tulis minimum 198 px (72 dp).
        val usable = 1080 - 2 * 3
        val minBoard = 198
        val cell = GridMath.cellSizePx(
            600, usable, 8, 60, rows = 3, columns = 6, minBoardWidthPx = minBoard
        )
        val grid = GridMath.gridWidthPx(6, cell, 8)
        // Enam kolom pas: sisa lebar panel tepat sama dengan area tulis minimum.
        assertEquals(minBoard, GridMath.boardWidthPx(1080, 3, grid, 8))
        assertTrue(grid <= usable - minBoard - 8)
        // Ukuran tombol pilihan pengguna juga tetap dipangkas oleh lebar area tulis.
        val explicit = GridMath.cellSizePx(
            900, usable, 8, 0, rows = 6, columns = 6, cellDp = 160, density = 1f, minBoardWidthPx = minBoard
        )
        assertTrue(explicit < 160)
        val explicitGrid = GridMath.gridWidthPx(6, explicit, 8)
        assertTrue(GridMath.boardWidthPx(1080, 3, explicitGrid, 8) >= minBoard)
    }

    @Test
    fun `tinggi papan dipangkas supaya seluruh panel muat`() {
        assertEquals(400, GridMath.fitBoardHeightPx(400, 900, 200, 120))
        assertEquals(250, GridMath.fitBoardHeightPx(400, 600, 350, 120))
        assertEquals(120, GridMath.fitBoardHeightPx(400, 300, 350, 120))
        assertEquals(80, GridMath.fitBoardHeightPx(80, 300, 350, 120))
        assertEquals(400, GridMath.fitBoardHeightPx(400, 0, 350, 120))
    }

    @Test
    fun `posisi papan selalu di dalam layar`() {
        // Papan 400 px di layar 1000 px: boleh 0..600.
        assertEquals(0, GridMath.clampOffset(-50, 400, 1000))
        assertEquals(600, GridMath.clampOffset(900, 400, 1000))
        assertEquals(250, GridMath.clampOffset(250, 400, 1000))
        // Papan lebih besar dari layar: tetap di pojok, tidak pernah negatif.
        assertEquals(0, GridMath.clampOffset(120, 1200, 1000))
    }

    @Test
    fun `posisi papan disimpan sebagai bagian layar`() {
        assertEquals(0.25f, GridMath.toFraction(250, 1000), 0.0001f)
        assertEquals(250, GridMath.fromFraction(0.25f, 1000))
        assertEquals(0f, GridMath.toFraction(250, 0), 0.0001f)
        // Nilai liar ditarik ke rentang 0..1.
        assertEquals(0f, GridMath.toFraction(-90, 1000), 0.0001f)
        assertEquals(1f, GridMath.toFraction(9000, 1000), 0.0001f)
        assertEquals(1000, GridMath.fromFraction(9f, 1000))
    }

    @Test
    fun `kolom dan baris bisa diatur satu sampai enam`() {
        assertEquals(1, GridMath.columnsOr(0))
        assertEquals(6, GridMath.columnsOr(99))
        assertEquals(4, GridMath.columnsOr(4))
        assertEquals(1, GridMath.rowsOr(-3))
        assertEquals(6, GridMath.rowsOr(6))
        assertEquals(36, GridMath.slots(6, 6))
        assertEquals(3, GridMath.slots(3, 1))
    }

    @Test
    fun `baris grid mengikuti jumlah tombol`() {
        assertEquals(1, GridMath.rowsFor(0))
        assertEquals(1, GridMath.rowsFor(3))
        assertEquals(2, GridMath.rowsFor(4))
        assertEquals(3, GridMath.rowsFor(9))
        // Empat kolom: 9 tombol butuh 3 baris.
        assertEquals(3, GridMath.rowsFor(9, columns = 4))
        assertEquals(1, GridMath.rowsFor(4, columns = 6))
    }

    @Test
    fun `lebar area tulis bisa diatur dan tidak pernah menekan tombol`() {
        // 1080 px isi panel, 3 kolom minimum 102 px + jarak: sisanya untuk area tulis.
        val usable = 1080
        val spacing = 9
        val gridMin = GridMath.gridWidthPx(3, 102, spacing)

        // Permintaan 60% dipakai apa adanya.
        assertEquals(648, GridMath.boardWidthFor(usable, 0.6f, 216, gridMin, spacing))
        // Permintaan 80% dipangkas supaya tombol tetap selebar minimum.
        assertEquals(747, GridMath.boardWidthFor(usable, 0.8f, 216, gridMin, spacing))
        // Nilai di luar 40-80% ditarik dulu ke rentang itu.
        assertEquals(432, GridMath.boardWidthFor(usable, 0.1f, 216, gridMin, spacing))
        // Ruang sempit: area tulis boleh mengecil sampai batas yang masih muat.
        assertEquals(27, GridMath.boardWidthFor(360, 0.6f, 216, gridMin, spacing))

        assertEquals(0.4f, GridMath.boardFractionOr(0f), 0.0001f)
        assertEquals(0.8f, GridMath.boardFractionOr(9f), 0.0001f)
        assertEquals(GridMath.DEFAULT_BOARD_WIDTH_FRACTION, GridMath.boardFractionOr(Float.NaN), 0.0001f)

        // Ukuran tombol maksimum yang menyisakan lebar area tulis.
        assertEquals(135, GridMath.maxCellByWidthPx(usable, 648, 3, spacing))
        assertEquals(63, GridMath.maxCellByWidthPx(usable, 648, 6, spacing))
        assertEquals(1, GridMath.maxCellByWidthPx(100, 200, 3, spacing))
    }

    @Test
    fun `tombol atas maksimal lima dan rata kiri`() {
        // Batas jumlah tombol atas: 0-5.
        assertEquals(5, PenboardConfig.MAX_BOTTOM_KEYS)
        assertEquals(5, PenboardConfig.BOTTOM_SLOTS)
        assertEquals(5, PenboardConfig.defaultBottomKeys().size)
        // Semua tombol bawaan benar-benar bekerja (bukan TEXT kosong).
        PenboardConfig.defaultBottomKeys().forEach { key ->
            assertTrue(!(key.action == KeyAction.TEXT && key.text.isBlank()))
        }
    }

    @Test
    fun `tiga kolom selalu muat di samping area tulis`() {
        val usable = 1080
        val spacing = 9
        val gridMin = GridMath.gridWidthPx(3, 102, spacing)
        for (fraction in listOf(0.4f, 0.6f, 0.8f)) {
            val board = GridMath.boardWidthFor(usable, fraction, 216, gridMin, spacing)
            val cell = GridMath.maxCellByWidthPx(usable, board, 3, spacing)
            val grid = GridMath.gridWidthPx(3, cell, spacing)
            // Area tulis + jarak + seluruh kolom tombol = tepat selebar isi panel.
            assertTrue(board + spacing + grid <= usable)
            assertTrue(cell >= 1)
        }
    }

    @Test
    fun `tiga kolom bawaan dan baris 1 sampai 6`() {
        assertEquals(3, GridMath.DEFAULT_COLUMNS)
        assertEquals(1, GridMath.MIN_ROWS)
        assertEquals(6, GridMath.MAX_ROWS)
        assertEquals(3, GridMath.ROWS)
        assertTrue(GridMath.MAX_WIDTH_FRACTION in 0.2f..0.5f)
        assertTrue(GridMath.MIN_CELL_DP < GridMath.MAX_CELL_DP)
    }
}
