package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Daftar karakter keyboard konvensional untuk tombol Karakter dan papan karakter. */
class KeyboardCharsTest {

    @Test
    fun `kelompok memuat angka, huruf, dan tanda baca tanpa kembar`() {
        val titles = KeyboardChars.groups.map { it.title }
        assertEquals(listOf("Angka", "Huruf kecil", "Huruf besar", "Tanda baca", "Simbol"), titles)
        val all = KeyboardChars.groups.flatMap { it.chars.toList() }
        assertEquals(all.size, all.toSet().size)
        assertTrue(all.containsAll("0123456789".toList()))
        assertTrue(all.containsAll("abcdefghijklmnopqrstuvwxyz".toList()))
        assertTrue(all.containsAll("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toList()))
        assertTrue(all.containsAll(".,!?;:'\"-()@#%".toList()))
        assertTrue(KeyboardChars.groups.all { group -> group.chars.isNotEmpty() && group.short.isNotEmpty() })
    }

    @Test
    fun `teks tombol dipotong tanpa membelah emoji`() {
        assertEquals("", KeyboardChars.clamp(""))
        assertEquals("abc", KeyboardChars.clamp("abc"))
        assertEquals(KeyboardChars.MAX_LENGTH, KeyboardChars.clamp("a".repeat(200)).length)
        val emoji = "\uD83D\uDE00".repeat(80)
        val clamped = KeyboardChars.clamp(emoji)
        assertTrue(clamped.length <= KeyboardChars.MAX_LENGTH * 2)
        assertTrue(clamped.codePointCount(0, clamped.length) <= KeyboardChars.MAX_LENGTH)
        assertFalseSurrogateTail(clamped)
    }

    @Test
    fun `muka tombol menampilkan karakter, label menang bila diisi`() {
        assertNull(KeyboardChars.faceOf("", ""))
        assertEquals("@", KeyboardChars.faceOf("", "@"))
        assertEquals("email", KeyboardChars.faceOf("email", "@gmail.com"))
        assertEquals("\u2423\u2423", KeyboardChars.faceOf("", "  "))
        assertEquals("\u21B5", KeyboardChars.faceOf("", "\n"))
        assertTrue(KeyboardChars.faceTextPx(100, "@") > KeyboardChars.faceTextPx(100, "halo"))
    }

    private fun assertFalseSurrogateTail(text: String) {
        if (text.isEmpty()) return
        assertTrue(!text.last().isHighSurrogate())
    }
}
