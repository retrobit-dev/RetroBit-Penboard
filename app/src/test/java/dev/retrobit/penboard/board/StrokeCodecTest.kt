package dev.retrobit.penboard.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeCodecTest {

    private fun sample(): List<Stroke> {
        val first = Stroke(0xFFF8FAFC.toInt(), 3.5f)
        first.add(10.4f, 20.6f)
        first.add(30.2f, 44.9f)
        val second = Stroke(0xFFFF6B6B.toInt(), 6f)
        second.add(100f, 300f)
        return listOf(first, second)
    }

    @Test
    fun `goresan bertahan setelah disimpan dan dimuat`() {
        val original = sample()
        val encoded = StrokeCodec.encode(original, 132.5f)
        val snapshot = StrokeCodec.decode(encoded)

        assertEquals(132.5f, snapshot.shiftPx, 0.05f)
        assertEquals(2, snapshot.strokes.size)

        val first = snapshot.strokes[0]
        assertEquals(original[0].color, first.color)
        assertEquals(3.5f, first.widthPx, 0.05f)
        assertEquals(2, first.size)
        assertEquals(10f, first.xAt(0), 1.01f)
        assertEquals(21f, first.yAt(0), 1.01f)
        assertEquals(30f, first.xAt(1), 1.01f)
        assertEquals(45f, first.yAt(1), 1.01f)

        assertEquals(0xFFFF6B6B.toInt(), snapshot.strokes[1].color)
        assertEquals(6f, snapshot.strokes[1].widthPx, 0.05f)
    }

    @Test
    fun `papan kosong menghasilkan snapshot kosong`() {
        val snapshot = StrokeCodec.decode(StrokeCodec.encode(emptyList(), 0f))
        assertTrue(snapshot.strokes.isEmpty())
        assertEquals(0f, snapshot.shiftPx, 0.001f)
    }

    @Test
    fun `teks kosong atau null aman`() {
        assertTrue(StrokeCodec.decode(null).strokes.isEmpty())
        assertTrue(StrokeCodec.decode("").strokes.isEmpty())
        assertTrue(StrokeCodec.decode("   \n  ").strokes.isEmpty())
    }

    @Test
    fun `baris rusak diabaikan tanpa menggagalkan pemuatan`() {
        val text = buildString {
            append("PENBOARD-BOARD 1\n")
            append("shift=99\n")
            append("S bukan-angka 3.0 10,10\n")
            append("S -1 0 10,10\n")
            append("S -1 3.0 10,abc 12,\n")
            append("baris aneh\n")
            append("S -2298474 4.0 5,5 6,7\n")
        }
        val snapshot = StrokeCodec.decode(text)
        assertEquals(99f, snapshot.shiftPx, 0.001f)
        assertEquals(1, snapshot.strokes.size)
        assertEquals(-2298474, snapshot.strokes[0].color)
        assertEquals(2, snapshot.strokes[0].size)
    }

    @Test
    fun `geser negatif dianggap nol`() {
        val snapshot = StrokeCodec.decode("PENBOARD-BOARD 1\nshift=-50\n")
        assertEquals(0f, snapshot.shiftPx, 0.001f)
    }

    @Test
    fun `banyak goresan tetap ringkas saat disimpan`() {
        val strokes = (0 until 50).map { index ->
            Stroke(0xFF00FF00.toInt(), 3f).also { stroke ->
                repeat(80) { point -> stroke.add(point.toFloat(), index.toFloat()) }
            }
        }
        val encoded = StrokeCodec.encode(strokes, 0f)
        val snapshot = StrokeCodec.decode(encoded)
        assertEquals(50, snapshot.strokes.size)
        assertEquals(80, snapshot.strokes.last().size)
        // format teks: setiap titik hanya beberapa byte
        assertTrue(encoded.length < 50 * 80 * 12)
    }
}
