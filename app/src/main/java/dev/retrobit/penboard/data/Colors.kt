package dev.retrobit.penboard.data

/**
 * Utilitas warna. Sengaja **tanpa ketergantungan Android** supaya bisa diuji lewat unit test
 * biasa (android.graphics.Color hanya berupa stub di unit test JVM).
 */
object Colors {

    fun alpha(color: Int): Int = (color ushr 24) and 0xFF

    fun red(color: Int): Int = (color ushr 16) and 0xFF

    fun green(color: Int): Int = (color ushr 8) and 0xFF

    fun blue(color: Int): Int = color and 0xFF

    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        ((alpha and 0xFF) shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)

    /** Gabungkan warna dasar dengan tingkat transparansi (0..100). */
    fun withOpacity(color: Int, percent: Int): Int {
        val a = percent.coerceIn(0, 100) * 255 / 100
        return argb(a, red(color), green(color), blue(color))
    }

    /** Campur dua warna (ratio 0 = a, 1 = b). */
    fun blend(a: Int, b: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        return argb(
            (alpha(a) * (1 - t) + alpha(b) * t).toInt(),
            (red(a) * (1 - t) + red(b) * t).toInt(),
            (green(a) * (1 - t) + green(b) * t).toInt(),
            (blue(a) * (1 - t) + blue(b) * t).toInt()
        )
    }

    /** Warna teks yang paling kontras untuk latar tertentu. */
    fun contrastFor(color: Int): Int {
        val luminance = (0.299 * red(color) + 0.587 * green(color) + 0.114 * blue(color)) / 255.0
        return if (luminance > 0.6) 0xFF0F172A.toInt() else 0xFFF8FAFC.toInt()
    }

    fun toHex(color: Int): String = String.format("#%06X", 0xFFFFFF and color)

    fun parseHex(value: String, fallback: Int): Int {
        val clean = value.trim().removePrefix("#")
        val body = when (clean.length) {
            6 -> clean
            8 -> clean.substring(2)
            3 -> buildString { clean.forEach { ch -> append(ch).append(ch) } }
            else -> return fallback
        }
        return try {
            0xFF000000.toInt() or body.toLong(16).toInt()
        } catch (_: Exception) {
            fallback
        }
    }

    /** Palet pilihan warna yang dipakai dialog pemilih warna. */
    val palette: List<Int> = listOf(
        0xFFFFFFFF.toInt(), 0xFFF8FAFC.toInt(), 0xFFCBD5E1.toInt(), 0xFF94A3B8.toInt(),
        0xFF64748B.toInt(), 0xFF475569.toInt(), 0xFF334155.toInt(), 0xFF1E293B.toInt(),
        0xFF0F172A.toInt(), 0xFF111827.toInt(), 0xFF0B1220.toInt(), 0xFF000000.toInt(),
        0xFFFF6B6B.toInt(), 0xFFFF9F43.toInt(), 0xFFFFD93D.toInt(), 0xFF6BCB77.toInt(),
        0xFF34D399.toInt(), 0xFF22D3EE.toInt(), 0xFF3D5AFE.toInt(), 0xFF7C4DFF.toInt(),
        0xFFEC4899.toInt(), 0xFFA855F7.toInt(), 0xFF14B8A6.toInt(), 0xFFF59E0B.toInt()
    )

    /** Warna pena cepat (dipakai tombol ganti warna jika diinginkan). */
    val penPalette: List<Int> = listOf(
        0xFFF8FAFC.toInt(), 0xFF111827.toInt(), 0xFFFF6B6B.toInt(), 0xFF34D399.toInt(),
        0xFF3D5AFE.toInt(), 0xFFFFD93D.toInt(), 0xFFEC4899.toInt(), 0xFF22D3EE.toInt()
    )
}
