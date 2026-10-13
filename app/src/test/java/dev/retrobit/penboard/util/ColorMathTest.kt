package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    @Test
    fun `kecerahan mengikuti persepsi mata`() {
        assertEquals(0, ColorMath.luminance(0xFF000000.toInt()))
        assertEquals(255, ColorMath.luminance(0xFFFFFFFF.toInt()))
        // Hijau paling terang, biru paling gelap.
        assertTrue(ColorMath.luminance(0xFF00FF00.toInt()) > ColorMath.luminance(0xFFFF0000.toInt()))
        assertTrue(ColorMath.luminance(0xFFFF0000.toInt()) > ColorMath.luminance(0xFF0000FF.toInt()))
        // Abu-abu tetap abu-abu.
        assertEquals(128, ColorMath.luminance(0xFF808080.toInt()))
    }

    @Test
    fun `kanal warna dibaca benar`() {
        val color = 0xFF123456.toInt()
        assertEquals(0x12, ColorMath.red(color))
        assertEquals(0x34, ColorMath.green(color))
        assertEquals(0x56, ColorMath.blue(color))
    }

    @Test
    fun `teks di atas aksen terang memakai tinta gelap`() {
        // Aksen tema retro (#C8C8C8) terang -> teks harus gelap supaya terbaca.
        assertEquals(ColorMath.DARK_TEXT, ColorMath.textOn(0xFFC8C8C8.toInt()))
        assertEquals(ColorMath.DARK_TEXT, ColorMath.textOn(0xFFEDEDED.toInt()))
        assertEquals(ColorMath.DARK_TEXT, ColorMath.textOn(0xFF808080.toInt()))
    }

    @Test
    fun `teks di atas latar gelap memakai putih`() {
        assertEquals(ColorMath.LIGHT_TEXT, ColorMath.textOn(0xFF0B0B0B.toInt()))
        assertEquals(ColorMath.LIGHT_TEXT, ColorMath.textOn(0xFF161616.toInt()))
        assertEquals(ColorMath.LIGHT_TEXT, ColorMath.textOn(0xFF3F3F3F.toInt()))
    }

    @Test
    fun `ambang terang ada di angka 128`() {
        // Abu-abu 0x80 tepat di ambang, 0x7F tepat di bawahnya.
        assertEquals(ColorMath.LIGHT_THRESHOLD, ColorMath.luminance(0xFF808080.toInt()))
        assertTrue(ColorMath.isLight(0xFF808080.toInt()))
        assertEquals(ColorMath.LIGHT_THRESHOLD - 1, ColorMath.luminance(0xFF7F7F7F.toInt()))
        assertFalse(ColorMath.isLight(0xFF7F7F7F.toInt()))
    }

    @Test
    fun `warna teks bisa diganti pemanggil`() {
        val dark = 0xFF111111.toInt()
        val light = 0xFFEEEEEE.toInt()
        assertEquals(dark, ColorMath.textOn(0xFFFFFFFF.toInt(), dark, light))
        assertEquals(light, ColorMath.textOn(0xFF000000.toInt(), dark, light))
    }
}
