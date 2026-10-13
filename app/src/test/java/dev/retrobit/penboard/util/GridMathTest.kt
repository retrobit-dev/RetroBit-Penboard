package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ukuran tombol grid 3x3: papan tulis harus tetap selebar mungkin di kiri. */
class GridMathTest {

    @Test
    fun `grid mengikuti tinggi papan bila lebar cukup`() {
        // papan 600px tinggi, layar lebar 2400px: 600/3 = 200px per tombol
        assertEquals(200, GridMath.cellSizePx(boardHeightPx = 600, availableWidthPx = 2400, spacingPx = 0))
    }

    @Test
    fun `jarak antar tombol diperhitungkan`() {
        val cell = GridMath.cellSizePx(boardHeightPx = 600, availableWidthPx = 2400, spacingPx = 10)
        assertEquals((600 - 20) / 3, cell)
    }

    @Test
    fun `pada layar sempit grid dibatasi agar papan tetap lebar`() {
        val screen = 1080
        val budget = (screen * GridMath.MAX_WIDTH_FRACTION).toInt()
        val cell = GridMath.cellSizePx(boardHeightPx = 900, availableWidthPx = screen, spacingPx = 0)
        assertTrue("grid tidak boleh lebih dari 34% lebar layar", cell * 3 <= budget)
    }

    @Test
    fun `ukuran minimum dipakai selama masih muat di tinggi papan`() {
        // lebar layar sempit: tanpa batas minimum, tombol hanya ~45px; minimum menaikkannya
        val withMinimum = GridMath.cellSizePx(
            boardHeightPx = 600,
            availableWidthPx = 400,
            spacingPx = 0,
            minCellPx = 140
        )
        assertEquals(140, withMinimum)
        assertTrue("tidak boleh melebihi tinggi papan/baris", withMinimum * 3 <= 600)
    }

    @Test
    fun `papan pendek selalu diprioritaskan agar grid tidak melewati tinggi papan`() {
        val cell = GridMath.cellSizePx(
            boardHeightPx = 300,
            availableWidthPx = 200,
            spacingPx = 8,
            minCellPx = 100
        )
        // ruang per baris = (300 - 2*8) / 3 = 94 -> tinggi papan yang menentukan
        assertEquals((300 - 16) / 3, cell)
        assertTrue(cell * 3 + 8 * 2 <= 300)
    }

    @Test
    fun `tinggi papan nol tidak membuat pembagian gagal`() {
        assertEquals(1, GridMath.cellSizePx(boardHeightPx = 0, availableWidthPx = 1000))
        assertTrue(GridMath.cellSizePx(boardHeightPx = -50, availableWidthPx = 1000) >= 1)
    }

    @Test
    fun `jumlah baris grid mengikuti jumlah tombol`() {
        assertEquals(1, GridMath.rowsFor(1))
        assertEquals(1, GridMath.rowsFor(3))
        assertEquals(2, GridMath.rowsFor(4))
        assertEquals(2, GridMath.rowsFor(6))
        assertEquals(3, GridMath.rowsFor(7))
        assertEquals(3, GridMath.rowsFor(9))
        // nilai di luar batas tetap aman
        assertEquals(1, GridMath.rowsFor(0))
        assertEquals(3, GridMath.rowsFor(99))
    }

    @Test
    fun `grid selalu muat di dalam tinggi papan`() {
        val boardHeight = 420
        val cell = GridMath.cellSizePx(boardHeightPx = boardHeight, availableWidthPx = 1600, spacingPx = 6)
        assertTrue(cell * 3 + 6 * 2 <= boardHeight)
    }
}
