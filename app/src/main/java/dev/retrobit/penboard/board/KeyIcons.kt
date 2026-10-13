package dev.retrobit.penboard.board

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import dev.retrobit.penboard.data.KeyAction

/** Ikon vektor sederhana untuk tombol aksi (tanpa font khusus, aman di semua perangkat). */
object KeyIcons {

    fun draw(canvas: Canvas, action: KeyAction, cx: Float, cy: Float, size: Float, paint: Paint) {
        val half = size / 2f
        val l = cx - half
        val r = cx + half
        val t = cy - half
        val b = cy + half
        val s = size

        when (action) {
            KeyAction.SPACE -> {
                canvas.drawLine(l + 0.16f * s, cy, r - 0.16f * s, cy, paint)
                canvas.drawLine(l + 0.16f * s, cy, l + 0.16f * s, cy - 0.18f * s, paint)
                canvas.drawLine(r - 0.16f * s, cy, r - 0.16f * s, cy - 0.18f * s, paint)
            }

            KeyAction.ENTER -> {
                canvas.drawLine(l + 0.28f * s, t + 0.12f * s, l + 0.28f * s, cy + 0.22f * s, paint)
                canvas.drawLine(l + 0.28f * s, cy + 0.22f * s, r - 0.12f * s, cy + 0.22f * s, paint)
                canvas.drawLine(r - 0.36f * s, cy - 0.04f * s, r - 0.12f * s, cy + 0.22f * s, paint)
                canvas.drawLine(r - 0.36f * s, cy + 0.48f * s, r - 0.12f * s, cy + 0.22f * s, paint)
            }

            KeyAction.BACKSPACE, KeyAction.DELETE_STROKE -> {
                val body = Path().apply {
                    moveTo(l + 0.34f * s, t + 0.14f * s)
                    lineTo(r - 0.08f * s, t + 0.14f * s)
                    lineTo(r - 0.08f * s, b - 0.14f * s)
                    lineTo(l + 0.34f * s, b - 0.14f * s)
                    lineTo(l + 0.06f * s, cy)
                    close()
                }
                canvas.drawPath(body, paint)
                if (action == KeyAction.BACKSPACE) {
                    canvas.drawLine(l + 0.46f * s, cy - 0.16f * s, l + 0.84f * s, cy + 0.16f * s, paint)
                    canvas.drawLine(l + 0.46f * s, cy + 0.16f * s, l + 0.84f * s, cy - 0.16f * s, paint)
                } else {
                    canvas.drawLine(l + 0.46f * s, cy, l + 0.86f * s, cy, paint)
                }
            }

            KeyAction.CLEAR -> {
                canvas.drawLine(l + 0.14f * s, t + 0.24f * s, r - 0.14f * s, t + 0.24f * s, paint)
                canvas.drawLine(cx - 0.14f * s, t + 0.24f * s, cx - 0.14f * s, t + 0.1f * s, paint)
                canvas.drawLine(cx + 0.14f * s, t + 0.24f * s, cx + 0.14f * s, t + 0.1f * s, paint)
                canvas.drawLine(cx - 0.14f * s, t + 0.1f * s, cx + 0.14f * s, t + 0.1f * s, paint)
                val body = Path().apply {
                    moveTo(l + 0.24f * s, t + 0.42f * s)
                    lineTo(l + 0.32f * s, b - 0.1f * s)
                    lineTo(r - 0.32f * s, b - 0.1f * s)
                    lineTo(r - 0.24f * s, t + 0.42f * s)
                }
                canvas.drawPath(body, paint)
                canvas.drawLine(cx - 0.1f * s, t + 0.56f * s, cx - 0.06f * s, b - 0.24f * s, paint)
                canvas.drawLine(cx + 0.1f * s, t + 0.56f * s, cx + 0.06f * s, b - 0.24f * s, paint)
            }

            KeyAction.UNDO -> curvedArrow(canvas, paint, l, r, t, b, toLeft = true)

            KeyAction.REDO -> curvedArrow(canvas, paint, l, r, t, b, toLeft = false)

            KeyAction.CURSOR_LEFT -> {
                val arrow = Path().apply {
                    moveTo(r - 0.14f * s, t + 0.14f * s)
                    lineTo(r - 0.14f * s, b - 0.14f * s)
                    lineTo(l + 0.14f * s, cy)
                    close()
                }
                canvas.drawPath(arrow, paint)
            }

            KeyAction.CURSOR_RIGHT -> {
                val arrow = Path().apply {
                    moveTo(l + 0.14f * s, t + 0.14f * s)
                    lineTo(l + 0.14f * s, b - 0.14f * s)
                    lineTo(r - 0.14f * s, cy)
                    close()
                }
                canvas.drawPath(arrow, paint)
            }

            KeyAction.SAVE_IMAGE -> {
                canvas.drawLine(cx, t + 0.1f * s, cx, cy + 0.16f * s, paint)
                canvas.drawLine(cx - 0.2f * s, cy - 0.04f * s, cx, cy + 0.16f * s, paint)
                canvas.drawLine(cx + 0.2f * s, cy - 0.04f * s, cx, cy + 0.16f * s, paint)
                canvas.drawLine(l + 0.14f * s, cy + 0.3f * s, l + 0.14f * s, b - 0.12f * s, paint)
                canvas.drawLine(l + 0.14f * s, b - 0.12f * s, r - 0.14f * s, b - 0.12f * s, paint)
                canvas.drawLine(r - 0.14f * s, b - 0.12f * s, r - 0.14f * s, cy + 0.3f * s, paint)
            }

            KeyAction.COLOR -> {
                val droplet = Path().apply {
                    moveTo(cx, t + 0.08f * s)
                    quadTo(r - 0.06f * s, cy + 0.02f * s, cx, b - 0.1f * s)
                    quadTo(l + 0.06f * s, cy + 0.02f * s, cx, t + 0.08f * s)
                    close()
                }
                canvas.drawPath(droplet, paint)
            }

            KeyAction.PROFILE_NEXT -> {
                canvas.drawLine(l + 0.3f * s, t + 0.12f * s, r - 0.08f * s, t + 0.12f * s, paint)
                canvas.drawLine(r - 0.08f * s, t + 0.12f * s, r - 0.08f * s, b - 0.34f * s, paint)
                canvas.drawLine(l + 0.08f * s, t + 0.3f * s, r - 0.3f * s, t + 0.3f * s, paint)
                canvas.drawLine(r - 0.3f * s, t + 0.3f * s, r - 0.3f * s, b - 0.12f * s, paint)
                canvas.drawLine(r - 0.3f * s, b - 0.12f * s, l + 0.08f * s, b - 0.12f * s, paint)
                canvas.drawLine(l + 0.08f * s, b - 0.12f * s, l + 0.08f * s, t + 0.3f * s, paint)
            }

            KeyAction.SCROLL_OLDER -> {
                canvas.drawLine(l + 0.14f * s, t + 0.18f * s, r - 0.14f * s, t + 0.18f * s, paint)
                canvas.drawLine(cx, b - 0.16f * s, l + 0.22f * s, cy + 0.04f * s, paint)
                canvas.drawLine(cx, b - 0.16f * s, r - 0.22f * s, cy + 0.04f * s, paint)
                canvas.drawLine(cx, b - 0.16f * s, cx, cy - 0.06f * s, paint)
            }

            KeyAction.SCROLL_NEWER -> {
                canvas.drawLine(l + 0.14f * s, b - 0.18f * s, r - 0.14f * s, b - 0.18f * s, paint)
                canvas.drawLine(cx, t + 0.16f * s, l + 0.22f * s, cy - 0.04f * s, paint)
                canvas.drawLine(cx, t + 0.16f * s, r - 0.22f * s, cy - 0.04f * s, paint)
                canvas.drawLine(cx, t + 0.16f * s, cx, cy + 0.06f * s, paint)
            }

            KeyAction.PROFILE_PREV -> {
                canvas.drawLine(r - 0.3f * s, t + 0.12f * s, l + 0.08f * s, t + 0.12f * s, paint)
                canvas.drawLine(l + 0.08f * s, t + 0.12f * s, l + 0.08f * s, b - 0.34f * s, paint)
                canvas.drawLine(r - 0.08f * s, t + 0.3f * s, l + 0.3f * s, t + 0.3f * s, paint)
                canvas.drawLine(l + 0.3f * s, t + 0.3f * s, l + 0.3f * s, b - 0.12f * s, paint)
                canvas.drawLine(l + 0.3f * s, b - 0.12f * s, r - 0.08f * s, b - 0.12f * s, paint)
                canvas.drawLine(r - 0.08f * s, b - 0.12f * s, r - 0.08f * s, t + 0.3f * s, paint)
            }

            KeyAction.RECOGNIZE -> {
                canvas.drawLine(l + 0.08f * s, t + 0.22f * s, cx - 0.04f * s, t + 0.22f * s, paint)
                canvas.drawLine(l + 0.08f * s, t + 0.5f * s, cx - 0.04f * s, t + 0.5f * s, paint)
                canvas.drawLine(l + 0.08f * s, b - 0.22f * s, cx - 0.2f * s, b - 0.22f * s, paint)
                canvas.drawLine(cx + 0.04f * s, cy, r - 0.2f * s, cy, paint)
                canvas.drawLine(r - 0.34f * s, cy - 0.13f * s, r - 0.2f * s, cy, paint)
                canvas.drawLine(r - 0.34f * s, cy + 0.13f * s, r - 0.2f * s, cy, paint)
            }

            KeyAction.ERASE -> {
                val body = Path().apply {
                    moveTo(l + 0.14f * s, b - 0.44f * s)
                    lineTo(r - 0.44f * s, t + 0.14f * s)
                    lineTo(r - 0.14f * s, t + 0.44f * s)
                    lineTo(l + 0.44f * s, b - 0.14f * s)
                    close()
                }
                canvas.drawPath(body, paint)
                canvas.drawLine(l + 0.3f * s, b - 0.28f * s, r - 0.28f * s, t + 0.3f * s, paint)
            }

            KeyAction.WRITE -> {
                val body = Path().apply {
                    moveTo(l + 0.12f * s, b - 0.12f * s)
                    lineTo(l + 0.16f * s, b - 0.36f * s)
                    lineTo(r - 0.36f * s, t + 0.12f * s)
                    lineTo(r - 0.12f * s, t + 0.36f * s)
                    lineTo(l + 0.36f * s, b - 0.16f * s)
                    close()
                }
                canvas.drawPath(body, paint)
                canvas.drawLine(l + 0.22f * s, b - 0.22f * s, l + 0.36f * s, b - 0.16f * s, paint)
            }

            KeyAction.HIDE -> {
                canvas.drawLine(l + 0.16f * s, cy - 0.16f * s, cx, cy + 0.18f * s, paint)
                canvas.drawLine(cx, cy + 0.18f * s, r - 0.16f * s, cy - 0.16f * s, paint)
            }

            KeyAction.INSERT -> Unit
        }
    }

    private fun curvedArrow(canvas: Canvas, paint: Paint, l: Float, r: Float, t: Float, b: Float, toLeft: Boolean) {
        val s = r - l
        val path = Path()
        if (toLeft) {
            path.moveTo(r - 0.14f * s, b - 0.2f * s)
            path.quadTo(r - 0.2f * s, t + 0.14f * s, l + 0.3f * s, t + 0.24f * s)
        } else {
            path.moveTo(l + 0.14f * s, b - 0.2f * s)
            path.quadTo(l + 0.2f * s, t + 0.14f * s, r - 0.3f * s, t + 0.24f * s)
        }
        canvas.drawPath(path, paint)
        if (toLeft) {
            canvas.drawLine(l + 0.3f * s, t + 0.24f * s, l + 0.52f * s, t + 0.06f * s, paint)
            canvas.drawLine(l + 0.3f * s, t + 0.24f * s, l + 0.5f * s, t + 0.46f * s, paint)
        } else {
            canvas.drawLine(r - 0.3f * s, t + 0.24f * s, r - 0.52f * s, t + 0.06f * s, paint)
            canvas.drawLine(r - 0.3f * s, t + 0.24f * s, r - 0.5f * s, t + 0.46f * s, paint)
        }
    }
}
