package dev.retrobit.penboard.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeTest {

    private fun stroke(vararg points: Pair<Float, Float>): Stroke {
        val stroke = Stroke(0xFF112233.toInt(), 4f)
        points.forEach { (x, y) -> stroke.add(x, y) }
        return stroke
    }

    @Test
    fun `goresan kosong tidak dianggap punya isi`() {
        val stroke = Stroke(0, 3f)
        assertTrue(stroke.isEmpty)
        assertEquals(0, stroke.size)
        assertFalse(stroke.intersectsCircle(0f, 0f, 100f))
    }

    @Test
    fun `titik tersimpan sesuai urutan`() {
        val stroke = stroke(10f to 20f, 30f to 40f, 55f to 66f)
        assertEquals(3, stroke.size)
        assertEquals(10f, stroke.xAt(0), 0.001f)
        assertEquals(20f, stroke.yAt(0), 0.001f)
        assertEquals(55f, stroke.xAt(2), 0.001f)
        assertEquals(66f, stroke.yAt(2), 0.001f)
    }

    @Test
    fun `batas goresan mengikuti titik terjauh`() {
        val stroke = stroke(10f to 5f, 40f to 90f, -5f to 30f)
        assertEquals(-5f, stroke.left(), 0.001f)
        assertEquals(5f, stroke.top(), 0.001f)
        assertEquals(40f, stroke.right(), 0.001f)
        assertEquals(90f, stroke.bottom(), 0.001f)
    }

    @Test
    fun `baris dihitung dari titik pertama`() {
        val stroke = stroke(10f to 130f, 20f to 10f)
        assertEquals(2, stroke.lineIndex(50f))
        assertEquals(0, stroke.lineIndex(0f))
    }

    @Test
    fun `penghapus mengenai garis dan melewatkan area kosong`() {
        val stroke = stroke(0f to 0f, 100f to 100f)
        assertTrue(stroke.intersectsCircle(50f, 52f, 6f))
        assertFalse(stroke.intersectsCircle(20f, 90f, 5f))
        // masih terdeteksi selama radius menyentuh ujung goresan
        assertTrue(stroke.intersectsCircle(104f, 104f, 8f))
    }

    @Test
    fun `penghapus bekerja pada goresan satu titik`() {
        val stroke = stroke(30f to 30f)
        assertTrue(stroke.intersectsCircle(32f, 31f, 4f))
        assertFalse(stroke.intersectsCircle(60f, 60f, 4f))
    }

    @Test
    fun `banyak titik tetap tersimpan dengan benar`() {
        val stroke = Stroke(1, 2f)
        repeat(1000) { index -> stroke.add(index.toFloat(), index * 2f) }
        assertEquals(1000, stroke.size)
        assertEquals(999f, stroke.xAt(999), 0.001f)
        assertEquals(1998f, stroke.yAt(999), 0.001f)
        assertEquals(0f, stroke.left(), 0.001f)
        assertEquals(1998f, stroke.bottom(), 0.001f)
    }
}
