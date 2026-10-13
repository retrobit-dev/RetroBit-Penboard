package dev.retrobit.penboard.data

import android.graphics.Color

/** Utilitas warna: menggabungkan warna dasar dengan tingkat transparansi (0..100). */
object Colors {
    fun withOpacity(color: Int, percent: Int): Int {
        val a = (percent.coerceIn(0, 100) * 255 / 100)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    fun toHex(color: Int): String = String.format("#%06X", 0xFFFFFF and color)

    fun parseHex(value: String, fallback: Int): Int = try {
        val clean = value.trim().removePrefix("#")
        val body = when (clean.length) {
            6 -> clean
            8 -> clean.substring(2)
            3 -> buildString { clean.forEach { append(it).append(it) } }
            else -> return fallback
        }
        (0xFF000000.toInt() or body.toLong(16).toInt())
    } catch (_: Exception) {
        fallback
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
