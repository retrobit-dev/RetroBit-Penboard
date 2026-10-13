package dev.retrobit.penboard.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Latar panel gaya ePaper retro: warna dasar, garis tepi 1 px, sudut sedikit membulat,
 * dan tekstur **garis pindaian** tipis yang bisa dimatikan pengguna.
 */
class ScanlineDrawable : Drawable() {

    var fillColor: Int = Color.BLACK
    var borderColor: Int = Color.GRAY
    var radiusPx: Float = 0f
    var scanlines: Boolean = true
    var lineSpacingPx: Float = 4f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val rect = RectF()
    private val clip = Path()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        rect.set(
            bounds.left + 0.5f,
            bounds.top + 0.5f,
            bounds.right - 0.5f,
            bounds.bottom - 0.5f
        )

        paint.style = Paint.Style.FILL
        paint.color = fillColor
        canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)

        if (scanlines) {
            val save = canvas.save()
            clip.reset()
            clip.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW)
            canvas.clipPath(clip)
            paint.color = withAlpha(borderColor, 26)
            var y = rect.top + lineSpacingPx
            while (y < rect.bottom) {
                canvas.drawRect(rect.left, y, rect.right, y + 1f, paint)
                y += lineSpacingPx
            }
            canvas.restoreToCount(save)
        }

        borderPaint.color = borderColor
        canvas.drawRoundRect(rect, radiusPx, radiusPx, borderPaint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        borderPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        borderPaint.colorFilter = colorFilter
    }

    @Deprecated("Diperlukan oleh Drawable", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        /** Warna dengan alfa diubah (0-255). */
        fun withAlpha(color: Int, alpha: Int): Int =
            (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
