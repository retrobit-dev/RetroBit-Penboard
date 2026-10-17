package dev.retrobit.penboard.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityIdsTest {

    private val pkg = "dev.retrobit.penboard"
    private val className = "dev.retrobit.penboard.overlay.PenboardTypingService"

    @Test
    fun `id pendek dikenali`() {
        assertTrue(AccessibilityIds.matches("$pkg/.overlay.PenboardTypingService", pkg, className))
    }

    @Test
    fun `id panjang dikenali`() {
        assertTrue(AccessibilityIds.matches("$pkg/$className", pkg, className))
        assertTrue(AccessibilityIds.matches(className, pkg, className))
    }

    @Test
    fun `layanan lain tidak dikenali`() {
        assertFalse(AccessibilityIds.matches("$pkg/.overlay.LainService", pkg, className))
        assertFalse(AccessibilityIds.matches("com.lain.app/.OtherService", pkg, className))
        assertFalse(AccessibilityIds.matches(null, pkg, className))
        assertFalse(AccessibilityIds.matches("", pkg, className))
    }
}
