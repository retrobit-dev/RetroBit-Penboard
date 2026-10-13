package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextInsertTest {

    @Test
    fun `teks disisipkan pada posisi kursor`() {
        val result = TextInsert.into("halo dunia", 5, 5, " baru")
        assertEquals("halo baru dunia", result.text)
        assertEquals(10, result.caret)
    }

    @Test
    fun `teks menggantikan bagian yang dipilih`() {
        val result = TextInsert.into("halo dunia", 5, 10, "kawan")
        assertEquals("halo kawan", result.text)
        assertEquals(10, result.caret)
    }

    @Test
    fun `kolom kosong diisi apa adanya`() {
        val result = TextInsert.into("", -1, -1, "kata ")
        assertEquals("kata ", result.text)
        assertEquals(5, result.caret)
    }

    @Test
    fun `posisi kursor yang aneh tetap aman`() {
        // Melebihi panjang teks: dianggap di akhir teks.
        assertEquals(TextInsert.Result("halo", 4), TextInsert.into("halo", 99, 99, ""))
        // -1 (tidak diketahui) diperlakukan sebagai akhir teks.
        assertEquals("halo!", TextInsert.into("halo", 4, -1, "!").text)
        assertEquals("halo!", TextInsert.into("halo", -1, -1, "!").text)
        assertEquals("halox", TextInsert.into("halo", -5, -5, "x").text)
    }

    @Test
    fun `hapus satu huruf sebelum kursor`() {
        assertEquals("halo", TextInsert.deleteBackward("halo!", 5, 5).text)
        assertEquals(4, TextInsert.deleteBackward("halo!", 5, 5).caret)
        // Di awal teks tidak ada yang dihapus.
        assertEquals("halo", TextInsert.deleteBackward("halo", 0, 0).text)
    }

    @Test
    fun `hapus satu huruf sesudah kursor`() {
        assertEquals("halo", TextInsert.deleteForward("hxalo", 1, 1).text)
        assertEquals(1, TextInsert.deleteForward("hxalo", 1, 1).caret)
        assertEquals("halo", TextInsert.deleteForward("halo", 4, 4).text)
    }

    @Test
    fun `hapus satu kata`() {
        assertEquals("halo ", TextInsert.deleteWord("halo dunia", 10, 10).text)
        assertEquals(5, TextInsert.deleteWord("halo dunia", 10, 10).caret)
        // Spasi tepat di kiri kursor ikut dibuang, jadi hanya sisa kata sebelumnya.
        assertEquals("halo ", TextInsert.deleteWord("halo dunia ", 11, 11).text)
        assertEquals("", TextInsert.deleteWord("halo ", 5, 5).text)
    }

    @Test
    fun `awal kata ditemukan`() {
        assertEquals(5, TextInsert.wordStart("halo dunia", 10))
        assertEquals(5, TextInsert.wordStart("halo dunia", 6))
        assertEquals(0, TextInsert.wordStart("dunia", 5))
        assertEquals(0, TextInsert.wordStart("", 0))
    }

    @Test
    fun `kursor digeser di dalam batas teks`() {
        assertEquals(3, TextInsert.moveCaret("halo", 2, 1))
        assertEquals(0, TextInsert.moveCaret("halo", 0, -1))
        assertEquals(4, TextInsert.moveCaret("halo", 4, 5))
        // Kursor tak diketahui dianggap di awal saat menggeser.
        assertEquals(1, TextInsert.moveCaret("halo", -1, 1))
    }
}
