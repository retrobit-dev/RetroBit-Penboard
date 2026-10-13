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
import dev.retrobit.penboard.data.PenButtonAction
import dev.retrobit.penboard.data.Presets
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.util.BoardMath
import kotlin.math.max

/**
 * Papan tulis stylus.
 *
 * - Goresan disimpan dalam koordinat konten, jadi "geser papan otomatis" hanya
 *   memindahkan gambar tanpa merusak tulisan.
 * - Mendukung Huawei M-Pencil: tekanan stylus memengaruhi tebal garis, tombol
 *   samping pena = urungkan, ujung penghapus pena = mode hapus.
 * - Bisa menolak sentuhan jari (anti telapak tangan) sesuai pengaturan profil.
 * - Isi papan bisa disimpan ([snapshot]) dan dimuat kembali ([restore]) sehingga
 *   tulisan tetap ada walau keyboard ditutup atau profil berganti.
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

    /**
     * Warna pena. Bila null, warna diambil dari profil; tombol pena berwarna
     * mengisi nilai ini (atau mengosongkannya kembali bila ditekan lagi).
     */
    var penColorOverride: Int? = null

    private var currentTool: ToolMode = ToolMode.WRITE

    var tool: ToolMode
        get() = currentTool
        set(value) {
            if (currentTool == value) return
            currentTool = value
            inputPaint = null
            if (value == ToolMode.ERASE) onToolChangedByPen?.invoke(value)
            invalidate()
        }

    /** Radius sudut latar papan (px). 0 = tanpa pembulatan. */
    var cornerRadiusPx: Float = 0f
        set(value) {
            field = value
            applyBackground()
        }

    var onStrokeCommitted: (() -> Unit)? = null
    var onContentChanged: (() -> Unit)? = null
    var onEraseHappened: (() -> Unit)? = null
    /** Dipanggil saat alat berubah karena pena (mis. ujung penghapus), agar tombol ikut menyorot. */
    var onToolChangedByPen: ((ToolMode) -> Unit)? = null

    /** Dipanggil saat tombol samping pena ditekan (aksinya diatur per profil). */
    var onPenButtonAction: ((PenButtonAction) -> Unit)? = null

    // ---------------------------------------------------------------- konten
    private val strokes = ArrayList<Stroke>()
    private val undoStack = ArrayDeque<BoardAction>()
    private val redoStack = ArrayDeque<BoardAction>()

    /** Geser konten otomatis (koordinat konten). */
    private var contentShift = 0f

    /** Geser tampilan manual dari baris terbaru (koordinat layar). */
    private var viewOffset = 0f

    private var activeStroke: Stroke? = null
    private var activeEraseIsPen = false
    private var erasePoints: MutableList<Float>? = null

    private var gapPx = 44f * density
    private var visibleLines = 6
    private var insetV = 0f
    private var insetH = 0f

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
        recomputeGeometry()
        invalidate()
    }

    val hasContent: Boolean get() = strokes.isNotEmpty()

    val hasUndo: Boolean get() = undoStack.isNotEmpty()

    val hasRedo: Boolean get() = redoStack.isNotEmpty()

    val strokeCount: Int get() = strokes.size

    /** Geser konten otomatis dalam satuan baris (bisa negatif). */
    val shiftLines: Int get() = if (gapPx <= 0f) 0 else (contentShift / gapPx).toInt()

    /** Baris yang sedang dilihat saat pengguna menggeser tampilan. */
    val viewLine: Int get() = (viewOffset / gapPx).toInt()

    val isViewingHistory: Boolean get() = viewOffset > 1f

    /** Bisa digeser bila ada baris lama yang tersembunyi. */
    val canScroll: Boolean get() = contentShift > 1f

    /** Warna pena yang sedang dipakai. */
    fun activePenColor(): Int = penColorOverride ?: config.strokeColor

    fun clearBoard() {
        strokes.clear()
        undoStack.clear()
        redoStack.clear()
        contentShift = 0f
        viewOffset = 0f
        activeStroke = null
        erasePoints = null
        invalidate()
        onContentChanged?.invoke()
    }

    fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        when (action) {
            is BoardAction.AddStroke -> strokes.remove(action.stroke)
            is BoardAction.Shift -> contentShift = max(0f, contentShift - action.amount)
            is BoardAction.Erase -> {
                action.removed.sortedBy { it.first }.forEach { (index, stroke) ->
                    strokes.add(index.coerceIn(0, strokes.size), stroke)
                }
            }
        }
        redoStack.addLast(action)
        viewOffset = BoardMath.clampViewOffset(viewOffset, contentShift)
        recomputeShift()
        invalidate()
        onContentChanged?.invoke()
    }

    fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        when (action) {
            is BoardAction.AddStroke -> strokes.add(action.stroke)
            is BoardAction.Shift -> contentShift += action.amount
            is BoardAction.Erase -> {
                val toRemove = action.removed.map { it.second }.toSet()
                strokes.removeAll(toRemove)
            }
        }
        undoStack.addLast(action)
        recomputeShift()
        invalidate()
        onContentChanged?.invoke()
    }

    /** Hapus goresan terakhir (tombol "hapus goresan"). */
    fun removeLastStroke() {
        val last = strokes.lastOrNull() ?: return
        strokes.remove(last)
        undoStack.addLast(BoardAction.AddStroke(last))
        redoStack.clear()
        invalidate()
        onContentChanged?.invoke()
    }

    /** Salinan daftar goresan (untuk pengenalan tulisan). */
    fun contentStrokes(): List<Stroke> = ArrayList(strokes)

    /** Jarak baris saat ini dalam piksel (dipakai pengenalan tulisan). */
    val lineGapPx: Float get() = gapPx

    /** Hapus sekumpulan goresan sebagai satu langkah urungkan (dipakai setelah pengenalan). */
    fun removeStrokes(targets: List<Stroke>) {
        if (targets.isEmpty()) return
        val wanted = targets.toSet()
        val removed = ArrayList<Pair<Int, Stroke>>()
        val kept = ArrayList<Stroke>(strokes.size)
        strokes.forEachIndexed { index, stroke ->
            if (stroke in wanted) removed.add(index to stroke) else kept.add(stroke)
        }
        if (removed.isEmpty()) return
        strokes.clear()
        strokes.addAll(kept)
        undoStack.addLast(BoardAction.Erase(removed))
        redoStack.clear()
        recomputeShift()
        invalidate()
        onContentChanged?.invoke()
    }

    /** Geser tampilan ke tulisan yang lebih lama (delta > 0) atau kembali ke baris terbaru. */
    fun scrollByLines(lines: Int) {
        if (lines == 0 || gapPx <= 0f) return
        viewOffset = BoardMath.clampViewOffset(viewOffset + lines * gapPx, contentShift)
        invalidate()
    }

    /** Lihat tulisan paling awal. */
    fun scrollToOldest() {
        viewOffset = BoardMath.clampViewOffset(contentShift, contentShift)
        invalidate()
    }

    /** Kembali ke baris kosong terbaru untuk menulis lanjutan. */
    fun scrollToNewest() {
        viewOffset = 0f
        invalidate()
    }

    // ---------------------------------------------------------------- simpan/muat

    /** Ambil isi papan untuk disimpan. Null bila papan kosong. */
    fun snapshot(): BoardSnapshot? {
        if (strokes.isEmpty()) return null
        return BoardSnapshot(strokes.map { it }, contentShift)
    }

    /** Muat isi papan. Data lama pada instance ini diganti seluruhnya. */
    fun restore(snapshot: BoardSnapshot) {
        strokes.clear()
        undoStack.clear()
        redoStack.clear()
        strokes.addAll(snapshot.strokes)
        contentShift = snapshot.shiftPx
        viewOffset = 0f
        activeStroke = null
        erasePoints = null
        recomputeShift()
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
        recomputeGeometry()
    }

    /**
     * Hitung ulang jarak baris. Dengan "geser otomatis" aktif, jarak baris mengikuti
     * tinggi papan supaya selalu pas; bila tidak aktif, jarak baris tetap sesuai
     * pengaturan pengguna dan papan naik sendiri saat baris bawah terpakai.
     */
    private fun recomputeGeometry() {
        val available = height - insetV * 2f
        if (available <= 0f) return
        val desiredGap = config.lineGapDp * density
        if (config.autoShift) {
            visibleLines = BoardMath.visibleLines(available, desiredGap)
            gapPx = BoardMath.gapPx(available, visibleLines)
        } else {
            visibleLines = BoardMath.visibleLines(available, desiredGap)
            gapPx = desiredGap
        }
        recomputeShift()
    }

    /**
     * Geser konten supaya selalu ada tempat menulis:
     * - "geser otomatis" aktif: satu baris kosong selalu disediakan di bawah;
     * - nonaktif: hanya digeser bila tulisan benar-benar melewati baris terbawah.
     */
    private fun recomputeShift() {
        val lastStroke = strokes.lastOrNull() ?: return
        val lastLine = lastStroke.lineIndex(gapPx)
        val requiredLines = if (config.autoShift) {
            BoardMath.autoShiftLines(lastLine, visibleLines)
        } else {
            (lastLine - (visibleLines - 1)).coerceAtLeast(0)
        }
        val desired = requiredLines * gapPx
        if (desired > contentShift) {
            val delta = desired - contentShift
            contentShift = desired
            undoStack.addLast(BoardAction.Shift(delta))
            redoStack.clear()
        }
    }

    // ---------------------------------------------------------------- menggambar

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGuides(canvas)

        val scroll = BoardMath.scrollY(contentShift, viewOffset)
        canvas.save()
        canvas.translate(0f, insetV - scroll)

        for (stroke in strokes) drawStroke(canvas, stroke)
        activeStroke?.let { drawStroke(canvas, it) }

        val eraser = erasePoints
        if (eraser != null && eraser.size >= 4) {
            var i = 0
            while (i <= eraser.size - 4) {
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
        for (i in 0..visibleLines) {
            val y = insetV + i * gapPx - viewOffset
            if (y < -gapPx || y > height + gapPx) continue
            canvas.drawLine(left, y, right, y, linePaint)
            if (config.showBaseline && i < visibleLines) {
                canvas.drawLine(left, y + gapPx * 0.78f, right, y + gapPx * 0.78f, baselinePaint)
            }
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        if (stroke.isEmpty) return
        if (stroke.bottom() < contentShift - viewOffset - gapPx * 2f) return
        if (stroke.top() > contentShift - viewOffset + height + gapPx * 2f) return
        val paint = inputPaint ?: Paint(penPaint).also { inputPaint = it }
        paint.color = stroke.color
        paint.strokeWidth = stroke.widthPx
        if (stroke.size == 1) {
            dotPaint.color = stroke.color
            canvas.drawCircle(stroke.xAt(0), stroke.yAt(0), stroke.widthPx / 2f, dotPaint)
        } else {
            canvas.drawPath(StrokePaths.of(stroke), paint)
        }
    }

    // ---------------------------------------------------------------- sentuhan

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(event.actionIndex)
        val buttonState = event.buttonState
        val isEraserTool = toolType == MotionEvent.TOOL_TYPE_ERASER
        val primaryButton = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
        val secondaryButton = (buttonState and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            // Tombol samping pena menjalankan aksi sesuai pengaturan profil.
            val penButton = when {
                secondaryButton -> config.penButtonSecondary
                primaryButton -> config.penButtonPrimary
                else -> null
            }
            if (penButton != null && penButton != PenButtonAction.NONE) {
                if (penButton == PenButtonAction.ERASE) {
                    activeEraseIsPen = true
                } else {
                    onPenButtonAction?.invoke(penButton)
                    return true
                }
            }
            if (toolType == MotionEvent.TOOL_TYPE_FINGER && !config.allowFinger) {
                return false
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount > 1) return false
                gestureActive = true
                if (isEraserTool) activeEraseIsPen = true
                val useEraser = activeEraseIsPen || currentTool == ToolMode.ERASE
                if (useEraser) {
                    erasePoints = ArrayList()
                    gestureRemoved = ArrayList()
                } else {
                    val width = config.strokeWidthDp * density * pressureScale(event)
                    activeStroke = Stroke(activePenColor(), width).also {
                        it.add(toContentX(event.x), toContentY(event.y))
                    }
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!gestureActive) return false
                val useEraser = activeEraseIsPen || currentTool == ToolMode.ERASE
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
                val useEraser = activeEraseIsPen || currentTool == ToolMode.ERASE
                if (useEraser) {
                    if (gestureRemoved.isNotEmpty()) {
                        undoStack.addLast(BoardAction.Erase(gestureRemoved))
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
                        undoStack.addLast(BoardAction.AddStroke(stroke))
                        redoStack.clear()
                        trimIfNeeded()
                        recomputeShift()
                        onStrokeCommitted?.invoke()
                        onContentChanged?.invoke()
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

    private fun toContentY(y: Float): Float =
        BoardMath.toContentY(y, contentShift, viewOffset, insetV)

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

    private fun trimIfNeeded() {
        while (strokes.size > MAX_STROKES) {
            strokes.removeAt(0)
        }
    }

    /** Tinggi yang disarankan untuk beberapa baris tulis. */
    fun suggestedHeightPx(): Int = (config.lineGapDp * density * 7f).toInt() + (insetV * 2).toInt()

    /** Informasi baris (dipakai dasbor). */
    fun lineInfo(): String = "$visibleLines baris x ${gapPx.toInt()}px"

    fun guideRect(): RectF = RectF(insetH, insetV, width - insetH, height - insetV)

    fun linePath(index: Int): Path {
        val path = Path()
        val y = insetV + index * gapPx - viewOffset
        path.moveTo(insetH, y)
        path.lineTo(width - insetH, y)
        return path
    }

    fun lineIndexOf(y: Float): Int = BoardMath.lineIndexOf(y - insetV + contentShift - viewOffset, gapPx)

    private sealed interface BoardAction {
        data class AddStroke(val stroke: Stroke) : BoardAction
        data class Shift(val amount: Float) : BoardAction
        data class Erase(val removed: List<Pair<Int, Stroke>>) : BoardAction
    }

    private companion object {
        const val MAX_STROKES = 400
    }
}
