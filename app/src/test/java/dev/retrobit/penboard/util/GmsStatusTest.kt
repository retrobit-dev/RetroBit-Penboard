package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GmsStatusTest {

    @Test
    fun `kode Google Play services dipetakan dengan benar`() {
        assertEquals(GmsStatus.READY, GmsStatus.from(0))
        assertEquals(GmsStatus.MISSING, GmsStatus.from(1))
        assertEquals(GmsStatus.UPDATE_REQUIRED, GmsStatus.from(2))
        assertEquals(GmsStatus.DISABLED, GmsStatus.from(3))
        assertEquals(GmsStatus.INVALID, GmsStatus.from(9))
        assertEquals(GmsStatus.UPDATING, GmsStatus.from(18))
    }

    @Test
    fun `kode yang tidak dikenal dianggap tidak diketahui`() {
        assertEquals(GmsStatus.UNKNOWN, GmsStatus.from(99))
        assertEquals(GmsStatus.UNKNOWN, GmsStatus.from(-5))
    }

    @Test
    fun `versi lama dan sedang diperbarui tetap dicoba`() {
        // Perbedaan penting: GMS versi lama sering dijumpai di perangkat Huawei,
        // dan mesin pengenalan masih bisa berjalan.
        assertTrue(GmsStatus.UPDATE_REQUIRED.canAttempt)
        assertTrue(GmsStatus.UPDATING.canAttempt)
        assertTrue(GmsStatus.READY.canAttempt)
        assertTrue(GmsStatus.UNKNOWN.canAttempt)
    }

    @Test
    fun `tanpa Google Play services tidak dicoba`() {
        assertFalse(GmsStatus.MISSING.canAttempt)
        assertFalse(GmsStatus.DISABLED.canAttempt)
        assertFalse(GmsStatus.INVALID.canAttempt)
    }

    @Test
    fun `setiap keadaan punya pesan dan saran`() {
        GmsStatus.entries.forEach { status ->
            assertTrue("pesan kosong: $status", status.message.isNotBlank())
            assertTrue("saran kosong: $status", status.advice.isNotBlank())
        }
        // Yang tidak bisa dicoba harus menyebutkan jalan keluarnya.
        assertTrue(GmsStatus.MISSING.advice.contains("tulisan tangan perangkat"))
        assertTrue(GmsStatus.INVALID.advice.contains("tulisan tangan perangkat"))
    }

    @Test
    fun `ringkasan menggabungkan pesan dan saran`() {
        assertEquals(GmsStatus.MISSING.message + " " + GmsStatus.MISSING.advice, GmsStatus.MISSING.summary)
    }
}
