package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTagsTest {

    @Test
    fun `tag lengkap dicoba dulu lalu bentuk pendeknya`() {
        // Model bahasa Indonesia di ML Kit bernama "id", bukan "id-ID".
        assertEquals(listOf("id-ID", "id"), LanguageTags.candidates("id-ID"))
        assertEquals(listOf("en-US", "en"), LanguageTags.candidates("en-US"))
        assertEquals(listOf("zh-Hans", "zh"), LanguageTags.candidates("zh-Hans"))
    }

    @Test
    fun `tag tanpa wilayah dipakai apa adanya`() {
        assertEquals(listOf("id"), LanguageTags.candidates("id"))
        assertEquals(listOf("ar"), LanguageTags.candidates("ar"))
        assertEquals("ja", LanguageTags.candidates("ja-JP").last())
        assertEquals(listOf("ja-JP", "ja"), LanguageTags.candidates("ja-JP"))
    }

    @Test
    fun `tag kosong memakai bahasa cadangan`() {
        assertEquals(listOf("en-US", "en"), LanguageTags.candidates(""))
        assertEquals(listOf("en-US", "en"), LanguageTags.candidates("   "))
    }

    @Test
    fun `bentuk pendek tetap benar untuk berbagai penulisan`() {
        assertEquals("id", LanguageTags.short("id-ID"))
        assertEquals("id", LanguageTags.short("id_ID"))
        assertEquals("zh", LanguageTags.short("zh-Hans"))
        assertEquals("en", LanguageTags.short("EN-GB"))
        assertEquals("jv", LanguageTags.short("jv-ID"))
    }

    @Test
    fun `wilayah dikenali`() {
        assertTrue(LanguageTags.hasRegion("id-ID"))
        assertTrue(LanguageTags.hasRegion("zh-Hans"))
        assertFalse(LanguageTags.hasRegion("id"))
        assertFalse(LanguageTags.hasRegion("ar"))
    }
}
