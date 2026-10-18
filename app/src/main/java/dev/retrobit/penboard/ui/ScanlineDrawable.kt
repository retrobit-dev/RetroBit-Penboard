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
 * Latar panel gaya ePaper retro: warna dasar, garis tepi, sudut sedikit membulat,
 * dan tekstur **garis pindaian** tipis yang bisa dimatikan pengguna.
 *
 * Bila [titleBarHeightPx] lebih dari nol, bagian atas digambar sebagai **bilah judul
 * jendela** (dengan garis pemisah di bawahnya) supaya panel terlihat seperti jendela
 * melayang -- lengkap dengan tombol minimize yang dipasang di atasnya.
 */
class ScanlineDrawable : Drawable() {

    var fillColor: Int = Color.BLACK
    var borderColor: Int = Color.GRAY
    var radiusPx: Float = 0f
    var scanlines: Boolean = true
    var lineSpacingPx: Float = 4f
    /** Tebal garis tepi panel (px). */
    var borderWidthPx: Float = 1f
    /** Tinggi bilah judul di bagian atas panel (0 = tanpa bilah judul). */
    var titleBarHeightPx: Float = 0f
    /** Warna bilah judul. */
    var titleBarColor: Int = Color.TRANSPARENT
    /** Warna garis pemisah di bawah bilah judul. */
    var dividerColor: Int = Color.TRANSPARENT

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val rect = RectF()
    private val clip = Path()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        val stroke = borderWidthPx.coerceAtLeast(0f)
        val inset = stroke / 2f
        rect.set(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset
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

        if (titleBarHeightPx > 0f) {
            // Bilah judul: ikut bentuk lengkung panel, lalu ditutup garis pemisah.
            val save = canvas.save()
            clip.reset()
            clip.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW)
            canvas.clipPath(clip)
            paint.color = titleBarColor
            canvas.drawRect(rect.left, rect.top, rect.right, rect.top + titleBarHeightPx, paint)
            paint.color = dividerColor
            canvas.drawRect(
                rect.left,
                rect.top + titleBarHeightPx,
                rect.right,
                rect.top + titleBarHeightPx + 1f,
                paint
            )
            canvas.restoreToCount(save)
        }

        borderPaint.color = borderColor
        borderPaint.strokeWidth = stroke.coerceAtLeast(1f)
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
