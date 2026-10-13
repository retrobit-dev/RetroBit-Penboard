package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Test

class BoardMathTest {

    @Test
    fun `jumlah baris mengikuti tinggi papan dan jarak baris`() {
        assertEquals(7, BoardMath.visibleLines(700f, 100f))
        assertEquals(3, BoardMath.visibleLines(80f, 100f))
        assertEquals(BoardMath.MIN_VISIBLE_LINES, BoardMath.visibleLines(0f, 44f))
    }

    @Test
    fun `jarak baris membagi tinggi papan dengan pas`() {
        val gap = BoardMath.gapPx(700f, 7)
        assertEquals(100f, gap, 0.001f)
        assertEquals(0f, BoardMath.gapPx(700f, 0), 0.001f)
    }

    @Test
    fun `indeks baris dihitung dari koordinat konten`() {
        assertEquals(0, BoardMath.lineIndexOf(0f, 50f))
        assertEquals(1, BoardMath.lineIndexOf(50f, 50f))
        assertEquals(3, BoardMath.lineIndexOf(175f, 50f))
        assertEquals(0, BoardMath.lineIndexOf(-20f, 50f))
    }

    @Test
    fun `perubahan koordinat konten dan layar saling membatalkan`() {
        val shift = 132f
        val offset = 40f
        val inset = 6f
        val contentY = 275f
        val viewY = BoardMath.toViewY(contentY, shift, offset, inset)
        assertEquals(contentY, BoardMath.toContentY(viewY, shift, offset, inset), 0.001f)
    }

    @Test
    fun `geser tampilan dibatasi antara baris terbaru dan awal konten`() {
        assertEquals(0f, BoardMath.clampViewOffset(-50f, 400f), 0.001f)
        assertEquals(250f, BoardMath.clampViewOffset(250f, 400f), 0.001f)
        assertEquals(400f, BoardMath.clampViewOffset(900f, 400f), 0.001f)
        assertEquals(0f, BoardMath.clampViewOffset(100f, 0f), 0.001f)
    }

    @Test
    fun `geser otomatis menjaga satu baris kosong di bawah`() {
        // 7 baris terlihat (0..6): baris 0..5 masih menyisakan tempat, baris terakhir (6) memicu geser
        assertEquals(0, BoardMath.autoShiftLines(0, 7))
        assertEquals(0, BoardMath.autoShiftLines(5, 7))
        assertEquals(1, BoardMath.autoShiftLines(6, 7))
        assertEquals(2, BoardMath.autoShiftLines(7, 7))
        // nilai di luar area tetap aman
        assertEquals(0, BoardMath.autoShiftLines(-3, 7))
        // goresan yang jauh di bawah digeser seluruhnya sampai masuk ke baris bebas terakhir
        assertEquals(94, BoardMath.autoShiftLines(99, 7))
        // papan dua baris: menulis di baris terakhir menggeser dua baris sekaligus
        assertEquals(2, BoardMath.autoShiftLines(2, 2))
        assertEquals(0, BoardMath.autoShiftLines(2, 0))
    }
}
