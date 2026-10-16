package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `baris tulisan menentukan berapa enter yang dipakai`() {
        // Baris pertama: teks masuk di posisi kursor (tanpa enter).
        assertEquals(0, BoardMath.newlineOffset(0))
        // Baris kedua: satu enter (satu baris di bawah kursor).
        assertEquals(1, BoardMath.newlineOffset(1))
        // Baris ketiga: dua enter.
        assertEquals(2, BoardMath.newlineOffset(2))
        // Nilai liar tetap aman.
        assertEquals(0, BoardMath.newlineOffset(-5))
        assertEquals(BoardMath.MAX_LINE_OFFSET, BoardMath.newlineOffset(99))
        assertEquals(0, BoardMath.newlineOffset(9, maxOffset = 0))
    }

    @Test
    fun `baris tulisan dibaca dari tengah goresan`() {
        // Tulisan yang duduk di garis panduan pertama (jarak 80 px) memakai y sekitar 0,4-1,1.
        val firstLine = floatArrayOf(38f, 52f, 61f, 70f, 80f, 88f)
        assertEquals(0, BoardMath.newlineOffset(BoardMath.lineIndexOf(BoardMath.medianY(firstLine), 80f, 4)))

        // Baris kedua: tulisan digeser 80 px.
        val secondLine = FloatArray(firstLine.size) { index -> firstLine[index] + 80f }
        assertEquals(1, BoardMath.newlineOffset(BoardMath.lineIndexOf(BoardMath.medianY(secondLine), 80f, 4)))

        // Baris ketiga: digeser 160 px.
        val thirdLine = FloatArray(firstLine.size) { index -> firstLine[index] + 160f }
        assertEquals(2, BoardMath.newlineOffset(BoardMath.lineIndexOf(BoardMath.medianY(thirdLine), 80f, 4)))

        // Papan pendek (hanya dua baris): baris paling bawah tetap dibatasi.
        assertEquals(1, BoardMath.newlineOffset(BoardMath.lineIndexOf(230f, 80f, 2)))
    }

    @Test
    fun `nilai tengah goresan tahan terhadap ekor panjang`() {
        assertEquals(0f, BoardMath.medianY(floatArrayOf()), 0.001f)
        assertEquals(5f, BoardMath.medianY(floatArrayOf(5f)), 0.001f)
        assertEquals(2f, BoardMath.medianY(floatArrayOf(9f, 2f, 1f)), 0.001f)
        assertEquals(2.5f, BoardMath.medianY(floatArrayOf(9f, 2f, 1f, 3f)), 0.001f)
    }

    @Test
    fun `tekanan pena mengubah ketebalan dalam batas aman`() {
        assertEquals(0.75f, BoardMath.pressureScale(0f), 0.001f)
        assertEquals(1.0f, BoardMath.pressureScale(0.5f), 0.001f)
        assertEquals(1.25f, BoardMath.pressureScale(1f), 0.001f)
    }

    @Test
    fun `tekanan di luar rentang tetap aman`() {
        assertEquals(0.75f, BoardMath.pressureScale(-3f), 0.001f)
        assertEquals(1.25f, BoardMath.pressureScale(9f), 0.001f)
        assertTrue(BoardMath.pressureScale(0.25f) in 0.75f..1.25f)
    }
}
