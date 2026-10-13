package dev.retrobit.penboard.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Tombol grid: kotak membulat dengan label, umpan balik tekan, getar, dan
 * pengulangan otomatis bila ditahan (dipakai untuk tombol hapus).
 */
class KeyButton(context: Context) : AppCompatTextView(context) {

    var onTap: (() -> Unit)? = null
    var repeatOnHold: Boolean = false

    private var haptics: Boolean = true
    private val handler = Handler(Looper.getMainLooper())
    private var repeated = false

    private val repeatTask = object : Runnable {
        override fun run() {
            repeated = true
            onTap?.invoke()
            handler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    init {
        isClickable = true
        isFocusable = false
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setPadding(0, 0, 0, 0)
        setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun style(
        radiusPx: Float,
        fillColor: Int,
        borderColor: Int,
        labelColor: Int,
        textSizePx: Float,
        haptics: Boolean
    ) {
        this.haptics = haptics
        setTextColor(labelColor)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
        background = StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                rounded(radiusPx, blend(fillColor, Color.WHITE, 0.24f), borderColor)
            )
            addState(intArrayOf(), rounded(radiusPx, fillColor, borderColor))
        }
    }

    override fun performClick(): Boolean = super.performClick()

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                repeated = false
                if (haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (repeatOnHold) handler.postDelayed(repeatTask, HOLD_DELAY_MS)
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                handler.removeCallbacks(repeatTask)
                if (!repeated) {
                    performClick()
                    onTap?.invoke()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                handler.removeCallbacks(repeatTask)
            }
            else -> return false
        }
        return true
    }

    private fun rounded(radiusPx: Float, fill: Int, border: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(fill)
            setStroke(max(1f, radiusPx / 8f).roundToInt(), border)
        }

    private fun blend(color: Int, target: Int, ratio: Float): Int {
        fun mix(from: Int, to: Int): Int = (from + (to - from) * ratio).roundToInt().coerceIn(0, 255)
        return Color.argb(
            Color.alpha(color),
            mix(Color.red(color), Color.red(target)),
            mix(Color.green(color), Color.green(target)),
            mix(Color.blue(color), Color.blue(target))
        )
    }

    companion object {
        const val HOLD_DELAY_MS = 380L
        const val REPEAT_INTERVAL_MS = 90L
    }
}
