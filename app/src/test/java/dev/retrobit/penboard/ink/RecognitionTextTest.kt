package dev.retrobit.penboard.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionTextTest {

    @Test
    fun `satu tulisan menjadi satu kata dengan spasi di ujung`() {
        assertEquals("halo ", RecognitionText.prepare("halo", autoSpace = true))
        assertEquals("halo dunia ", RecognitionText.prepare("halo\n dunia ", autoSpace = true))
    }

    @Test
    fun `tanpa spasi otomatis teks dikirim tanpa tambahan`() {
        assertEquals("halo", RecognitionText.prepare(" halo ", autoSpace = false))
    }

    @Test
    fun `teks kosong tetap kosong`() {
        assertEquals("", RecognitionText.prepare(null, autoSpace = true))
        assertEquals("", RecognitionText.prepare("   ", autoSpace = true))
        assertEquals("", RecognitionText.prepare("\n", autoSpace = true))
    }

    @Test
    fun `hanya tulisan bermakna yang layak diinput`() {
        assertTrue(RecognitionText.isMeaningful("halo"))
        assertTrue(RecognitionText.isMeaningful(" 12 "))
        assertFalse(RecognitionText.isMeaningful("  ..  "))
        assertFalse(RecognitionText.isMeaningful(""))
        assertFalse(RecognitionText.isMeaningful(null))
    }
}
