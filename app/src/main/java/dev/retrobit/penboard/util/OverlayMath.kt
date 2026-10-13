package dev.retrobit.penboard.util

/**
 * Hitungan posisi papan melayang (murni Kotlin, mudah diuji).
 *
 * Papan melayang memakai `Gravity.BOTTOM`, sehingga **`y` diukur dari tepi bawah
 * layar**: `y = 0` berarti menempel di bawah dan **makin besar `y` makin ke atas**.
 * Kesalahan tanda di sini membuat papan bergerak ke arah yang berlawanan dengan
 * jari pengguna (drag ke atas justru turun), jadi hitungannya dipisahkan ke sini
 * supaya bisa diuji.
 */
object OverlayMath {

    /**
     * Posisi `y` baru saat papan digeser: **jari naik → papan naik**.
     *
     * @param windowStartY posisi `y` saat jari mulai menyentuh pegangan
     * @param touchStartY  posisi jari (rawY) saat mulai menyentuh
     * @param touchY       posisi jari (rawY) sekarang
     */
    fun dragY(windowStartY: Int, touchStartY: Float, touchY: Float): Int =
        windowStartY + (touchStartY - touchY).toInt()

    /**
     * Batasi `y` supaya papan tetap terlihat penuh di layar.
     *
     * @param y              posisi yang diinginkan
     * @param screenHeightPx tinggi layar
     * @param boardHeightPx  tinggi papan (termasuk pegangan)
     * @param topInsetPx     ruang aman di atas (bilah status)
     * @param bottomInsetPx  ruang aman di bawah (bilah navigasi)
     */
    fun clampY(
        y: Int,
        screenHeightPx: Int,
        boardHeightPx: Int,
        topInsetPx: Int = 0,
        bottomInsetPx: Int = 0
    ): Int {
        val highest = (screenHeightPx - boardHeightPx - topInsetPx - bottomInsetPx).coerceAtLeast(0)
        return y.coerceIn(0, highest)
    }

    /** Posisi awal bawaan (sedikit di atas tepi bawah layar), juga dibatasi [clampY]. */
    fun defaultY(
        boardHeightPx: Int,
        screenHeightPx: Int,
        preferredY: Int,
        topInsetPx: Int = 0,
        bottomInsetPx: Int = 0
    ): Int = clampY(preferredY, screenHeightPx, boardHeightPx, topInsetPx, bottomInsetPx)
}
