package dev.retrobit.penboard.util

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayMathTest {

    @Test
    fun `drag ke atas menaikkan papan`() {
        // Gravitasi bawah: rawY mengecil (jari naik) => y membesar (papan naik).
        assertEquals(1100, OverlayMath.dragY(1000, touchStartY = 500f, touchY = 400f))
    }

    @Test
    fun `drag ke bawah menurunkan papan`() {
        assertEquals(600, OverlayMath.dragY(1000, touchStartY = 500f, touchY = 900f))
    }

    @Test
    fun `tidak bergerak bila jari tidak berpindah`() {
        assertEquals(1000, OverlayMath.dragY(1000, touchStartY = 500f, touchY = 500f))
    }

    @Test
    fun `pergeseran tetap sama walau dilakukan bertahap`() {
        // Turun 120 px, lalu naik 200 px: hasilnya sama dengan satu kali gerakan.
        val step1 = OverlayMath.dragY(1000, 500f, 620f)
        val step2 = OverlayMath.dragY(step1, 620f, 420f)
        assertEquals(OverlayMath.dragY(1000, 500f, 420f), step2)
    }

    @Test
    fun `papan tidak bisa keluar dari layar`() {
        // Layar 2400 px, papan 400 px: y aman 0..2000.
        assertEquals(0, OverlayMath.clampY(-300, 2400, 400))
        assertEquals(0, OverlayMath.clampY(0, 2400, 400))
        assertEquals(2000, OverlayMath.clampY(2000, 2400, 400))
        assertEquals(2000, OverlayMath.clampY(5000, 2400, 400))
    }

    @Test
    fun `ruang aman atas dan bawah ikut dihitung`() {
        // Bilah status 90 px + bilah navigasi 60 px.
        assertEquals(1850, OverlayMath.clampY(9999, 2400, 400, topInsetPx = 90, bottomInsetPx = 60))
        // Bila papannya lebih tinggi dari layar, batasnya 0 (tidak negatif).
        assertEquals(0, OverlayMath.clampY(500, 1000, 1500))
    }

    @Test
    fun `posisi bawaan juga dibatasi`() {
        assertEquals(2000, OverlayMath.defaultY(boardHeightPx = 400, screenHeightPx = 2400, preferredY = 9000))
        assertEquals(0, OverlayMath.defaultY(boardHeightPx = 400, screenHeightPx = 2400, preferredY = -50))
        assertEquals(600, OverlayMath.defaultY(boardHeightPx = 400, screenHeightPx = 2400, preferredY = 600))
    }
}
