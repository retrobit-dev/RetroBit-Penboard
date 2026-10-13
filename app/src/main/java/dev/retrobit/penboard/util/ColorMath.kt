package dev.retrobit.penboard.util

/**
 * Hitungan warna sederhana (tanpa Android) supaya bisa diuji unit.
 *
 * Dipakai untuk memilih warna teks di atas lencana/kilasan papan: tema retro
 * cyberpunk grayscale ePaper memakai aksen **terang** (#C8C8C8), jadi teks putih
 * akan tenggelam. [textOn] memilih gelap atau putih sesuai kecerahan latarnya.
 */
object ColorMath {

    /** Teks gelap ala tinta ePaper. */
    const val DARK_TEXT = 0xFF0B0B0B.toInt()

    /** Teks terang. */
    const val LIGHT_TEXT = 0xFFFFFFFF.toInt()

    /** Ambang kecerahan: di atas ini latar dianggap terang. */
    const val LIGHT_THRESHOLD = 150

    fun red(color: Int): Int = (color shr 16) and 0xFF

    fun green(color: Int): Int = (color shr 8) and 0xFF

    fun blue(color: Int): Int = color and 0xFF

    /** Kecerahan 0-255 menurut persepsi mata (hijau paling terang, biru paling gelap). */
    fun luminance(color: Int): Int =
        (0.299 * red(color) + 0.587 * green(color) + 0.114 * blue(color)).toInt().coerceIn(0, 255)

    /** Apakah [color] termasuk terang (sehingga perlu teks gelap). */
    fun isLight(color: Int): Boolean = luminance(color) >= LIGHT_THRESHOLD

    /** Warna teks yang paling terbaca di atas [background]. */
    fun textOn(background: Int, dark: Int = DARK_TEXT, light: Int = LIGHT_TEXT): Int =
        if (isLight(background)) dark else light
}
