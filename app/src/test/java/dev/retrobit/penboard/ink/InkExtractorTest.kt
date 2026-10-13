package dev.retrobit.penboard.ink

import dev.retrobit.penboard.board.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InkExtractorTest {

    private val gap = 50f

    private fun stroke(line: Int, points: Int = 3, x: Float = 10f): Stroke {
        val stroke = Stroke(0xFF112233.toInt(), 3f)
        repeat(points) { index -> stroke.add(x + index * 4f, line * gap + 8f + index * 2f) }
        return stroke
    }

    @Test
    fun `papan kosong menghasilkan capture kosong`() {
        val capture = InkExtractor.capture(emptyList(), gap, 3, 800, 400)
        assertTrue(capture.isEmpty)
        assertEquals(0, capture.pointCount)
        assertEquals(800, capture.width)
    }

    @Test
    fun `hanya goresan dari baris terakhir yang diambil`() {
        val strokes = listOf(stroke(0), stroke(1), stroke(2), stroke(3))
        val capture = InkExtractor.capture(strokes, gap, 2, 800, 400)
        assertEquals(2, capture.lines.size)
        // goresan baris 2 & 3 diambil, dan yang paling awal (baris 2) jadi acuan koordinat
        assertEquals(0f, capture.lines.first().points.first().y, 0.01f)
        assertEquals((gap + 8f), capture.lines.last().points.first().y, 0.01f)
    }

    @Test
    fun `koordinat y dinormalkan ke baris pertama yang dikenali`() {
        val strokes = listOf(stroke(4), stroke(5))
        val capture = InkExtractor.capture(strokes, gap, 2, 800, 400)
        assertEquals(0f, capture.lines.first().points.first().y, 0.01f)
        assertEquals(gap, capture.lines.last().points.first().y, 0.01f)
    }

    @Test
    fun `goresan satu titik diabaikan`() {
        val strokes = listOf(stroke(0), stroke(0, points = 1), stroke(0, points = 2))
        val capture = InkExtractor.capture(strokes, gap, 3, 800, 400)
        assertEquals(2, capture.lines.size)
        assertTrue(capture.lines.all { it.points.size >= 2 })
    }

    @Test
    fun `jumlah goresan dan titik dibatasi`() {
        val many = (0 until 200).map { stroke(5) }
        val capture = InkExtractor.capture(many, gap, 3, 800, 400)
        assertTrue(capture.lines.size <= InkExtractor.MAX_STROKES)
        assertTrue(capture.pointCount <= InkExtractor.MAX_POINTS)
    }

    @Test
    fun `nilai jumlah baris di luar batas tetap aman`() {
        val strokes = listOf(stroke(0), stroke(9))
        assertEquals(2, InkExtractor.capture(strokes, gap, 0, 800, 400).lines.size)
        assertEquals(2, InkExtractor.capture(strokes, gap, 99, 800, 400).lines.size)
        assertFalse(InkExtractor.capture(strokes, 0f, 3, 800, 400).isEmpty)
    }

    @Test
    fun `urutan goresan dipertahankan sesuai urutan menulis`() {
        val strokes = listOf(stroke(1, x = 5f), stroke(1, x = 40f), stroke(1, x = 80f))
        val capture = InkExtractor.capture(strokes, gap, 1, 800, 400)
        assertEquals(3, capture.lines.size)
        assertEquals(5f, capture.lines[0].points.first().x, 0.01f)
        assertEquals(40f, capture.lines[1].points.first().x, 0.01f)
        assertEquals(80f, capture.lines[2].points.first().x, 0.01f)
    }
}
