package dev.retrobit.penboard.board

import java.util.Locale
import kotlin.math.roundToInt

/** Isi papan yang bisa disimpan dan dimuat kembali. */
data class BoardSnapshot(val strokes: List<Stroke>, val shiftPx: Float)

/**
 * Format penyimpanan goresan yang ringkas dan tahan rusak.
 *
 * ```
 * PENBOARD-BOARD 1
 * shift=123.4
 * S -2298474 3.2 41,88 43,90 46,94 ...
 * ```
 */
object StrokeCodec {

    private const val HEADER = "PENBOARD-BOARD 1"
    private const val MAX_POINTS_PER_STROKE = 20_000

    fun encode(strokes: List<Stroke>, shiftPx: Float): String {
        val builder = StringBuilder(HEADER).append('\n')
        builder.append("shift=").append(format(shiftPx)).append('\n')
        strokes.forEach { stroke ->
            if (stroke.isEmpty) return@forEach
            builder.append("S ").append(stroke.color).append(' ')
            builder.append(format(stroke.widthPx)).append(' ')
            for (index in 0 until stroke.size) {
                if (index > 0) builder.append(' ')
                builder.append(stroke.xAt(index).roundToInt())
                builder.append(',')
                builder.append(stroke.yAt(index).roundToInt())
            }
            builder.append('\n')
        }
        return builder.toString()
    }

    fun decode(text: String?): BoardSnapshot {
        if (text.isNullOrBlank()) return BoardSnapshot(emptyList(), 0f)
        val strokes = mutableListOf<Stroke>()
        var shift = 0f
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("PENBOARD-BOARD") -> Unit
                line.startsWith("shift=") -> shift = line.removePrefix("shift=").toFloatOrNull() ?: 0f
                line.startsWith("S ") -> parseStroke(line)?.let { strokes.add(it) }
            }
        }
        return BoardSnapshot(strokes, shift.coerceAtLeast(0f))
    }

    private fun parseStroke(line: String): Stroke? {
        val parts = line.split(' ')
        if (parts.size < 4) return null
        val color = parts[1].toLongOrNull()?.toInt() ?: return null
        val width = parts[2].toFloatOrNull()?.takeIf { it > 0f } ?: return null
        val stroke = Stroke(color, width)
        var points = 0
        for (index in 3 until parts.size) {
            if (points >= MAX_POINTS_PER_STROKE) break
            val pair = parts[index]
            val comma = pair.indexOf(',')
            if (comma <= 0) continue
            val x = pair.substring(0, comma).toFloatOrNull() ?: continue
            val y = pair.substring(comma + 1).toFloatOrNull() ?: continue
            stroke.add(x, y)
            points++
        }
        return if (stroke.isEmpty) null else stroke
    }

    private fun format(value: Float): String = String.format(Locale.US, "%.1f", value)
}
