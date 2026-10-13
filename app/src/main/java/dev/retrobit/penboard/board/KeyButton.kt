package dev.retrobit.penboard.board

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import dev.retrobit.penboard.data.Colors
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import kotlin.math.min

/** Tampilan/warna tombol, diturunkan dari pengaturan profil. */
data class KeyStyle(
    val background: Int,
    val pressed: Int,
    val textColor: Int,
    val accent: Int,
    val cornerRadius: Float,
    val textSizePx: Float,
    val iconWidthPx: Float
) {
    companion object {
        fun from(config: PenboardConfig, density: Float): KeyStyle {
            val bg = Colors.withOpacity(config.keyBgColor, config.keyBgOpacity)
            val accent = config.keyAccentColor
            return KeyStyle(
                background = bg,
                pressed = Colors.blend(bg, accent, 0.45f),
                textColor = config.keyTextColor,
                accent = accent,
                cornerRadius = config.keyCornerDp * density,
                textSizePx = config.keyTextSizeSp * density,
                iconWidthPx = 2.1f * density
            )
        }
    }
}

/**
 * Tombol papan tulis: mendukung label teks, ikon aksi, status aktif, dan
 * pengulangan otomatis saat ditahan (mis. hapus huruf).
 */
class KeyButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var key: KeyDef? = null
        set(value) {
            field = value
            invalidate()
        }

    var style: KeyStyle = KeyStyle(
        background = 0xFF1E293B.toInt(),
        pressed = 0xFF334155.toInt(),
        textColor = 0xFFF8FAFC.toInt(),
        accent = 0xFF3D5AFE.toInt(),
        cornerRadius = 24f,
        textSizePx = 44f,
        iconWidthPx = 5f
    )
        set(value) {
            field = value
            invalidate()
        }

    /** Tombol sedang dipakai (mis. alat pena/penghapus yang aktif). */
    var active: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var onTrigger: ((KeyDef) -> Unit)? = null
    var hapticsEnabled: Boolean = true

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = false
    }
    private val rect = RectF()

    private var pressed = false
    private var repeatStarted = false

    init {
        isClickable = true
        isFocusable = false
    }

    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (!pressed) return
            val current = key ?: return
            repeatStarted = true
            onTrigger?.invoke(current)
            postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        backgroundPaint.color = when {
            active -> style.accent
            pressed -> style.pressed
            else -> style.background
        }
        canvas.drawRoundRect(rect, style.cornerRadius, style.cornerRadius, backgroundPaint)

        val current = key ?: return
        val activeTextColor = if (active) Colors.contrastFor(style.accent) else style.textColor

        if (current.action == KeyAction.INSERT) {
            textPaint.color = activeTextColor
            textPaint.textSize = style.textSizePx
            val available = w - 14f * resources.displayMetrics.density
            val measured = textPaint.measureText(current.text)
            if (measured > available && measured > 0f) {
                textPaint.textSize = style.textSizePx * (available / measured)
            }
            val metrics = textPaint.fontMetrics
            val baseline = h / 2f - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(current.text, w / 2f, baseline, textPaint)
        } else {
            iconPaint.color = activeTextColor
            iconPaint.strokeWidth = style.iconWidthPx
            val size = min(w, h) * 0.52f
            KeyIcons.draw(canvas, current.action, w / 2f, h / 2f, size, iconPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = true
                repeatStarted = false
                if (hapticsEnabled) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                key?.let { onTrigger?.invoke(it) }
                if (key?.repeat == true) {
                    postDelayed(repeatRunnable, REPEAT_START_MS)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                return true
            }

            MotionEvent.ACTION_UP -> {
                pressed = false
                removeCallbacks(repeatRunnable)
                invalidate()
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressed = false
                removeCallbacks(repeatRunnable)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** Dipakai oleh pembangun baris tombol. */
    fun applyPresentation(config: PenboardConfig, density: Float, haptics: Boolean) {
        style = KeyStyle.from(config, density)
        hapticsEnabled = haptics
    }

    private companion object {
        const val REPEAT_START_MS = 380L
        const val REPEAT_INTERVAL_MS = 55L
    }
}

/** Latar belakang membulat untuk tombol (dipakai saat tombol non-aktif). */
fun roundedBackground(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = radius
    setColor(color)
}
