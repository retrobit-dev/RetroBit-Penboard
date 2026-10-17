package dev.retrobit.penboard.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Petunjuk sudut pengubah ukuran: tiga garis pendek bertingkat di pojok kanan bawah
 * papan (gaya pixel art, sama seperti ikon papan). Digambar tajam tanpa anti-alias
 * supaya tetap terlihat sebagai piksel.
 */
class ResizeCornerDrawable : Drawable() {

    var color: Int = Color.GRAY
    var strokePx: Float = 4f

    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        paint.color = color
        val step = strokePx.coerceAtLeast(1f)
        paint.strokeWidth = step
        for (index in 1..3) {
            val length = step * (index * 2 - 1)
            val offset = step * (3 - index) * 2 - step / 2f
            val x = bounds.right - step / 2f
            val y = bounds.bottom - offset
            canvas.drawLine(x, y, x - length, y, paint)
            val x2 = bounds.right - offset
            val y2 = bounds.bottom - step / 2f
            canvas.drawLine(x2, y2, x2, y2 - length, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Drawable", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
