package dev.retrobit.penboard.board

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import dev.retrobit.penboard.data.Colors
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.Presets
import dev.retrobit.penboard.data.ToolMode
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Papan tulis stylus.
 *
 * - Goresan disimpan dalam koordinat konten, jadi "geser papan otomatis" hanya
 *   memindahkan gambar tanpa merusak tulisan.
 * - Mendukung Huawei M-Pencil: tekanan stylus memengaruhi tebal garis, tombol
 *   samping pena = urungkan, ujung penghapus pena = mode hapus.
 * - Bisa menolak sentuhan jari (anti telapak tangan) sesuai pengaturan profil.
 */
class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Skala perangkat, dipakai untuk mengubah satuan dp -> px. */
    private val density = resources.displayMetrics.density

    // ---------------------------------------------------------------- gaya/paint
    private val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        pathEffect = DashPathEffect(floatArrayOf(6f * density, 8f * density), 0f)
    }
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(120, 255, 255, 255)
        strokeWidth = 3f * density
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // ---------------------------------------------------------------- pengaturan
    var config: PenboardConfig = Presets.defaultConfig()

    var tool: ToolMode = ToolMode.WRITE
        set(value) {
            field = value
            inputPaint = null
            invalidate()
        }

    /** Warna pena sesaat (dipakai tombol "ganti warna pena"). null = warna dari profil. */
    var penColorOverride: Int? = null

    /** Radius sudut latar papan (px). 0 = tanpa pembulatan. */
    var cornerRadiusPx: Float = 0f
        set(value) {
            field = value
            applyBackground()
        }

    var onStrokeCommitted: (() -> Unit)? = null
    var onContentChanged: (() -> Unit)? = null
    var onEraseHappened: (() -> Unit)? = null
    var onFeedback: (() -> Unit)? = null
    var onToolChangedByPen: ((ToolMode) -> Unit)? = null

    // ---------------------------------------------------------------- konten
    private val strokes = ArrayList<Stroke>()
    private val undoStack = ArrayDeque<Action>()
    private val redoStack = ArrayDeque<Action>()

    private var contentShift = 0f
    private var activeStroke: Stroke? = null
    private var activeEraseIsPen = false
    private var erasePoints: MutableList<Float>? = null

    private var gapPx = 44f * density
    private var visibleLines = 6
    private var insetV = 0f
    private var insetH = 0f

    /** Penanda gesture sedang berlangsung. */
    private var gestureActive = false
    private var gestureRemoved = ArrayList<Pair<Int, Stroke>>()

    private var inputPaint: Paint? = null

    init {
        isFocusable = false
        isFocusableInTouchMode = false
        setWillNotDraw(false)
        applyBackground()
    }

    // ---------------------------------------------------------------- api publik

    fun applyConfig(newConfig: PenboardConfig) {
        config = newConfig
        eraserPaint.color = Colors.withOpacity(newConfig.strokeColor, 45)
        applyBackground()
        requestLayout()
        invalidate()
    }

    val hasContent: Boolean get() = strokes.isNotEmpty()

    val hasUndo: Boolean get() = undoStack.isNotEmpty()

    val hasRedo: Boolean get() = redoStack.isNotEmpty()

    val strokeCount: Int get() = strokes.size

    val shiftLines: Int get() = if (gapPx <= 0f) 0 else (contentShift / gapPx).roundToInt()

    fun clearBoard() {
        strokes.clear()
        undoStack.clear()
        redoStack.clear()
        contentShift = 0f
        activeStroke = null
        erasePoints = null
        invalidate()
        onContentChanged?.invoke()
    }

    fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        when (action) {
            is Action.AddStroke -> strokes.remove(action.stroke)
            is Action.Shift -> contentShift = max(0f, contentShift - action.amount)
            is Action.Erase -> {
                // kembalikan goresan yang terhapus ke posisi semula
                action.removed.sortedBy { it.first }.forEach { (index, stroke) ->
                    val safeIndex = index.coerceIn(0, strokes.size)
                    strokes.add(safeIndex, stroke)
                }
            }
        }
        redoStack.addLast(action)
        invalidate()
        onContentChanged?.invoke()
    }

    fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        when (action) {
            is Action.AddStroke -> strokes.add(action.stroke)
            is Action.Shift -> contentShift += action.amount
            is Action.Erase -> {
                val toRemove = action.removed.mapNotNull { it.second }
                strokes.removeAll(toRemove.toSet())
            }
        }
        undoStack.addLast(action)
        invalidate()
        onContentChanged?.invoke()
    }

    /** Hapus goresan terakhir (tombol "hapus goresan"). */
    fun removeLastStroke() {
        val last = strokes.lastOrNull() ?: return
        strokes.remove(last)
        undoStack.addLast(Action.AddStroke(last))
        redoStack.clear()
        invalidate()
        onContentChanged?.invoke()
    }

    // ---------------------------------------------------------------- latar

    private fun applyBackground() {
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        val radius = cornerRadiusPx
        bg.cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
        bg.setColor(Colors.withOpacity(config.boardBgColor, config.boardBgOpacity))
        background = bg
    }

    // ---------------------------------------------------------------- ukuran

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        insetV = 4f * density
        insetH = 14f * density
        val available = h - insetV * 2f
        if (available <= 0f) return
        val desired = config.lineGapDp * density
        visibleLines = max(3, (available / desired).roundToInt())
        gapPx = available / visibleLines
    }

    // ---------------------------------------------------------------- menggambar

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGuides(canvas)

        canvas.save()
        canvas.translate(0f, insetV - contentShift)

        for (stroke in strokes) {
            drawStroke(canvas, stroke)
        }
        activeStroke?.let { drawStroke(canvas, it) }

        val eraser = erasePoints
        if (eraser != null && eraser.size >= 2) {
            var i = 0
            while (i < eraser.size - 3) {
                canvas.drawLine(eraser[i], eraser[i + 1], eraser[i + 2], eraser[i + 3], eraserPaint)
                i += 2
            }
        }
        canvas.restore()
    }

    private fun drawGuides(canvas: Canvas) {
        if (!config.gridEnabled || config.gridOpacity <= 0) return
        linePaint.color = Colors.withOpacity(config.gridColor, config.gridOpacity)
        baselinePaint.color = Colors.withOpacity(config.gridColor, (config.gridOpacity * 0.7f).toInt())
        val left = insetH
        val right = width - insetH
        val top = insetV
        for (i in 0..visibleLines) {
            val y = top + i * gapPx
            canvas.drawLine(left, y, right, y, linePaint)
            if (config.showBaseline && i < visibleLines) {
                canvas.drawLine(left, y + gapPx * 0.78f, right, y + gapPx * 0.78f, baselinePaint)
            }
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        val paint = inputPaint ?: Paint(penPaint).also { inputPaint = it }
        paint.color = stroke.color
        paint.strokeWidth = stroke.widthPx
        if (stroke.size == 1) {
            dotPaint.color = stroke.color
            canvas.drawCircle(stroke.xAt(0), stroke.yAt(0), stroke.widthPx / 2f, dotPaint)
        } else {
            canvas.drawPath(stroke.path(), paint)
        }
    }

    // ---------------------------------------------------------------- sentuhan

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(event.actionIndex)
        val buttonState = event.buttonState
        val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER
        val isEraserTool = toolType == MotionEvent.TOOL_TYPE_ERASER
        val primaryButton = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
        val secondaryButton = (buttonState and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (secondaryButton) {
                undo()
                return true
            }
            if (toolType == MotionEvent.TOOL_TYPE_FINGER && !config.allowFinger) {
                // telapak tangan / jari diabaikan
                return false
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount > 1) return false
                gestureActive = true
                activeEraseIsPen = isEraserTool || primaryButton
                val useEraser = activeEraseIsPen || tool == ToolMode.ERASE
                if (useEraser) {
                    erasePoints = ArrayList()
                    gestureRemoved = ArrayList()
                    onFeedback?.invoke()
                } else {
                    val width = config.strokeWidthDp * density * pressureScale(event)
                    val color = penColorOverride ?: config.strokeColor
                    activeStroke = Stroke(color, width).also { it.add(toContentX(event.x), toContentY(event.y)) }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!gestureActive) return false
                val useEraser = activeEraseIsPen || tool == ToolMode.ERASE
                if (useEraser) {
                    eraseAt(toContentX(event.x), toContentY(event.y))
                } else {
                    activeStroke?.add(toContentX(event.x), toContentY(event.y))
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!gestureActive) return false
                gestureActive = false
                val useEraser = activeEraseIsPen || tool == ToolMode.ERASE
                if (useEraser) {
                    if (gestureRemoved.isNotEmpty()) {
                        undoStack.addLast(Action.Erase(gestureRemoved))
                        redoStack.clear()
                        onEraseHappened?.invoke()
                        onContentChanged?.invoke()
                    }
                    erasePoints = null
                    gestureRemoved = ArrayList()
                } else {
                    val stroke = activeStroke
                    activeStroke = null
                    if (stroke != null && !stroke.isEmpty) {
                        strokes.add(stroke)
                        undoStack.addLast(Action.AddStroke(stroke))
                        redoStack.clear()
                        trimIfNeeded()
                        onStrokeCommitted?.invoke()
                        onContentChanged?.invoke()
                        autoShiftIfNeeded(stroke)
                    }
                }
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

    private fun pressureScale(event: MotionEvent): Float {
        val pressure = event.pressure
        return when {
            pressure <= 0f -> 1f
            pressure < 0.25f -> 0.75f
            pressure > 1.1f -> 1.5f
            else -> 0.75f + (pressure - 0.25f) * 0.85f
        }
    }

    private fun toContentX(x: Float): Float = x

    private fun toContentY(y: Float): Float = y - insetV + contentShift

    private fun eraseAt(x: Float, y: Float) {
        val radius = config.eraserWidthDp * density / 2f
        erasePoints?.let { points ->
            points.add(x)
            points.add(y)
        }
        var index = 0
        while (index < strokes.size) {
            val stroke = strokes[index]
            if (stroke.intersectsCircle(x, y, radius)) {
                gestureRemoved.add(index to stroke)
                strokes.removeAt(index)
            } else {
                index++
            }
        }
    }

    /** Geser konten naik supaya selalu ada baris kosong di bawah tulisan. */
    private fun autoShiftIfNeeded(stroke: Stroke) {
        if (!config.autoShift) return
        val writtenLine = stroke.lineIndex(gapPx).coerceAtMost(visibleLines - 1)
        val lastFree = visibleLines - 2
        if (writtenLine > lastFree) {
            val amount = (writtenLine - lastFree) * gapPx
            contentShift += amount
            undoStack.addLast(Action.Shift(amount))
            redoStack.clear()
        }
    }

    private fun trimIfNeeded() {
        while (strokes.size > MAX_STROKES) {
            strokes.removeAt(0)
        }
    }

    /** Perkiraan tinggi yang dibutuhkan untuk beberapa baris tulis. */
    fun suggestedHeightPx(): Int = (config.lineGapDp * density * 7f).roundToInt() + (insetV * 2).roundToInt()

    /** Geometri untuk pengujian / tampilan informasi. */
    fun lineInfo(): String = "$visibleLines baris x ${gapPx.roundToInt()}px"

    fun guideRect(): RectF = RectF(insetH, insetV, width - insetH, height - insetV)

    fun linePath(index: Int): Path {
        val path = Path()
        val y = insetV + index * gapPx - contentShift
        path.moveTo(insetH, y)
        path.lineTo(width - insetH, y)
        return path
    }

    fun lineIndexOf(y: Float): Int = floor((y - insetV + contentShift) / gapPx).toInt()

    private sealed interface Action {
        data class AddStroke(val stroke: Stroke) : Action
        data class Shift(val amount: Float) : Action
        data class Erase(val removed: List<Pair<Int, Stroke>>) : Action
    }

    private companion object {
        const val MAX_STROKES = 400
    }
}
