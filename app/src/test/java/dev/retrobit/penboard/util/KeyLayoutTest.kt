package dev.retrobit.penboard.util

import dev.retrobit.penboard.data.KeyDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyLayoutTest {

    private fun key(width: Float = 1f, flex: Float = 0f) =
        KeyDef(text = "x", width = width, flex = flex)

    @Test
    fun `empty row produces no widths`() {
        assertEquals(0, KeyLayout.computeWidths(emptyList(), 1000).size)
    }

    @Test
    fun `equal keys share space equally`() {
        val widths = KeyLayout.computeWidths(List(3) { key() }, 900)
        assertEquals(3, widths.size)
        assertEquals(300, widths[0])
        assertEquals(300, widths[1])
        assertEquals(300, widths[2])
    }

    @Test
    fun `widths always fill the available space`() {
        val widths = KeyLayout.computeWidths(List(7) { key(width = 1.3f) }, 1013)
        assertEquals(1013, widths.sum())
    }

    @Test
    fun `flexible key takes its share of the row`() {
        val keys = listOf(key(), key(flex = 0.4f), key())
        val widths = KeyLayout.computeWidths(keys, 1000)
        assertEquals(1000, widths.sum())
        assertEquals(400, widths[1])
        assertEquals(300, widths[0])
        assertEquals(300, widths[2])
    }

    @Test
    fun `flexible keys split their share proportionally`() {
        val keys = listOf(key(flex = 0.2f), key(flex = 0.2f), key())
        val widths = KeyLayout.computeWidths(keys, 1000)
        assertEquals(1000, widths.sum())
        assertEquals(200, widths[0])
        assertEquals(200, widths[1])
        assertEquals(600, widths[2])
    }

    @Test
    fun `relative widths are respected`() {
        val keys = listOf(key(width = 2f), key(width = 1f))
        val widths = KeyLayout.computeWidths(keys, 900)
        assertEquals(600, widths[0])
        assertEquals(300, widths[1])
    }

    @Test
    fun `every key gets at least one pixel`() {
        val widths = KeyLayout.computeWidths(List(10) { key() }, 9)
        assertTrue(widths.all { it >= 1 })
    }
}
