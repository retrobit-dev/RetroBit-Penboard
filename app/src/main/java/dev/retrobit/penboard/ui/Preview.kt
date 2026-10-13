package dev.retrobit.penboard.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.data.ToolbarSide

/** Pratinjau papan + tombol yang menyerupai tampilan keyboard sungguhan. */
object Preview {

    fun populate(
        container: LinearLayout,
        config: PenboardConfig,
        density: Float,
        currentTool: ToolMode? = null,
        interactive: Boolean = false,
        onKey: (KeyDef) -> Unit = {}
    ) {
        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        val context = container.context

        val toolbarColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val boardRows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val sideWidth = if (config.toolbarSide == ToolbarSide.SIDE) {
            (config.keyHeightDp * density).toInt()
        } else {
            null
        }

        if (config.showToolbar && config.toolbarRows.isNotEmpty()) {
            KeyRowsBuilder.populate(
                container = toolbarColumn,
                rows = config.toolbarRows,
                config = config,
                density = density,
                haptics = false,
                currentTool = currentTool,
                fixedKeyWidthPx = sideWidth,
                interactive = interactive,
                onKey = onKey
            )
        }

        KeyRowsBuilder.populate(
            container = boardRows,
            rows = config.boardRows,
            config = config,
            density = density,
            haptics = false,
            currentTool = currentTool,
            fixedKeyWidthPx = null,
            interactive = interactive,
            onKey = onKey
        )

        when (config.toolbarSide) {
            ToolbarSide.TOP -> {
                container.addView(toolbarColumn, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                container.addView(boardRows, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }

            ToolbarSide.SIDE -> {
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(toolbarColumn, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                row.addView(boardRows, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                container.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }

            ToolbarSide.BOTTOM -> {
                container.addView(toolbarColumn, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                container.addView(boardRows, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
        }
    }

    fun boardPreview(container: LinearLayout, board: BoardView, config: PenboardConfig, density: Float, maxHeightFraction: Float = 0.45f) {
        val screenHeight = container.resources.displayMetrics.heightPixels
        val height = (config.boardHeightDp * density).toInt()
            .coerceAtMost((screenHeight * maxHeightFraction).toInt())
        board.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, height)
        board.cornerRadiusPx = Ui.dpF(container.context, 16f)
        board.applyConfig(config)
        if (board.parent == null) container.addView(board)
    }
}
