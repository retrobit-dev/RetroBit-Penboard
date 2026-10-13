package dev.retrobit.penboard.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.util.GridMath

/**
 * Susunan Penboard: **papan tulis di kiri sampai tombol, grid 3x3 di kanan**.
 *
 * Dipakai bersama oleh keyboard ([dev.retrobit.penboard.ime.PenboardInputService])
 * dan papan melayang ([dev.retrobit.penboard.overlay.PenboardOverlayService]),
 * dibuat sepenuhnya dari kode supaya tidak ada berkas tata letak yang perlu disamakan.
 */
class BoardPanel(private val context: Context, private val showHandle: Boolean) {

    interface Listener {
        fun onKey(key: KeyDef, action: KeyAction)
        fun onStrokeCommitted()
        fun onPenButton(primary: Boolean)
        fun onContentChanged()
    }

    var listener: Listener? = null

    private val density = context.resources.displayMetrics.density
    private val buttons = mutableListOf<KeyButton>()
    private val gridRows = mutableListOf<LinearLayout>()
    private val gridContainer = LinearLayout(context)
    private val panelRow = LinearLayout(context)
    private val panel = LinearLayout(context)
    private val panelBackground = GradientDrawable()

    val root = FrameLayout(context)
    val board = BoardView(context)
    val handle: View?

    init {
        board.onStrokeCommitted = { listener?.onStrokeCommitted() }
        board.onContentChanged = { listener?.onContentChanged() }
        board.onPenButtonPrimary = { listener?.onPenButton(true) }
        board.onPenButtonSecondary = { listener?.onPenButton(false) }
        board.edgeToEdge = true

        gridContainer.orientation = LinearLayout.VERTICAL
        gridContainer.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        for (rowIndex in 0 until GridMath.ROWS) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            for (column in 0 until GridMath.COLUMNS) {
                val button = KeyButton(context)
                buttons.add(button)
                row.addView(button)
            }
            gridRows.add(row)
            gridContainer.addView(row)
        }

        panelRow.orientation = LinearLayout.HORIZONTAL
        panelRow.gravity = Gravity.CENTER_VERTICAL
        panelRow.addView(board, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        panelRow.addView(
            gridContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        handle = if (showHandle) buildHandle() else null
        handle?.let { panel.addView(it) }
        panel.addView(
            panelRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        panel.orientation = LinearLayout.VERTICAL

        root.addView(
            panel,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    /** Terapkan seluruh pengaturan ke papan dan tombol. */
    fun applyConfig(config: PenboardConfig) {
        val spacing = (GridMath.SPACING_DP * density).toInt()
        val boardHeightPx = (config.boardHeightDp * density).toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val cell = GridMath.cellSizePx(boardHeightPx, screenWidth, spacing, (44 * density).toInt())
        val panelPadding = (2 * density).toInt()

        panel.setPadding(panelPadding, panelPadding, panelPadding, panelPadding)
        panelBackground.shape = GradientDrawable.RECTANGLE
        panelBackground.cornerRadius = config.cornerRadiusDp * density
        panelBackground.setColor(BoardView.withAlpha(config.panelColor, config.panelOpacity * 255 / 100))
        panel.background = panelBackground

        board.layoutParams = LinearLayout.LayoutParams(0, boardHeightPx, 1f)
        board.lineGapPx = config.lineGapDp * density
        board.penColor = config.currentPenColor
        board.penWidthPx = config.penWidthDp * density
        board.allowFinger = config.allowFinger
        board.showLines = config.showLines
        board.haptics = config.haptics
        board.edgeToEdge = true
        board.cornerRadiusPx = config.cornerRadiusDp * density
        board.boardColor = config.boardColor
        board.boardAlpha = config.boardOpacity * 255 / 100
        board.lineColor = config.lineColor

        val defaults = PenboardConfig.defaultGridKeys()
        gridRows.forEachIndexed { rowIndex, row ->
            row.visibility = if (config.gridButtonCount > rowIndex * GridMath.COLUMNS) View.VISIBLE else View.GONE
        }
        for (index in 0 until PenboardConfig.GRID_SLOTS) {
            val button = buttons[index]
            val key = config.gridKeys.getOrNull(index) ?: defaults[index]
            button.visibility = if (index < config.gridButtonCount) View.VISIBLE else View.INVISIBLE
            button.text = config.labelFor(key)
            button.repeatOnHold = key.repeatOnHold
            button.onTap = { listener?.onKey(key, key.action) }
            button.layoutParams = LinearLayout.LayoutParams(cell, cell).apply {
                marginStart = if (index % GridMath.COLUMNS == 0) 0 else spacing
                topMargin = if (index >= GridMath.COLUMNS) spacing else 0
            }
            button.style(
                radiusPx = config.cornerRadiusDp * density * 0.85f,
                fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
                borderColor = BoardView.withAlpha(config.accentColor, 140),
                labelColor = config.buttonTextColor,
                textSizePx = cell * 0.20f,
                haptics = config.haptics
            )
        }
        gridContainer.layoutParams = LinearLayout.LayoutParams(
            cell * GridMath.COLUMNS + spacing * (GridMath.COLUMNS - 1),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = spacing }
    }

    private fun buildHandle(): View {
        val container = FrameLayout(context)
        container.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (18 * density).toInt()
        )
        val bar = View(context)
        bar.layoutParams = FrameLayout.LayoutParams((46 * density).toInt(), (5 * density).toInt(), Gravity.CENTER)
        bar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 3f * density
            setColor(0x66FFFFFF)
        }
        container.addView(bar)
        return container
    }
}
