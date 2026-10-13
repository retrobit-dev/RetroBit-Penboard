package dev.retrobit.penboard.ink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Aturan "satu tulisan = satu kata": hasil pengenalan selalu diakhiri satu spasi. */
class RecognitionTextTest {

    @Test
    fun `kata diberi spasi di belakang agar kata berikutnya terpisah`() {
        assertEquals("halo ", RecognitionText.prepare("halo", autoSpace = true))
        assertEquals("selamat pagi ", RecognitionText.prepare("selamat pagi", autoSpace = true))
    }

    @Test
    fun `spasi/baris di ujung dibersihkan lalu hanya satu spasi ditambahkan`() {
        assertEquals("halo ", RecognitionText.prepare("halo ", autoSpace = true))
        assertEquals("halo ", RecognitionText.prepare("halo\n", autoSpace = true))
        assertEquals("halo ", RecognitionText.prepare("  halo  ", autoSpace = true))
    }

    @Test
    fun `tanpa spasi otomatis teks dikirim tanpa tambahan`() {
        assertEquals("halo", RecognitionText.prepare("halo", autoSpace = false))
        assertEquals("halo", RecognitionText.prepare(" halo ", autoSpace = false))
    }

    @Test
    fun `teks kosong tidak menghasilkan spasi`() {
        assertEquals("", RecognitionText.prepare(null, autoSpace = true))
        assertEquals("", RecognitionText.prepare("", autoSpace = true))
        assertEquals("", RecognitionText.prepare("   ", autoSpace = true))
    }

    @Test
    fun `tanda baca tetap dianggap kata`() {
        assertEquals(". ", RecognitionText.prepare(".", autoSpace = true))
    }

    @Test
    fun `hanya hasil dengan huruf atau angka yang layak dikirim`() {
        assertTrue(RecognitionText.isMeaningful("halo"))
        assertTrue(RecognitionText.isMeaningful(" 12 "))
        assertTrue(RecognitionText.isMeaningful("3.14"))
        assertFalse(RecognitionText.isMeaningful("."))
        assertFalse(RecognitionText.isMeaningful("  "))
        assertFalse(RecognitionText.isMeaningful(null))
    }
}
