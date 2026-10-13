package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ukuran tombol grid 3x3: papan tulis harus tetap lebar di kiri. */
class GridMathTest {

    @Test
    fun `grid mengikuti tinggi papan bila lebar layar cukup`() {
        assertEquals(200, GridMath.cellSizePx(600, 2400, 0))
    }

    @Test
    fun `jarak antar tombol diperhitungkan`() {
        assertEquals((600 - 20) / 3, GridMath.cellSizePx(600, 2400, 10))
    }

    @Test
    fun `pada layar sempit grid dibatasi agar papan tetap lebar`() {
        val screen = 1080
        val budget = (screen * GridMath.MAX_WIDTH_FRACTION).toInt()
        val cell = GridMath.cellSizePx(900, screen, 0)
        assertTrue("grid maksimal 34% lebar layar", cell * GridMath.COLUMNS <= budget)
    }

    @Test
    fun `ukuran minimum dipakai selama masih muat di tinggi papan`() {
        val cell = GridMath.cellSizePx(boardHeightPx = 600, availableWidthPx = 400, spacingPx = 0, minCellPx = 140)
        assertEquals(140, cell)
        assertTrue(cell * GridMath.ROWS <= 600)
    }

    @Test
    fun `papan pendek lebih menentukan daripada ukuran minimum`() {
        val cell = GridMath.cellSizePx(boardHeightPx = 300, availableWidthPx = 200, spacingPx = 8, minCellPx = 100)
        assertEquals((300 - 16) / 3, cell)
        assertTrue(cell * 3 + 8 * 2 <= 300)
    }

    @Test
    fun `tinggi papan nol atau negatif tidak membuat gagal`() {
        assertEquals(1, GridMath.cellSizePx(0, 1000))
        assertTrue(GridMath.cellSizePx(-50, 1000) >= 1)
    }

    @Test
    fun `jumlah baris grid mengikuti jumlah tombol`() {
        assertEquals(1, GridMath.rowsFor(0))
        assertEquals(1, GridMath.rowsFor(1))
        assertEquals(1, GridMath.rowsFor(3))
        assertEquals(2, GridMath.rowsFor(4))
        assertEquals(2, GridMath.rowsFor(6))
        assertEquals(3, GridMath.rowsFor(7))
        assertEquals(3, GridMath.rowsFor(9))
        assertEquals(3, GridMath.rowsFor(99))
    }

    @Test
    fun `grid selalu muat di dalam tinggi papan`() {
        val boardHeight = 420
        val cell = GridMath.cellSizePx(420, 1600, 6)
        assertTrue(cell * 3 + 6 * 2 <= boardHeight)
    }
}
