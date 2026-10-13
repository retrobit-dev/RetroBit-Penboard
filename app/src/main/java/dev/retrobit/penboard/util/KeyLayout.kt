package dev.retrobit.penboard.util

import dev.retrobit.penboard.data.KeyDef
import kotlin.math.roundToInt

/**
 * Perhitungan lebar tombol. Fungsi murni (tanpa Android) supaya bisa diuji lewat unit test.
 *
 * Aturan:
 *  - Tombol dengan `flex = 0` berbagi sisa ruang secara proporsional terhadap `width`.
 *  - Tombol dengan `flex > 0` berbagi porsi ruang `flex` (dari total lebar baris) secara proporsional.
 */
object KeyLayout {

    fun computeWidths(keys: List<KeyDef>, availablePx: Int): IntArray {
        if (keys.isEmpty() || availablePx <= 0) return IntArray(keys.size)

        val fixedKeys = keys.filter { it.flex <= 0f }
        val flexKeys = keys.filter { it.flex > 0f }
        val fixedSum = fixedKeys.sumOf { it.width.toDouble() }.toFloat()
        val flexSum = flexKeys.sumOf { it.flex.toDouble() }.toFloat().coerceAtMost(0.85f)
        val fixedSpace = availablePx * (1f - flexSum)
        val flexSpace = availablePx * flexSum

        val widths = FloatArray(keys.size)
        keys.forEachIndexed { index, key ->
            widths[index] = when {
                key.flex > 0f && flexSum > 0f -> flexSpace * (key.flex / flexSum)
                fixedSum > 0f -> fixedSpace * (key.width / fixedSum)
                else -> availablePx.toFloat() / keys.size
            }
        }

        val result = IntArray(keys.size)
        var used = 0
        for (i in result.indices) {
            result[i] = widths[i].roundToInt().coerceAtLeast(1)
            used += result[i]
        }
        // Rapikan sisa pembulatan agar totalnya pas dengan ruang yang tersedia.
        var diff = availablePx - used
        var i = 0
        while (diff != 0 && result.isNotEmpty() && i < 10_000) {
            val idx = i % result.size
            if (diff > 0) {
                result[idx]++
                diff--
            } else if (result[idx] > 1) {
                result[idx]--
                diff++
            }
            i++
        }
        return result
    }
}
