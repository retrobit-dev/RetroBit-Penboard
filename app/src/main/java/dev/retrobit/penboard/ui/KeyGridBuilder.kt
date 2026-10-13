package dev.retrobit.penboard.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import dev.retrobit.penboard.board.KeyButton
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.util.GridMath

/**
 * Menyusun tombol operasi dalam **grid 3x3** (untuk tata letak "papan di kiri, tombol di kanan").
 *
 * Jumlah tombol yang tampil mengikuti [PenboardConfig.gridButtonCount] (1..9); slot yang belum
 * dipakai tetap disediakan sebagai ruang kosong agar susunan grid tidak bergeser.
 */
object KeyGridBuilder {

    const val COLUMNS = 3
    private const val SPACING_DP = 3f
    /** Tombol grid dibuat sedikit lebih besar dari tinggi tombol biasa agar mudah ditekan. */
    private const val CELL_SCALE = 1.18f

    fun populate(
        container: LinearLayout,
        config: PenboardConfig,
        density: Float,
        haptics: Boolean = true,
        currentTool: ToolMode? = null,
        interactive: Boolean = true,
        /** Tinggi papan tulis; grid dibuat sepadan agar rapi di sisi kanan. */
        boardHeightPx: Int = 0,
        /** Lebar layar yang tersedia (papan mengambil sisanya). */
        availableWidthPx: Int = 0,
        onKey: (KeyDef) -> Unit
    ): List<KeyButton> {
        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.END or Gravity.CENTER_VERTICAL

        val keys = PenboardConfig.withGridDefaults(config.gridKeys)
        val count = config.gridButtonCount.coerceIn(1, PenboardConfig.GRID_SLOTS)
        val spacing = (SPACING_DP * density).toInt().coerceAtLeast(1)
        val fallbackCell = (config.keyHeightDp * CELL_SCALE * density).toInt().coerceAtLeast(1)
        val cell = if (boardHeightPx > 0 && availableWidthPx > 0) {
            GridMath.cellSizePx(
                boardHeightPx = boardHeightPx,
                availableWidthPx = availableWidthPx,
                rows = GridMath.rowsFor(count),
                spacingPx = spacing,
                minCellPx = (34f * density).toInt()
            )
        } else {
            fallbackCell
        }

        val buttons = mutableListOf<KeyButton>()
        for (row in 0 until COLUMNS) {
            val rowView = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            for (column in 0 until COLUMNS) {
                val index = row * COLUMNS + column
                val params = LinearLayout.LayoutParams(cell, cell).apply {
                    // kolom pertama tanpa jarak kiri, kolom terakhir menempel tepi kanan
                    marginStart = if (column == 0) 0 else spacing
                    marginEnd = 0
                    if (row > 0) topMargin = spacing
                }
                if (index >= count) {
                    // slot belum dipakai: ruang kosong agar bentuk grid tetap terjaga
                    rowView.addView(View(container.context), params)
                    continue
                }
                val key = keys[index]
                val button = KeyButton(container.context)
                button.key = key
                button.applyPresentation(config, density, haptics)
                button.active = KeyRowsBuilder.isActive(key, currentTool)
                button.isEnabled = interactive
                button.onTrigger = onKey
                button.layoutParams = params
                rowView.addView(button)
                buttons.add(button)
            }
            container.addView(
                rowView,
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.END
                }
            )
        }
        return buttons
    }
}
