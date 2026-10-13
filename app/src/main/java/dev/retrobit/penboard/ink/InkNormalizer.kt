package dev.retrobit.penboard.ink

import dev.retrobit.penboard.board.InkPoint
import dev.retrobit.penboard.board.Stroke
import dev.retrobit.penboard.util.BoardMath
import kotlin.math.abs

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

    // --------------------------------------------------------------- percobaan kedua
    //
    // Kalau percobaan pertama tidak menghasilkan teks, tinta yang sama dikirim ulang
    // dengan ukuran berbeda. Sebagian mesin pengenalan lebih peka pada tulisan yang
    // kecil/rapat, jadi percobaan kedua ini dipaskan ke kotak tetap (tinggi 160).
    const val FIT_LINE_HEIGHT = 160f
    const val FIT_LARGE_HEIGHT = 320f
    const val FIT_MAX_WIDTH = 900f
    const val FIT_MARGIN = 12f
    const val FIT_MAX_SCALE = 8f

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

    /**
     * Percobaan kedua: tinta dipaskan ke kotak tetap berdasarkan **kotak batas
     * seluruh tulisan** (bukan tinggi baris panduan), lalu diletakkan mulai dari
     * [FIT_MARGIN]. Berguna bila percobaan pertama tidak menghasilkan teks.
     */
    fun normalizeFitted(
        strokes: List<Stroke>,
        lineGapPx: Float,
        targetHeight: Float = FIT_LINE_HEIGHT
    ): List<NormalizedStroke> {
        val usable = strokes.asSequence()
            .filter { it.points.size >= MIN_POINTS_PER_STROKE }
            .take(MAX_STROKES)
            .toList()
        if (usable.isEmpty()) return emptyList()

        val allPoints = usable.flatMap { it.points }
        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }
        val width = (maxX - minX).coerceAtLeast(1f)
        val height = (maxY - minY).coerceAtLeast(1f)

        var scale = (if (targetHeight > 1f) targetHeight else FIT_LINE_HEIGHT) / height
        if (width * scale > FIT_MAX_WIDTH) scale = FIT_MAX_WIDTH / width
        scale = scale.coerceIn(0.01f, FIT_MAX_SCALE)

        val gap = if (lineGapPx > 1f) lineGapPx else 1f
        val rawLines = usable.map { BoardMath.lineIndexOf(it.points.first().y, gap, 0) }
        val compactIndex = rawLines.distinct().sorted().withIndex()
            .associate { (index, line) -> line to index }

        var budget = MAX_POINTS
        val result = mutableListOf<NormalizedStroke>()
        for ((index, stroke) in usable.withIndex()) {
            if (budget <= 0) break
            val points = stroke.points.take(budget).map { point ->
                InkPoint(
                    x = (point.x - minX) * scale + FIT_MARGIN,
                    y = (point.y - minY) * scale + FIT_MARGIN
                )
            }
            budget -= points.size
            result.add(NormalizedStroke(points, compactIndex.getValue(rawLines[index])))
        }
        return result
    }

    /**
     * Beberapa versi tinta yang sama untuk dicoba berurutan oleh mesin pengenalan:
     * yang pertama normalisasi biasa (per baris panduan), lalu dua versi yang
     * dipaskan ke kotak tetap dengan ukuran berbeda. Dipakai bila percobaan
     * sebelumnya tidak menghasilkan teks sama sekali.
     */
    fun variants(strokes: List<Stroke>, lineGapPx: Float): List<List<NormalizedStroke>> {
        val ladder = listOf(
            normalize(strokes, lineGapPx),
            normalizeFitted(strokes, lineGapPx, FIT_LINE_HEIGHT),
            normalizeFitted(strokes, lineGapPx, FIT_LARGE_HEIGHT)
        )
        val result = mutableListOf<List<NormalizedStroke>>()
        for (variant in ladder) {
            if (variant.isEmpty()) continue
            if (result.any { sameInk(it, variant) }) continue
            result.add(variant)
        }
        return result
    }

    /** Apakah dua versi tinta pada dasarnya sama (dipakai agar tidak mencoba dua kali hal yang sama). */
    private fun sameInk(a: List<NormalizedStroke>, b: List<NormalizedStroke>): Boolean {
        if (a.size != b.size) return false
        return a.indices.all { index ->
            val first = a[index].points
            val second = b[index].points
            first.size == second.size && first.indices.all { point ->
                abs(first[point].x - second[point].x) < SAME_INK_TOLERANCE &&
                    abs(first[point].y - second[point].y) < SAME_INK_TOLERANCE
            }
        }
    }

    /** Selisih (satuan tinta) yang masih dianggap sama. */
    const val SAME_INK_TOLERANCE = 0.5f
}
