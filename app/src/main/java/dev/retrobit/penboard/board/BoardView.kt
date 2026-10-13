package dev.retrobit.penboard.board

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import dev.retrobit.penboard.util.BoardMath
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Papan tulis stylus.
 *
 * - Menulis dengan stylus (atau jari bila [allowFinger] aktif).
 * - Ketebalan goresan mengikuti tekanan pena bila [pressureEnabled] aktif.
 * - Tombol samping pena: primer = [onPenButtonPrimary], sekunder = [onPenButtonSecondary].
 * - Ujung penghapus stylus (bila ada) menghapus goresan yang tersentuh.
 * - [edgeToEdge] membuat papan menempel tepi kiri (mode "papan di kiri, tombol di kanan").
 * - [beginPending] menampilkan garis hitung mundur "menunggu jadi teks",
 *   [showFlash] menampilkan kilasan teks hasil input di bawah papan.
 */
class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onStrokeCommitted: (() -> Unit)? = null

    /** Pena baru menyentuh papan: batalkan hitungan tunggu supaya tulisan tidak terpotong. */
    var onStrokeStarted: (() -> Unit)? = null
    var onContentChanged: (() -> Unit)? = null
    var onPenButtonPrimary: (() -> Unit)? = null
    var onPenButtonSecondary: (() -> Unit)? = null

    private val strokes = mutableListOf<Stroke>()
    private val redoStack = mutableListOf<Stroke>()
    private val strokePaths = mutableListOf<Path>()
    private val currentPath = Path()
    private var current: Stroke? = null
    private var lastInk: InkPoint? = null

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
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundDrawable = GradientDrawable()
    private val flashBox = RectF()

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

    /** Ketebalan goresan mengikuti tekanan stylus (M-Pen dan pena sejenis). */
    var pressureEnabled: Boolean = true

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

    /** Warna aksen: dipakai indikator menunggu dan kilasan hasil input. */
    var accentColor: Int = 0xFF0EA5E9.toInt()
        set(value) {
            field = value
            invalidate()
        }

    val hasContent: Boolean get() = strokes.isNotEmpty()

    /** Benar bila pena masih menempel di papan (ada goresan yang belum diangkat). */
    val isWriting: Boolean get() = current != null

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
        // Goresan yang belum diangkat ikut dibuang supaya tidak tertinggal di papan.
        current = null
        currentPath.reset()
        lastInk = null
        if (strokes.isEmpty()) {
            invalidate()
            return
        }
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

    // ------------------------------------------------------------------ indikator

    private var pendingFraction = 0f
    private var pendingStarted = 0L
    private var pendingDuration = 0L

    private val pendingTick = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - pendingStarted
            if (elapsed >= pendingDuration) {
                pendingFraction = 0f
                invalidate()
                return
            }
            pendingFraction = (elapsed.toFloat() / pendingDuration).coerceIn(0f, 1f)
            invalidate()
            postDelayed(this, TICK_MS)
        }
    }

    /** Mulai garis hitung mundur "menunggu jadi teks" selama [durationMs]. */
    fun beginPending(durationMs: Int) {
        removeCallbacks(pendingTick)
        pendingDuration = durationMs.toLong().coerceAtLeast(1L)
        pendingStarted = SystemClock.uptimeMillis()
        pendingFraction = 0.01f
        invalidate()
        postDelayed(pendingTick, TICK_MS)
    }

    /** Hentikan hitung mundur (mis. karena tulisan sudah jadi teks). */
    fun cancelPending() {
        removeCallbacks(pendingTick)
        if (pendingFraction == 0f) return
        pendingFraction = 0f
        invalidate()
    }

    private var flashText: String? = null
    private var flashStarted = 0L
    private var flashDuration = 0L

    private val flashTick = object : Runnable {
        override fun run() {
            if (SystemClock.uptimeMillis() - flashStarted >= flashDuration) {
                flashText = null
                invalidate()
                return
            }
            invalidate()
            postDelayed(this, TICK_MS)
        }
    }

    /** Tampilkan kilasan teks hasil input di bawah papan (mis. "✓ halo"). */
    fun showFlash(text: String) {
        if (text.isBlank()) return
        flashText = text
        flashDuration = FLASH_MS
        flashStarted = SystemClock.uptimeMillis()
        removeCallbacks(flashTick)
        post(flashTick)
    }

    // ------------------------------------------------------------------ menggambar

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGuideLines(canvas)
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
        drawPending(canvas)
        drawFlash(canvas)
    }

    private fun drawGuideLines(canvas: Canvas) {
        if (!showLines) return
        val left = if (edgeToEdge) 0f else insetH
        val right = if (edgeToEdge) width.toFloat() else width - insetH
        var y = lineGapPx
        while (y < height - 1f) {
            canvas.drawLine(left, y, right, y, linePaint)
            y += lineGapPx
        }
    }

    private fun drawPending(canvas: Canvas) {
        if (pendingFraction <= 0f) return
        val barHeight = 3f * density
        val top = height - barHeight
        strokePaint.style = Paint.Style.FILL
        strokePaint.color = withAlpha(lineColor, 60)
        canvas.drawRect(0f, top, width.toFloat(), height.toFloat(), strokePaint)
        strokePaint.color = withAlpha(accentColor, 220)
        canvas.drawRect(0f, top, width * pendingFraction, height.toFloat(), strokePaint)
        strokePaint.style = Paint.Style.STROKE
    }

    private fun drawFlash(canvas: Canvas) {
        val text = flashText ?: return
        val elapsed = SystemClock.uptimeMillis() - flashStarted
        val fadeStart = (flashDuration * 0.6f).toLong()
        val alpha = if (elapsed <= fadeStart) {
            1f
        } else {
            (1f - (elapsed - fadeStart).toFloat() / (flashDuration - fadeStart).coerceAtLeast(1L)).coerceIn(0f, 1f)
        }

        val textSize = 13f * density
        flashPaint.textSize = textSize
        flashPaint.typeface = Typeface.DEFAULT_BOLD
        val padding = 9f * density
        val boxHeight = textSize * 1.9f
        val left = 6f * density
        val bottomOffset = if (pendingFraction > 0f) 8f * density else 5f * density
        val top = height - boxHeight - bottomOffset
        flashBox.set(left, top, left + flashPaint.measureText(text) + padding * 2f, top + boxHeight)

        flashPaint.style = Paint.Style.FILL
        flashPaint.color = withAlpha(accentColor, (240 * alpha).toInt())
        val radius = boxHeight / 2f
        canvas.drawRoundRect(flashBox, radius, radius, flashPaint)

        flashPaint.color = withAlpha(0xFFFFFFFF.toInt(), (255 * alpha).toInt())
        val baseline = flashBox.centerY() - (flashPaint.descent() + flashPaint.ascent()) / 2f
        canvas.drawText(text, flashBox.left + padding, baseline, flashPaint)
    }

    private val insetH: Float get() = if (edgeToEdge) 3f * density else 12f * density

    private fun applyBackground() {
        backgroundDrawable.shape = GradientDrawable.RECTANGLE
        val radius = cornerRadiusPx
        val left = if (edgeToEdge) 0f else radius
        backgroundDrawable.cornerRadii = floatArrayOf(left, left, radius, radius, radius, radius, left, left)
        backgroundDrawable.setColor(withAlpha(boardColor, boardAlpha))
        backgroundDrawable.setStroke(
            density.toInt().coerceAtLeast(1),
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
        if (event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY)) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onPenButtonPrimary?.invoke()
            return true
        }
        if (event.isButtonPressed(MotionEvent.BUTTON_STYLUS_SECONDARY)) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onPenButtonSecondary?.invoke()
            return true
        }

        val toolType = event.getToolType(0)
        val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS
        val isEraser = toolType == MotionEvent.TOOL_TYPE_ERASER
        if (!isStylus && !isEraser && !allowFinger) return false

        if (isEraser) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> removeStrokesNear(event.x, event.y)
            }
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val factor = if (isStylus && pressureEnabled) BoardMath.pressureScale(event.pressure) else 1f
                startStroke(event.x, event.y, factor)
            }
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

    private fun startStroke(x: Float, y: Float, widthFactor: Float) {
        parent?.requestDisallowInterceptTouchEvent(true)
        if (haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        // Pena mulai menulis: papan belum siap dijadikan teks.
        onStrokeStarted?.invoke()
        val stroke = Stroke(color = penColor, widthPx = penWidthPx * widthFactor)
        stroke.points.add(InkPoint(x, y))
        current = stroke
        currentPath.reset()
        currentPath.moveTo(x, y)
        lastInk = InkPoint(x, y)
        redoStack.clear()
        invalidate()
    }

    private fun extendStroke(x: Float, y: Float) {
        val stroke = current ?: return
        val last = stroke.last() ?: return
        if (hypot(x - last.x, y - last.y) < 1.2f) return
        stroke.points.add(InkPoint(x, y))
        // Lengkung kuadratik lewat titik tengah: tulisan halus, tidak patah-patah.
        currentPath.quadTo(last.x, last.y, (last.x + x) / 2f, (last.y + y) / 2f)
        lastInk = InkPoint(x, y)
        invalidate()
    }

    private fun finishStroke() {
        val stroke = current ?: return
        current = null
        lastInk?.let { currentPath.lineTo(it.x, it.y) }
        strokes.add(stroke)
        strokePaths.add(pathOf(currentPath))
        currentPath.reset()
        lastInk = null
        invalidate()
        onStrokeCommitted?.invoke()
        onContentChanged?.invoke()
    }

    private fun notifyChanged() {
        invalidate()
        onContentChanged?.invoke()
    }

    private fun pathOf(stroke: Stroke): Path = Path().also { path ->
        val points = stroke.points
        if (points.isEmpty()) return@also
        path.moveTo(points[0].x, points[0].y)
        var previous = points[0]
        for (index in 1 until points.size) {
            val point = points[index]
            // Sama seperti saat menulis: lengkung kuadratik lewat titik tengah.
            path.quadTo(previous.x, previous.y, (previous.x + point.x) / 2f, (previous.y + point.y) / 2f)
            previous = point
        }
        path.lineTo(previous.x, previous.y)
    }

    private fun pathOf(source: Path): Path = Path(source)

    companion object {
        const val DEFAULT_LINE_GAP_DP = 44f

        /** Selang gambar indikator (ms). */
        private const val TICK_MS = 40L

        /** Lama kilasan teks hasil input (ms). */
        private const val FLASH_MS = 1600L

        /** Warna dengan alfa diubah (0-255). */
        fun withAlpha(color: Int, alpha: Int): Int =
            (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }
}
