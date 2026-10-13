package dev.retrobit.penboard.board

import android.graphics.Path
import android.util.LruCache

/**
 * Cache [Path] untuk goresan. [Path] memerlukan Android, jadi pembuatannya dipisah
 * dari [Stroke] (yang sengaja bebas Android agar bisa diuji).
 */
internal object StrokePaths {

    private val cache = LruCache<Stroke, Path>(400)

    fun of(stroke: Stroke): Path {
        cache.get(stroke)?.let { return it }
        val path = build(stroke)
        cache.put(stroke, path)
        return path
    }

    private fun build(stroke: Stroke): Path {
        val path = Path()
        if (stroke.size == 0) return path
        if (stroke.size == 1) {
            path.moveTo(stroke.xAt(0), stroke.yAt(0))
            path.lineTo(stroke.xAt(0), stroke.yAt(0))
            return path
        }
        if (stroke.size == 2) {
            path.moveTo(stroke.xAt(0), stroke.yAt(0))
            path.lineTo(stroke.xAt(1), stroke.yAt(1))
            return path
        }
        path.moveTo(stroke.xAt(0), stroke.yAt(0))
        var i = 1
        while (i < stroke.size - 1) {
            val cx = stroke.xAt(i)
            val cy = stroke.yAt(i)
            val nx = stroke.xAt(i + 1)
            val ny = stroke.yAt(i + 1)
            path.quadTo(cx, cy, (cx + nx) / 2f, (cy + ny) / 2f)
            i++
        }
        path.lineTo(stroke.xAt(stroke.size - 1), stroke.yAt(stroke.size - 1))
        return path
    }
}
