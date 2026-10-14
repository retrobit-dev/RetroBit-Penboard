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
    fun `jumlah baris mengikuti tinggi papan dibagi jumlah baris tombol`() {
        // Layar lebar (2000 px): tinggi papan yang menentukan, bukan lebar layar.
        assertEquals(200, GridMath.cellSizePx(600, 2000, 0, 0, rows = 3))
        assertEquals(150, GridMath.cellSizePx(600, 2000, 0, 0, rows = 4))
        assertEquals(120, GridMath.cellSizePx(600, 2000, 0, 0, rows = 5))
        assertEquals(100, GridMath.cellSizePx(600, 2000, 0, 0, rows = 6))
        // Satu baris: tombolnya boleh tinggi, tetapi lebar layar tetap membatasi.
        assertEquals(226, GridMath.cellSizePx(600, 2000, 0, 0, rows = 1))
    }

    @Test
    fun `jumlah baris di luar rentang tetap aman`() {
        // Lebih dari 6 baris dipangkas ke 6; kurang dari 1 baris dianggap 1.
        assertEquals(100, GridMath.cellSizePx(600, 2000, 0, 0, rows = 9))
        assertEquals(226, GridMath.cellSizePx(600, 2000, 0, 0, rows = 0))
    }

    @Test
    fun `baris grid mengikuti jumlah tombol`() {
        assertEquals(1, GridMath.rowsFor(0))
        assertEquals(1, GridMath.rowsFor(1))
        assertEquals(1, GridMath.rowsFor(3))
        assertEquals(2, GridMath.rowsFor(4))
        assertEquals(3, GridMath.rowsFor(9))
        assertEquals(4, GridMath.rowsFor(10))
        assertEquals(4, GridMath.rowsFor(12))
        assertEquals(5, GridMath.rowsFor(13))
        assertEquals(6, GridMath.rowsFor(18))
        assertEquals(6, GridMath.rowsFor(20))
    }

    @Test
    fun `tombol bawah mengalir sesuai lebar papan tulis`() {
        // Papan 600 px, tombol 100 px, jarak 10 px: (600 + 10) / (100 + 10) = 5,5 -> 5 tombol.
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
    fun `tiga kolom tetap dan baris 1 sampai 6`() {
        assertEquals(3, GridMath.COLUMNS)
        assertEquals(1, GridMath.MIN_ROWS)
        assertEquals(6, GridMath.MAX_ROWS)
        assertEquals(3, GridMath.ROWS)
        assertTrue(GridMath.MAX_WIDTH_FRACTION in 0.2f..0.5f)
    }
}
