package dev.retrobit.penboard.util

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
    fun `tanpa jarak tombol tetap dibatasi lebar layar`() {
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
    fun `baris grid mengikuti jumlah tombol`() {
        assertEquals(1, GridMath.rowsFor(0))
        assertEquals(1, GridMath.rowsFor(1))
        assertEquals(1, GridMath.rowsFor(3))
        assertEquals(2, GridMath.rowsFor(4))
        assertEquals(3, GridMath.rowsFor(9))
        assertEquals(3, GridMath.rowsFor(12))
    }

    @Test
    fun `grid selalu tiga kolom tiga baris`() {
        assertEquals(3, GridMath.COLUMNS)
        assertEquals(3, GridMath.ROWS)
        assertTrue(GridMath.MAX_WIDTH_FRACTION in 0.2f..0.5f)
    }
}
