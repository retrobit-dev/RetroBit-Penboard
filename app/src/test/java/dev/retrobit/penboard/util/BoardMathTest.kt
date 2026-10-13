package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardMathTest {

    @Test
    fun `indeks baris mengikuti jarak garis panduan`() {
        assertEquals(0, BoardMath.lineIndexOf(10f, 80f, 4))
        assertEquals(1, BoardMath.lineIndexOf(85f, 80f, 4))
        assertEquals(2, BoardMath.lineIndexOf(160f, 80f, 4))
    }

    @Test
    fun `tanpa batas baris dihitung apa adanya`() {
        assertEquals(5, BoardMath.lineIndexOf(400f, 80f, 0))
    }

    @Test
    fun `tulisan di luar papan masuk ke baris terakhir`() {
        assertEquals(3, BoardMath.lineIndexOf(999f, 80f, 4))
    }

    @Test
    fun `jarak baris nol tidak membuat pembagian gagal`() {
        assertEquals(0, BoardMath.lineIndexOf(150f, 0f, 4))
    }

    @Test
    fun `jumlah baris selalu minimal satu`() {
        assertEquals(1, BoardMath.lineCount(0, 80f))
        assertEquals(1, BoardMath.lineCount(40, 80f))
        assertEquals(5, BoardMath.lineCount(400, 80f))
    }
}
