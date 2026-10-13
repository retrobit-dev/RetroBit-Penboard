package dev.retrobit.penboard.ink

import dev.retrobit.penboard.board.InkPoint
import dev.retrobit.penboard.board.Stroke
import dev.retrobit.penboard.util.BoardMath

/** Goresan yang sudah dinormalkan ke satu bidang koordinat untuk mesin pengenalan. */
data class NormalizedStroke(val points: List<InkPoint>, val line: Int)

/**
 * Menyiapkan goresan papan tulis untuk pengenalan tulisan (murni Kotlin, mudah diuji).
 *
 * Tiap baris panduan dipadatkan (baris kosong di antara tulisan dibuang) dan
 * tinggi satu baris tulisan dinormalkan ke [LINE_HEIGHT] satuan supaya mesin
 * pengenalan menerima ukuran yang konsisten, tanpa peduli tinggi papan pada layar.
 */
object InkNormalizer {

    /** Tinggi satu baris setelah normalisasi (satuan bebas). */
    const val LINE_HEIGHT = 64f

    /** Jarak antar baris setelah normalisasi. */
    const val LINE_STEP = 88f

    /** Lebar maksimal bidang tulisan setelah normalisasi. */
    const val MAX_WIDTH = 720f

    const val MAX_STROKES = 120
    const val MAX_POINTS = 8000
    const val MIN_POINTS_PER_STROKE = 2

    fun normalize(strokes: List<Stroke>, lineGapPx: Float): List<NormalizedStroke> {
        val usable = strokes.asSequence()
            .filter { it.points.size >= MIN_POINTS_PER_STROKE }
            .take(MAX_STROKES)
            .toList()
        if (usable.isEmpty()) return emptyList()

        // Baris asal setiap goresan ditentukan dari titik pertamanya.
        val rawLines = usable.map { BoardMath.lineIndexOf(it.points.first().y, lineGapPx, 0) }
        val compactIndex = rawLines.distinct().sorted()
            .withIndex()
            .associate { (index, line) -> line to index }

        // Skala: satu baris papan menjadi setinggi LINE_HEIGHT satuan.
        val gap = if (lineGapPx > 1f) lineGapPx else 1f
        var scale = LINE_HEIGHT / gap
        val maxX = usable.flatMap { it.points }.maxOf { it.x }
        if (maxX > 0f) scale = minOf(scale, MAX_WIDTH / maxX)

        var budget = MAX_POINTS
        val result = mutableListOf<NormalizedStroke>()
        for ((index, stroke) in usable.withIndex()) {
            if (budget <= 0) break
            val rawLine = rawLines[index]
            val compactLine = compactIndex.getValue(rawLine)
            val lineTop = rawLine * lineGapPx
            val points = stroke.points.take(budget).map { point ->
                InkPoint(
                    x = point.x * scale,
                    y = compactLine * LINE_STEP + (point.y - lineTop) * scale
                )
            }
            budget -= points.size
            result.add(NormalizedStroke(points, compactLine))
        }
        return result
    }
}
