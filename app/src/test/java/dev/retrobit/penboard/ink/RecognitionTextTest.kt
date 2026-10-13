package dev.retrobit.penboard.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionTextTest {

    @Test
    fun `satu tulisan menjadi satu kata dengan spasi otomatis`() {
        assertEquals("halo ", RecognitionText.prepare("halo", autoSpace = true))
    }

    @Test
    fun `spasi otomatis bisa dimatikan`() {
        assertEquals("halo", RecognitionText.prepare("halo", autoSpace = false))
    }

    @Test
    fun `teks berlebih dirapikan`() {
        assertEquals("kata dua ", RecognitionText.prepare("  kata   dua \n", autoSpace = true))
    }

    @Test
    fun `hasil kosong tidak menghasilkan spasi`() {
        assertEquals("", RecognitionText.prepare(null, autoSpace = true))
        assertEquals("", RecognitionText.prepare("   ", autoSpace = true))
        assertEquals("", RecognitionText.prepare("\n\n", autoSpace = true))
    }

    @Test
    fun `teks tanpa huruf atau angka dianggap tidak layak`() {
        assertFalse(RecognitionText.isMeaningful(null))
        assertFalse(RecognitionText.isMeaningful("   "))
        assertFalse(RecognitionText.isMeaningful("-- --"))
        assertFalse(RecognitionText.isMeaningful("."))
    }

    @Test
    fun `teks berisi huruf atau angka layak diinput`() {
        assertTrue(RecognitionText.isMeaningful("a"))
        assertTrue(RecognitionText.isMeaningful(" 7 "))
        assertTrue(RecognitionText.isMeaningful("kata"))
        assertTrue(RecognitionText.isMeaningful("日本語"))
    }

    @Test
    fun `bahasa bawaan adalah Indonesia`() {
        assertEquals("id-ID", RecognitionLanguages.DEFAULT)
        assertTrue(RecognitionLanguages.isSupported("id-ID"))
        assertTrue(RecognitionLanguages.all.any { it.first == RecognitionLanguages.DEFAULT })
    }
}
