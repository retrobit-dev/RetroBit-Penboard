package dev.retrobit.penboard.board

import dev.retrobit.penboard.util.BoardMath
import kotlin.math.sqrt

/**
 * Satu goresan stylus. Titik disimpan dalam koordinat konten papan (bukan koordinat layar)
 * supaya goresan tetap menempel pada barisnya saat papan "bergeser".
 *
 * Kelas ini sengaja **tanpa ketergantungan Android** agar bisa diuji dengan unit test dan
 * disimpan/dimuat lewat [StrokeCodec].
 */
class Stroke(val color: Int, val widthPx: Float) {

    private var data = FloatArray(INITIAL_CAPACITY * 2)

    private var minX = 0f
    private var maxX = 0f
    private var minY = 0f
    private var maxY = 0f

    var size: Int = 0
        private set

    val isEmpty: Boolean get() = size == 0

    fun add(x: Float, y: Float) {
        if ((size + 1) * 2 > data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size * 2] = x
        data[size * 2 + 1] = y
        if (size == 0) {
            minX = x
            maxX = x
            minY = y
            maxY = y
        } else {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        size++
    }

    fun xAt(index: Int): Float = data[index * 2]

    fun yAt(index: Int): Float = data[index * 2 + 1]

    fun startY(): Float = if (size == 0) 0f else data[1]

    fun left(): Float = minX

    fun top(): Float = minY

    fun right(): Float = maxX

    fun bottom(): Float = maxY

    /** Baris tempat goresan dimulai (0 = baris pertama konten). */
    fun lineIndex(gapPx: Float): Int = BoardMath.lineIndexOf(startY(), gapPx)

    /** Dipakai penghapus: apakah goresan menyentuh lingkaran (cx, cy, radius). */
    fun intersectsCircle(cx: Float, cy: Float, radius: Float): Boolean {
        if (size == 0) return false
        if (cx < minX - radius || cx > maxX + radius || cy < minY - radius || cy > maxY + radius) return false
        if (size == 1) return distance(data[0], data[1], cx, cy) <= radius
        for (i in 0 until size - 1) {
            val hit = distanceToSegment(
                cx, cy,
                data[i * 2], data[i * 2 + 1],
                data[(i + 1) * 2], data[(i + 1) * 2 + 1]
            )
            if (hit <= radius) return true
        }
        return false
    }

    private fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    private fun distanceToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) return distance(px, py, ax, ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0f, 1f)
        return distance(px, py, ax + t * dx, ay + t * dy)
    }

    private companion object {
        const val INITIAL_CAPACITY = 48
    }
}
