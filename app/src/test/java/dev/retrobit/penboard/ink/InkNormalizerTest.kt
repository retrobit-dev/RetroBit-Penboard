package dev.retrobit.penboard.ink

import dev.retrobit.penboard.board.InkPoint
import dev.retrobit.penboard.board.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkNormalizerTest {

    private fun stroke(vararg points: Pair<Float, Float>): Stroke =
        Stroke(points.map { InkPoint(it.first, it.second) }.toMutableList(), color = 0, widthPx = 2f)

    @Test
    fun `tanpa goresan tidak ada keluaran`() {
        assertTrue(InkNormalizer.normalize(emptyList(), 80f).isEmpty())
    }

    @Test
    fun `goresan satu titik diabaikan`() {
        assertTrue(InkNormalizer.normalize(listOf(stroke(10f to 10f)), 80f).isEmpty())
    }

    @Test
    fun `baris kosong di antara tulisan dipadatkan`() {
        val strokes = listOf(
            stroke(0f to 10f, 20f to 10f),
            stroke(0f to 170f, 20f to 170f)
        )
        val result = InkNormalizer.normalize(strokes, 80f)
        assertEquals(2, result.size)
        assertEquals(0, result[0].line)
        assertEquals(1, result[1].line)
        val secondY = result[1].points.first().y
        assertTrue("baris kedua memakai satu langkah, bukan dua", secondY >= InkNormalizer.LINE_STEP)
        assertTrue(secondY < 2 * InkNormalizer.LINE_STEP)
    }

    @Test
    fun `tinggi satu baris dinormalkan`() {
        val result = InkNormalizer.normalize(listOf(stroke(0f to 40f, 100f to 40f)), 80f)
        val points = result.first().points
        assertEquals(0f, points.first().x, 0.01f)
        assertEquals(32f, points.first().y, 0.01f)
        assertEquals(80f, points.last().x, 0.01f)
    }

    @Test
    fun `lebar tulisan dibatasi`() {
        val result = InkNormalizer.normalize(listOf(stroke(0f to 10f, 5000f to 10f)), 80f)
        assertTrue(result.first().points.last().x <= InkNormalizer.MAX_WIDTH + 1f)
    }

    @Test
    fun `jumlah goresan dibatasi`() {
        val many = (0 until InkNormalizer.MAX_STROKES + 40).map { stroke(0f to 10f, 5f to 10f) }
        assertTrue(InkNormalizer.normalize(many, 80f).size <= InkNormalizer.MAX_STROKES)
    }

    @Test
    fun `jarak baris nol tetap aman`() {
        val result = InkNormalizer.normalize(listOf(stroke(0f to 5f, 10f to 5f)), 0f)
        assertEquals(1, result.size)
        assertEquals(0, result.first().line)
    }
}
