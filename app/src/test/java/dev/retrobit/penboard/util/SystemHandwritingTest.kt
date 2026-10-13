package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pemilihan keyboard tulisan tangan bawaan (jembatan ke mesin Huawei/Celia). */
class SystemHandwritingTest {

    private fun candidate(pkg: String, label: String) =
        SystemHandwriting.Candidate(id = "$pkg/.Ime", packageName = pkg, label = label)

    @Test
    fun `keyboard huawei dan celia dikenali`() {
        assertTrue(SystemHandwriting.isCandidate("com.huawei.android.inputmethod", "Huawei Keyboard"))
        assertTrue(SystemHandwriting.isCandidate("com.huawei.celia", "Celia Keyboard"))
        assertTrue(SystemHandwriting.isCandidate("com.example.ime", "Tulisan Tangan"))
        assertTrue(SystemHandwriting.isCandidate("com.example.stylus", "Handwriting"))
        assertTrue(SystemHandwriting.isCandidate("jp.example", "手写入力"))
    }

    @Test
    fun `keyboard biasa bukan kandidat`() {
        assertFalse(SystemHandwriting.isCandidate("com.normal.keyboard", "Normal Keyboard"))
        assertFalse(SystemHandwriting.isCandidate("dev.retrobit.penboard", "Penboard"))
    }

    @Test
    fun `huawei dipilih lebih dulu daripada kandidat lain`() {
        val candidates = listOf(
            candidate("com.other.handwriting", "Handwriting Board"),
            candidate("com.huawei.android.inputmethod", "Huawei Keyboard"),
            candidate("com.honor.ime", "Honor Keyboard")
        )
        val best = SystemHandwriting.pickBest(candidates, ownPackage = "dev.retrobit.penboard")
        assertEquals("com.huawei.android.inputmethod", best?.packageName)
    }

    @Test
    fun `honor dipakai bila huawei tidak ada`() {
        val candidates = listOf(
            candidate("com.other.handwriting", "Handwriting"),
            candidate("com.honor.ime", "Honor Keyboard")
        )
        assertEquals("com.honor.ime", SystemHandwriting.pickBest(candidates, "dev.retrobit.penboard")?.packageName)
    }

    @Test
    fun `keyboard penboard sendiri tidak pernah dipilih`() {
        val candidates = listOf(
            candidate("dev.retrobit.penboard", "Penboard")
        )
        assertNull(SystemHandwriting.pickBest(candidates, "dev.retrobit.penboard"))
    }

    @Test
    fun `daftar kosong menghasilkan null`() {
        assertNull(SystemHandwriting.pickBest(emptyList(), "dev.retrobit.penboard"))
    }

    @Test
    fun `kandidat tulisan tangan umum tetap dipakai sebagai pilihan terakhir`() {
        val candidates = listOf(candidate("com.example.stylus", "Stylus Handwriting"))
        assertEquals("com.example.stylus", SystemHandwriting.pickBest(candidates, "own")?.packageName)
    }
}
