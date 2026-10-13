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
    fun `versi percobaan kedua dipaskan ke kotak tetap`() {
        // Tulisan setinggi 40 px diperbesar sampai tinggi kotak tetap.
        val fitted = InkNormalizer.normalizeFitted(listOf(stroke(0f, 0f, 100f, 40f)), 40f)
        assertEquals(1, fitted.size)
        val points = fitted.first().points
        val height = points.maxOf { it.y } - points.minOf { it.y }
        assertEquals(InkNormalizer.FIT_LINE_HEIGHT, height, 0.01f)
        // Selalu dimulai dari margin, bukan dari nol.
        assertTrue(points.minOf { it.x } >= InkNormalizer.FIT_MARGIN - 0.01f)
    }

    @Test
    fun `versi percobaan kedua bisa diminta lebih besar`() {
        val ink = listOf(stroke(0f, 0f, 100f, 40f))
        val large = InkNormalizer.normalizeFitted(ink, 40f, InkNormalizer.FIT_LARGE_HEIGHT)
        val height = large.first().points.maxOf { it.y } - large.first().points.minOf { it.y }
        assertEquals(InkNormalizer.FIT_LARGE_HEIGHT, height, 0.01f)
    }

    @Test
    fun `pembesaran dibatasi agar tulisan mini tidak meledak`() {
        // Tulisan 10x5 px hanya diperbesar sampai batas FIT_MAX_SCALE.
        val tiny = InkNormalizer.normalizeFitted(listOf(stroke(0f, 0f, 10f, 5f)), 40f)
        val height = tiny.first().points.maxOf { it.y } - tiny.first().points.minOf { it.y }
        assertEquals(5f * InkNormalizer.FIT_MAX_SCALE, height, 0.01f)
    }

    @Test
    fun `tinta yang sama disiapkan dalam beberapa versi`() {
        val strokes = listOf(stroke(0f, 0f, 120f, 30f), stroke(10f, 5f, 80f, 20f))
        val variants = InkNormalizer.variants(strokes, 40f)
        assertTrue("harus ada beberapa versi", variants.size >= 2)
        // Versi pertama adalah normalisasi biasa (tidak dimulai dari margin).
        assertEquals(0f, variants.first().first().points.minOf { it.x }, 0.01f)
        // Versi berikutnya dipaskan ke kotak tetap, jadi tidak ada yang sama persis.
        for (index in 1 until variants.size) {
            val a = variants[index - 1]
            val b = variants[index]
            val same = a.size == b.size && a.indices.all { s ->
                val pa = a[s].points
                val pb = b[s].points
                pa.size == pb.size && pa.indices.all { i ->
                    kotlin.math.abs(pa[i].x - pb[i].x) < 0.5f && kotlin.math.abs(pa[i].y - pb[i].y) < 0.5f
                }
            }
            assertTrue("versi $index seharusnya berbeda dari versi sebelumnya", !same)
        }
    }

    @Test
    fun `tidak ada versi untuk tulisan yang terlalu pendek`() {
        assertTrue(InkNormalizer.variants(listOf(stroke(5f, 5f)), 40f).isEmpty())
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
