package dev.retrobit.penboard.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatTextView
import dev.retrobit.penboard.R
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.util.KeyboardChars

/**
 * Papan karakter di dalam keyboard: angka, huruf, tanda baca, dan simbol.
 *
 * Bukan keyboard QWERTY. Satu tombol [KeyAction.CHAR_PAD] membuka papan ini di atas
 * panel; ketukan memasukkan karakter itu ke kolom isian, lalu papan tetap terbuka
 * supaya angka atau kata pendek bisa diketik berurutan. Tutup mengembalikan area tulis.
 */
class CharPad(context: Context) : LinearLayout(context) {

    var onInsert: ((String) -> Unit)? = null
    var onAction: ((KeyAction) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private var config = PenboardConfig()
    private var groupIndex = 0
    private var columns = 6
    private var cellPx = 0

    private val hint = AppCompatTextView(context)
    private val close = AppCompatTextView(context)
    private val tabs = LinearLayout(context)
    private val grid = LinearLayout(context)
    private val footer = LinearLayout(context)

    init {
        orientation = VERTICAL
        isClickable = true
        isFocusable = false
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        val pad = Ui.dp(context, 6f)
        setPadding(pad, pad, pad, pad)

        hint.text = context.getString(R.string.char_pad_hint)
        hint.textSize = 11f
        hint.typeface = Typeface.MONOSPACE
        hint.alpha = 0.75f

        close.text = context.getString(R.string.char_pad_close)
        close.textSize = 13f
        close.typeface = Typeface.MONOSPACE
        close.gravity = Gravity.CENTER
        close.isFocusable = false
        close.contentDescription = context.getString(R.string.char_pad_close)
        close.setPadding(Ui.dp(context, 10f), Ui.dp(context, 6f), Ui.dp(context, 10f), Ui.dp(context, 6f))
        close.setOnClickListener { onClose?.invoke() }

        tabs.orientation = HORIZONTAL
        tabs.gravity = Gravity.CENTER_VERTICAL
        grid.orientation = VERTICAL
        footer.orientation = HORIZONTAL
        footer.gravity = Gravity.CENTER_VERTICAL

        val tabScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            isFocusable = false
            addView(tabs)
        }
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(tabScroll, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(close)
        }
        val gridScroll = ScrollView(context).apply {
            isFocusable = false
            isFillViewport = true
            addView(grid)
        }
        addView(hint, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(
            header,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(context, 4f)
                bottomMargin = Ui.dp(context, 4f)
            }
        )
        addView(gridScroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(
            footer,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(context, 4f)
            }
        )
    }

    /** Sentuhan di luar tombol tidak boleh jatuh ke papan tulis di bawahnya. */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return true
    }

    fun applyStyle(config: PenboardConfig, widthPx: Int) {
        this.config = config
        val inner = (widthPx - paddingLeft - paddingRight).coerceAtLeast(Ui.dp(context, 120f))
        columns = (inner / Ui.dp(context, 36f)).coerceIn(4, 8)
        cellPx = (inner / columns).coerceIn(Ui.dp(context, 32f), Ui.dp(context, 52f))
        background = GradientDrawable().apply {
            cornerRadius = config.cornerRadiusDp * resources.displayMetrics.density
            setColor(BoardView.withAlpha(config.panelColor, 248))
            setStroke(
                Ui.dp(context, 1f).coerceAtLeast(1),
                BoardView.withAlpha(config.accentColor, 190)
            )
        }
        hint.setTextColor(config.buttonTextColor)
        close.setTextColor(config.buttonTextColor)
        close.background = chipBackground()
        rebuildTabs()
        rebuildGrid()
        rebuildFooter()
    }

    private fun rebuildTabs() {
        tabs.removeAllViews()
        KeyboardChars.groups.forEachIndexed { index, group ->
            val selected = index == groupIndex
            tabs.addView(
                AppCompatTextView(context).apply {
                    text = group.short
                    textSize = 13f
                    typeface = Typeface.MONOSPACE
                    gravity = Gravity.CENTER
                    isFocusable = false
                    contentDescription = group.title
                    setTextColor(config.buttonTextColor)
                    alpha = if (selected) 1f else 0.45f
                    setPadding(Ui.dp(context, 8f), Ui.dp(context, 6f), Ui.dp(context, 8f), Ui.dp(context, 6f))
                    background = GradientDrawable().apply {
                        cornerRadius = Ui.dp(context, 6f).toFloat()
                        setColor(
                            if (selected) BoardView.withAlpha(config.accentColor, 50) else 0x00000000
                        )
                    }
                    setOnClickListener {
                        groupIndex = index
                        rebuildTabs()
                        rebuildGrid()
                    }
                },
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = Ui.dp(context, 2f)
                }
            )
        }
    }

    private fun rebuildGrid() {
        grid.removeAllViews()
        val chars = KeyboardChars.groups[groupIndex].chars
        val gap = Ui.dp(context, 3f)
        var index = 0
        while (index < chars.length) {
            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            for (column in 0 until columns) {
                if (index >= chars.length) break
                val ch = chars[index].toString()
                index++
                row.addView(
                    characterChip(ch),
                    LayoutParams(cellPx, cellPx).apply {
                        marginEnd = if (column == columns - 1) 0 else gap
                        bottomMargin = gap
                    }
                )
            }
            grid.addView(row, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun characterChip(text: String): AppCompatTextView =
        AppCompatTextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.MONOSPACE
            isFocusable = false
            contentDescription = text
            setTextColor(config.buttonTextColor)
            background = chipBackground()
            setOnClickListener { onInsert?.invoke(text) }
        }

    private fun rebuildFooter() {
        footer.removeAllViews()
        val height = Ui.dp(context, 40f)
        val actions = listOf(
            Triple("\u2190", KeyAction.CURSOR_LEFT, true),
            Triple("\u2192", KeyAction.CURSOR_RIGHT, true),
            Triple("\u232B", KeyAction.BACKSPACE, true),
            Triple("\u2423", KeyAction.SPACE, false),
            Triple("\u21B5", KeyAction.ENTER, false)
        )
        actions.forEach { (label, action, repeat) ->
            val button = KeyButton(context).apply {
                text = label
                contentDescription = action.label
                isFocusable = false
                repeatOnHold = repeat
                onTap = { onAction?.invoke(action) }
                style(
                    radiusPx = config.cornerRadiusDp * resources.displayMetrics.density * 0.8f,
                    fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
                    borderColor = BoardView.withAlpha(config.accentColor, 140),
                    labelColor = config.buttonTextColor,
                    textSizePx = height * 0.42f,
                    haptics = config.haptics
                )
            }
            footer.addView(
                button,
                LayoutParams(0, height, 1f).apply { marginEnd = Ui.dp(context, 3f) }
            )
        }
    }

    private fun chipBackground(): GradientDrawable = GradientDrawable().apply {
        cornerRadius = Ui.dp(context, 6f).toFloat()
        setColor(BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100))
        setStroke(Ui.dp(context, 1f).coerceAtLeast(1), BoardView.withAlpha(config.accentColor, 120))
    }
}
