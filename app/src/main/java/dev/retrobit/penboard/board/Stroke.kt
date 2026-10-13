package dev.retrobit.penboard.board

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Satu goresan stylus. Titik disimpan dalam koordinat konten papan (bukan koordinat layar)
 * supaya goresan tetap menempel pada barisnya saat papan "bergeser".
 */
class Stroke(val color: Int, val widthPx: Float) {

    private var data = FloatArray(INITIAL_CAPACITY * 2)
    private var cachedPath: Path? = null
    private var cachedBounds: RectF? = null

    var size: Int = 0
        private set

    val isEmpty: Boolean get() = size == 0

    fun add(x: Float, y: Float) {
        if ((size + 1) * 2 > data.size) {
            data = data.copyOf(data.size * 2)
        }
        data[size * 2] = x
        data[size * 2 + 1] = y
        size++
        cachedPath = null
        cachedBounds = null
    }

    fun xAt(index: Int): Float = data[index * 2]

    fun yAt(index: Int): Float = data[index * 2 + 1]

    /** Baris tempat goresan dimulai (0 = baris pertama konten). */
    fun lineIndex(gapPx: Float): Int {
        if (size == 0 || gapPx <= 0f) return 0
        return floor(data[1] / gapPx).toInt().coerceAtLeast(0)
    }

    fun startY(): Float = if (size == 0) 0f else data[1]

    fun path(): Path {
        cachedPath?.let { return it }
        val path = Path()
        if (size == 1) {
            path.moveTo(data[0], data[1])
            path.lineTo(data[0], data[1])
        } else if (size == 2) {
            path.moveTo(data[0], data[1])
            path.lineTo(data[2], data[3])
        } else {
            path.moveTo(data[0], data[1])
            var i = 1
            while (i < size - 1) {
                val cx = data[i * 2]
                val cy = data[i * 2 + 1]
                val nx = data[(i + 1) * 2]
                val ny = data[(i + 1) * 2 + 1]
                path.quadTo(cx, cy, (cx + nx) / 2f, (cy + ny) / 2f)
                i++
            }
            path.lineTo(data[(size - 1) * 2], data[(size - 1) * 2 + 1])
        }
        cachedPath = path
        return path
    }

    fun bounds(): RectF {
        cachedBounds?.let { return it }
        val rect = RectF()
        if (size > 0) {
            var minX = data[0]
            var maxX = data[0]
            var minY = data[1]
            var maxY = data[1]
            for (i in 1 until size) {
                val x = data[i * 2]
                val y = data[i * 2 + 1]
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
            rect.set(minX, minY, maxX, maxY)
        }
        cachedBounds = rect
        return rect
    }

    /** Dipakai penghapus: apakah goresan menyentuh lingkaran (cx, cy, radius). */
    fun intersectsCircle(cx: Float, cy: Float, radius: Float): Boolean {
        if (size == 0) return false
        val box = bounds()
        if (cx < box.left - radius || cx > box.right + radius ||
            cy < box.top - radius || cy > box.bottom + radius
        ) return false
        if (size == 1) {
            return distance(data[0], data[1], cx, cy) <= radius
        }
        for (i in 0 until size - 1) {
            if (distanceToSegment(cx, cy, data[i * 2], data[i * 2 + 1], data[(i + 1) * 2], data[(i + 1) * 2 + 1]) <= radius) {
                return true
            }
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
        var t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared
        t = t.coerceIn(0f, 1f)
        return distance(px, py, ax + t * dx, ay + t * dy)
    }

    private companion object {
        const val INITIAL_CAPACITY = 48
    }
}
