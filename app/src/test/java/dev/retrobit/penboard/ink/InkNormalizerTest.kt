package dev.retrobit.penboard.ink

import dev.retrobit.penboard.board.InkPoint
import dev.retrobit.penboard.board.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkNormalizerTest {

    private fun stroke(vararg coords: Float): Stroke {
        val points = mutableListOf<InkPoint>()
        var index = 0
        while (index + 1 < coords.size) {
            points.add(InkPoint(coords[index], coords[index + 1]))
            index += 2
        }
        return Stroke(points, color = 0xFF000000.toInt(), widthPx = 4f)
    }

    @Test
    fun `goresan pendek dibuang`() {
        val result = InkNormalizer.normalize(listOf(stroke(10f, 10f)), 40f)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `goresan dinormalkan ke tinggi baris tetap`() {
        val result = InkNormalizer.normalize(listOf(stroke(0f, 0f, 100f, 10f)), 40f)
        assertEquals(1, result.size)
        val points = result.first().points
        assertEquals(2, points.size)
        // Skala = 64 / 40 = 1,6 sehingga x 100 menjadi 160 dan y 10 menjadi 16.
        assertEquals(160f, points[1].x, 0.01f)
        assertEquals(16f, points[1].y, 0.01f)
        assertEquals(0, result.first().line)
    }

    @Test
    fun `baris kosong di antara tulisan dipadatkan`() {
        val first = stroke(0f, 5f, 20f, 5f)
        val third = stroke(0f, 85f, 20f, 85f) // baris 2 (0, 1, 2)
        val result = InkNormalizer.normalize(listOf(first, third), 40f)
        assertEquals(2, result.size)
        assertEquals(0, result[0].line)
        assertEquals(1, result[1].line)
        // Baris kedua digeser ke LINE_STEP, bukan dua kali LINE_STEP.
        assertTrue(result[1].points.first().y >= InkNormalizer.LINE_STEP - 1f)
    }

    @Test
    fun `lebar tulisan dibatasi lebar maksimal`() {
        val wide = stroke(0f, 0f, 100000f, 0f)
        val result = InkNormalizer.normalize(listOf(wide), 40f)
        val maxX = result.first().points.maxOf { it.x }
        assertTrue(maxX <= InkNormalizer.MAX_WIDTH + 0.01f)
    }

    @Test
    fun `jarak baris nol tidak membuat pembagian gagal`() {
        val result = InkNormalizer.normalize(listOf(stroke(0f, 0f, 10f, 10f)), 0f)
        assertEquals(1, result.size)
        assertTrue(result.first().points.all { it.x.isFinite() && it.y.isFinite() })
    }

    @Test
    fun `jumlah goresan dibatasi`() {
        val many = (0 until InkNormalizer.MAX_STROKES + 50).map { index ->
            stroke(0f, (index % 3) * 40f + 5f, 10f, (index % 3) * 40f + 15f)
        }
        val result = InkNormalizer.normalize(many, 40f)
        assertTrue(result.size <= InkNormalizer.MAX_STROKES)
    }
}
