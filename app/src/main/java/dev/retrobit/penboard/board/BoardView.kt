package dev.retrobit.penboard.board

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Papan tulis stylus.
 *
 * - Menulis dengan stylus (atau jari bila [allowFinger] aktif).
 * - Tombol samping pena: primer = [onPenButtonPrimary], sekunder = [onPenButtonSecondary].
 * - Ujung penghapus stylus (bila ada) menghapus goresan yang tersentuh.
 * - [edgeToEdge] membuat papan menempel tepi kiri (mode "papan di kiri, tombol di kanan").
 */
class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onStrokeCommitted: (() -> Unit)? = null
    var onContentChanged: (() -> Unit)? = null
    var onPenButtonPrimary: (() -> Unit)? = null
    var onPenButtonSecondary: (() -> Unit)? = null

    private val strokes = mutableListOf<Stroke>()
    private val redoStack = mutableListOf<Stroke>()
    private val strokePaths = mutableListOf<Path>()
    private val currentPath = Path()
    private var current: Stroke? = null

    private val density = resources.displayMetrics.density

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    private val backgroundDrawable = GradientDrawable()

    /** Jarak baris panduan (px). */
    var lineGapPx: Float = DEFAULT_LINE_GAP_DP * density
        set(value) {
            field = if (value < 6f * density) 6f * density else value
            invalidate()
        }

    var showLines: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var penColor: Int = 0xFFEF4444.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var penWidthPx: Float = 3f * density
        set(value) {
            field = if (value < 1f) 1f else value
            invalidate()
        }

    /** Bila false, hanya stylus yang bisa menulis (sentuhan jari diabaikan). */
    var allowFinger: Boolean = false

    var haptics: Boolean = true

    /** Papan menempel tepi kiri dan memakai seluruh lebar yang tersedia. */
    var edgeToEdge: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            applyBackground()
            invalidate()
        }

    var cornerRadiusPx: Float = 16f * density
        set(value) {
            field = value
            applyBackground()
        }

    var boardColor: Int = Color.WHITE
        set(value) {
            field = value
            applyBackground()
        }

    var boardAlpha: Int = 245
        set(value) {
            field = value.coerceIn(0, 255)
            applyBackground()
        }

    var lineColor: Int = 0xFF94A3B8.toInt()
        set(value) {
            field = value
            invalidate()
        }

    val hasContent: Boolean get() = strokes.isNotEmpty()

    val strokeCount: Int get() = strokes.size

    /** Salinan goresan untuk pengenalan tulisan. */
    fun contentStrokes(): List<Stroke> = strokes.toList()

    fun undo(): Boolean {
        if (strokes.isEmpty()) return false
        val removed = strokes.removeAt(strokes.size - 1)
        strokePaths.removeAt(strokePaths.size - 1)
        redoStack.add(removed)
        notifyChanged()
        return true
    }

    fun redo(): Boolean {
        val restored = redoStack.removeLastOrNull() ?: return false
        strokes.add(restored)
        strokePaths.add(pathOf(restored))
        notifyChanged()
        return true
    }

    fun deleteLastStroke(): Boolean = undo()

    fun clear() {
        if (strokes.isEmpty()) return
        redoStack.clear()
        strokes.clear()
        strokePaths.clear()
        notifyChanged()
    }

    /** Hapus goresan yang melewati sekitar titik (dipakai ujung penghapus stylus). */
    fun removeStrokesNear(x: Float, y: Float, radiusPx: Float = 22f * density): Boolean {
        var changed = false
        for (index in strokes.indices.reversed()) {
            val hit = strokes[index].points.any { abs(it.x - x) <= radiusPx && abs(it.y - y) <= radiusPx }
            if (hit) {
                strokes.removeAt(index)
                strokePaths.removeAt(index)
                changed = true
            }
        }
        if (changed) notifyChanged()
        return changed
    }

    // ------------------------------------------------------------------ menggambar

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showLines) {
            val left = if (edgeToEdge) 0f else insetH
            val right = if (edgeToEdge) width.toFloat() else width - insetH
            var y = lineGapPx
            while (y < height - 1f) {
                canvas.drawLine(left, y, right, y, linePaint)
                y += lineGapPx
            }
        }
        for (index in strokePaths.indices) {
            val stroke = strokes.getOrNull(index) ?: continue
            strokePaint.color = stroke.color
            strokePaint.strokeWidth = stroke.widthPx
            canvas.drawPath(strokePaths[index], strokePaint)
        }
        current?.let { stroke ->
            strokePaint.color = stroke.color
            strokePaint.strokeWidth = stroke.widthPx
            canvas.drawPath(currentPath, strokePaint)
        }
    }

    private val insetH: Float get() = if (edgeToEdge) 3f * density else 12f * density

    private fun applyBackground() {
        backgroundDrawable.shape = GradientDrawable.RECTANGLE
        val radius = cornerRadiusPx
        val left = if (edgeToEdge) 0f else radius
        backgroundDrawable.cornerRadii = floatArrayOf(left, left, radius, radius, radius, radius, left, left)
        backgroundDrawable.setColor(withAlpha(boardColor, boardAlpha))
        backgroundDrawable.setStroke(
            (density).toInt().coerceAtLeast(1),
            withAlpha(lineColor, 110)
        )
        background = backgroundDrawable
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyBackground()
    }

    override fun performClick(): Boolean = super.performClick()

    // ------------------------------------------------------------------ sentuhan

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.isPenButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY)) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onPenButtonPrimary?.invoke()
            return true
        }
        if (event.isPenButtonPressed(MotionEvent.BUTTON_STYLUS_SECONDARY)) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onPenButtonSecondary?.invoke()
            return true
        }

        val isStylus = event.toolType == MotionEvent.TOOL_TYPE_STYLUS
        val isEraser = event.toolType == MotionEvent.TOOL_TYPE_ERASER
        if (!isStylus && !isEraser && !allowFinger) return false

        if (isEraser) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> removeStrokesNear(event.x, event.y)
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startStroke(event.x, event.y)
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.historySize) {
                    extendStroke(event.getHistoricalX(index), event.getHistoricalY(index))
                }
                extendStroke(event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finishStroke()
            else -> return false
        }
        return true
    }

    private fun startStroke(x: Float, y: Float) {
        parent?.requestDisallowInterceptTouchEvent(true)
        if (haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val stroke = Stroke(color = penColor, widthPx = penWidthPx)
        stroke.points.add(InkPoint(x, y))
        current = stroke
        currentPath.reset()
        currentPath.moveTo(x, y)
        redoStack.clear()
        invalidate()
    }

    private fun extendStroke(x: Float, y: Float) {
        val stroke = current ?: return
        val last = stroke.last()
        if (last != null && hypot(x - last.x, y - last.y) < 1.2f) return
        stroke.points.add(InkPoint(x, y))
        currentPath.lineTo(x, y)
        invalidate()
    }

    private fun finishStroke() {
        val stroke = current ?: return
        current = null
        strokes.add(stroke)
        strokePaths.add(pathOf(currentPath))
        currentPath.reset()
        invalidate()
        onStrokeCommitted?.invoke()
        onContentChanged?.invoke()
    }

    private fun notifyChanged() {
        invalidate()
        onContentChanged?.invoke()
    }

    private fun pathOf(stroke: Stroke): Path = Path().also { path ->
        var first = true
        for (point in stroke.points) {
            if (first) {
                path.moveTo(point.x, point.y)
                first = false
            } else {
                path.lineTo(point.x, point.y)
            }
        }
    }

    private fun pathOf(source: Path): Path = Path(source)

    companion object {
        const val DEFAULT_LINE_GAP_DP = 44f

        /** Warna dengan alfa diubah (0-255). */
        fun withAlpha(color: Int, alpha: Int): Int =
            (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
