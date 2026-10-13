package dev.retrobit.penboard.ink

import dev.retrobit.penboard.board.Stroke

/** Titik untuk pengenalan tulisan (tanpa ketergantungan Android/ML Kit). */
data class InkPoint(val x: Float, val y: Float)

/** Satu goresan untuk pengenalan tulisan. */
data class InkLine(val points: List<InkPoint>)

/** Hasil pengambilan goresan dari papan, siap dikirim ke mesin pengenal. */
data class InkCapture(val lines: List<InkLine>, val width: Int, val height: Int) {
    val isEmpty: Boolean get() = lines.isEmpty()

    val pointCount: Int get() = lines.sumOf { it.points.size }
}

/** Status model bahasa. */
enum class ModelStatus { READY, MISSING, UNKNOWN }

/** Sebab kegagalan pengenalan tulisan. */
enum class RecognitionError { NOT_SUPPORTED, EMPTY, MODEL_MISSING, FAILED }

fun interface RecognitionCallback {
    fun onResult(text: String?, error: RecognitionError?)
}

/**
 * Mengambil goresan dari beberapa baris terakhir papan untuk dikenali.
 * Fungsi murni (tanpa Android) sehingga perilakunya bisa diuji.
 */
object InkExtractor {

    const val MAX_STROKES = 80
    const val MAX_POINTS = 8000

    fun capture(
        strokes: List<Stroke>,
        gapPx: Float,
        lines: Int,
        width: Int,
        height: Int
    ): InkCapture {
        if (strokes.isEmpty() || gapPx <= 0f) return InkCapture(emptyList(), width, height)
        val lineCount = lines.coerceIn(1, 12)
        val lastLine = strokes.last().lineIndex(gapPx)
        val firstLine = (lastLine - (lineCount - 1)).coerceAtLeast(0)
        val offsetY = firstLine * gapPx

        val candidates = strokes
            .filter { it.lineIndex(gapPx) >= firstLine && it.size >= 2 }
            .takeLast(MAX_STROKES)

        val result = ArrayList<InkLine>(candidates.size)
        var points = 0
        for (stroke in candidates) {
            if (points >= MAX_POINTS) break
            val mapped = ArrayList<InkPoint>(stroke.size)
            for (index in 0 until stroke.size) {
                if (points >= MAX_POINTS) break
                mapped.add(InkPoint(stroke.xAt(index), (stroke.yAt(index) - offsetY).coerceAtLeast(0f)))
                points++
            }
            if (mapped.size >= 2) result.add(InkLine(mapped))
        }
        return InkCapture(result, width, height)
    }
}
