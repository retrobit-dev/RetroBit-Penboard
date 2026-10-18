package dev.retrobit.penboard.util

import dev.retrobit.penboard.data.KeyAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ikon pixel art (`util/PixelArt.kt`): kanvas 12 x 12, dipakai bilah bawah & tombol minimize. */
class PixelArtTest {

    private fun pixels(pattern: PixelArt.Pattern): Int =
        pattern.rows.sumOf { row -> row.count { it == PixelArt.Pattern.ON } }

    @Test
    fun `semua aksi punya ikon pixel art`() {
        val missing = KeyAction.entries.filter { !PixelArt.hasPattern(it) }
        assertTrue("aksi tanpa ikon: $missing", missing.isEmpty())
        assertEquals(KeyAction.entries.size, PixelArt.supported.size)
    }

    @Test
    fun `setiap pola berukuran 12 kali 12 dan tidak kosong`() {
        val all = KeyAction.entries.map { it to PixelArt.pattern(it) } + (null to PixelArt.MINIMIZE)
        all.forEach { (action, pattern) ->
            val name = action?.name ?: "MINIMIZE"
            assertEquals("$name: jumlah baris", PixelArt.GRID, pattern.rows.size)
            pattern.rows.forEachIndexed { y, row ->
                assertEquals("$name baris $y: lebar", PixelArt.GRID, row.length)
                assertTrue(
                    "$name baris $y: karakter tak dikenal",
                    row.all { it == PixelArt.Pattern.ON || it == '.' }
                )
            }
            val on = pixels(pattern)
            assertTrue("$name terlalu kosong ($on piksel)", on >= 14)
            assertTrue("$name terlalu penuh ($on piksel)", on <= PixelArt.GRID * PixelArt.GRID - 14)
        }
    }

    @Test
    fun `tidak ada dua aksi dengan ikon yang sama`() {
        val seen = HashMap<String, String>()
        (KeyAction.entries.map { it.name to PixelArt.pattern(it) } + ("MINIMIZE" to PixelArt.MINIMIZE))
            .forEach { (name, pattern) ->
                val twin = seen.put(pattern.rows.joinToString("/"), name)
                assertTrue("$name kembar dengan $twin", twin == null)
            }
    }

    @Test
    fun `ikon minimize berupa garis mendatar di tengah`() {
        val pattern = PixelArt.MINIMIZE
        val filled = (0 until PixelArt.GRID).filter { y ->
            (0 until PixelArt.GRID).any { x -> pattern.isOn(x, y) }
        }
        assertEquals(listOf(5, 6), filled)
        assertTrue(pattern.isOn(2, 5) && pattern.isOn(9, 6))
        assertFalse(pattern.isOn(1, 5))
        assertFalse(pattern.isOn(2, 4))
    }

    @Test
    fun `pola cadangan tetap kotak kecil yang sah`() {
        assertEquals(PixelArt.GRID, PixelArt.FALLBACK.rows.size)
        assertTrue(PixelArt.FALLBACK.rows.all { it.length == PixelArt.GRID })
        assertTrue(pixels(PixelArt.FALLBACK) >= 14)
    }

    @Test
    fun `piksel di luar kanvas dianggap mati`() {
        val pattern = PixelArt.pattern(KeyAction.TEXT)
        assertFalse(pattern.isOn(-1, 0))
        assertFalse(pattern.isOn(0, -1))
        assertFalse(pattern.isOn(PixelArt.GRID, 0))
        assertFalse(pattern.isOn(0, PixelArt.GRID))
    }
}
