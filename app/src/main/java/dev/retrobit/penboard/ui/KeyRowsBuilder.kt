package dev.retrobit.penboard.ui

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import dev.retrobit.penboard.board.KeyButton
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.RowDef
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.util.KeyLayout

/**
 * Membangun baris tombol (dipakai keyboard IME maupun pratinjau di aplikasi).
 * Lebar tombol dihitung oleh [KeyLayout] memakai bobot LinearLayout, sehingga
 * hasilnya persis mengikuti pengaturan lebar / "isi sisa ruang" milik pengguna.
 */
object KeyRowsBuilder {

    const val SPACING_DP = 5f
    private const val WEIGHT_UNITS = 10_000

    fun populate(
        container: LinearLayout,
        rows: List<RowDef>,
        config: PenboardConfig,
        density: Float,
        haptics: Boolean = true,
        currentTool: ToolMode? = null,
        fixedKeyWidthPx: Int? = null,
        interactive: Boolean = true,
        onKey: (KeyDef) -> Unit
    ): List<KeyButton> {
        container.removeAllViews()
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER

        val buttons = mutableListOf<KeyButton>()
        val spacing = (SPACING_DP * density).toInt().coerceAtLeast(1)

        rows.forEachIndexed { rowIndex, row ->
            if (row.keys.isEmpty()) return@forEachIndexed
            val rowView = LinearLayout(container.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val heightPx = (config.keyHeightDp * density * row.height).toInt().coerceAtLeast(1)
            val widths = KeyLayout.computeWidths(row.keys, WEIGHT_UNITS)

            row.keys.forEachIndexed { index, keyDef ->
                val button = KeyButton(container.context)
                button.key = keyDef
                button.applyPresentation(config, density, haptics)
                button.active = isActive(keyDef, currentTool)
                button.isEnabled = interactive
                button.onTrigger = onKey

                val params = if (fixedKeyWidthPx != null) {
                    LinearLayout.LayoutParams(fixedKeyWidthPx, heightPx)
                } else {
                    LinearLayout.LayoutParams(0, heightPx, widths.getOrElse(index) { 1 }.toFloat().coerceAtLeast(1f))
                }
                params.marginStart = spacing / 2
                params.marginEnd = spacing / 2
                if (rowIndex > 0) params.topMargin = spacing
                button.layoutParams = params

                rowView.addView(button)
                buttons.add(button)
            }
            container.addView(rowView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        if (rows.isNotEmpty()) {
            container.setPadding(0, spacing / 2, 0, spacing / 2)
        }
        return buttons
    }

    fun isActive(key: KeyDef, tool: ToolMode?): Boolean = when (key.action) {
        KeyAction.WRITE -> tool == ToolMode.WRITE
        KeyAction.ERASE -> tool == ToolMode.ERASE
        else -> false
    }
}
