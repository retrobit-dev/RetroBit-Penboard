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
    fun `baris tombol mengisi tepat setinggi papan`() {
        // 6 baris, jarak 10 px, papan 600 px: (600 - 50) / 6 = 91 sisa 4 -> empat baris 92.
        val heights = GridMath.rowHeightsPx(600, 10, 6)
        assertEquals(6, heights.size)
        assertEquals(92, heights.first())
        assertEquals(91, heights.last())
        assertEquals(600, heights.sum() + 10 * 5)

        // Tanpa sisa: semua baris sama tinggi.
        val even = GridMath.rowHeightsPx(600, 0, 3)
        assertEquals(listOf(200, 200, 200), even)
        assertEquals(600, even.sum())

        // Dua baris dengan jarak: tetap pas.
        val two = GridMath.rowHeightsPx(443, 3, 2)
        assertEquals(443, two.sum() + 3)
    }

    @Test
    fun `tinggi baris aman untuk angka ekstrem`() {
        assertEquals(1, GridMath.rowHeightPx(0, 0, 3))
        assertTrue(GridMath.rowHeightPx(40, 30, 6) >= 1)
        // Papan terlalu pendek: setiap baris tetap minimal 1 px, tidak pernah nol/negatif.
        val tiny = GridMath.rowHeightsPx(10, 30, 6)
        assertEquals(6, tiny.size)
        assertTrue(tiny.all { it >= 1 })
        // Baris di luar rentang dipangkas seperti cellSizePx.
        assertEquals(GridMath.MAX_ROWS, GridMath.rowHeightsPx(600, 0, 99).size)
        assertEquals(GridMath.MIN_ROWS, GridMath.rowHeightsPx(600, 0, 0).size)
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
    fun `tinggi papan dipangkas supaya seluruh panel muat`() {
        // Ruang cukup: tinggi yang diminta dipakai apa adanya.
        assertEquals(400, GridMath.fitBoardHeightPx(400, 900, 200, 120))
        // Ruang kurang: papan dipangkas sampai sisa ruang setelah bagian tetap.
        assertEquals(250, GridMath.fitBoardHeightPx(400, 600, 350, 120))
        // Perangkat sangat kecil: tetap ada lantai minimum, tapi tidak lebih dari yang diminta.
        assertEquals(120, GridMath.fitBoardHeightPx(400, 300, 350, 120))
        assertEquals(80, GridMath.fitBoardHeightPx(80, 300, 350, 120))
        // Tanpa informasi ruang: jangan diubah.
        assertEquals(400, GridMath.fitBoardHeightPx(400, 0, 350, 120))
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
