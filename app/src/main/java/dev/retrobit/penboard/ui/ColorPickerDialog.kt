package dev.retrobit.penboard.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import dev.retrobit.penboard.R
import dev.retrobit.penboard.data.Colors

/** Dialog pemilih warna: palet siap pakai + input kode hex. */
class ColorPickerDialog(
    private val context: Context,
    private val initial: Int,
    private val onPicked: (Int) -> Unit
) {

    private var selected: Int = initial

    fun show() {
        val padding = Ui.dp(context, 20f)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, Ui.dp(context, 8f), padding, 0)
        }

        val preview = View(context)
        root.addView(preview, LinearLayout.LayoutParams(MATCH_PARENT, Ui.dp(context, 34f)))

        val hexInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setText(Colors.toHex(selected))
            hint = context.getString(R.string.color_hex_label)
        }
        val hexParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        hexParams.topMargin = Ui.dp(context, 12f)
        root.addView(hexInput, hexParams)

        root.addView(TextView(context).apply {
            text = context.getString(R.string.color_dialog_title)
            textSize = 13f
            alpha = 0.7f
        }, hexParams)

        val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        root.addView(grid, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val swatchViews = mutableListOf<View>()

        fun paintSwatch(view: View, color: Int, isSelected: Boolean) {
            view.background = GradientDrawable().apply {
                cornerRadius = Ui.dpF(context, 10f)
                setColor(color)
                setStroke(
                    Ui.dp(context, if (isSelected) 3f else 1f),
                    if (isSelected) 0xFF3D5AFE.toInt() else 0x55808080
                )
            }
        }

        fun refresh() {
            paintSwatch(preview, selected, true)
            swatchViews.forEachIndexed { index, view ->
                paintSwatch(view, Colors.palette[index], Colors.palette[index] == selected)
            }
        }

        val palette = Colors.palette
        var index = 0
        while (index < palette.size) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (column in 0 until 6) {
                val color = palette.getOrNull(index) ?: break
                val swatch = View(context)
                val size = Ui.dp(context, 40f)
                val params = LinearLayout.LayoutParams(0, size, 1f)
                params.marginStart = Ui.dp(context, 3f)
                params.marginEnd = Ui.dp(context, 3f)
                params.topMargin = Ui.dp(context, 6f)
                swatch.layoutParams = params
                paintSwatch(swatch, color, color == selected)
                swatch.setOnClickListener {
                    selected = color
                    hexInput.setText(Colors.toHex(color))
                    refresh()
                }
                row.addView(swatch)
                swatchViews.add(swatch)
                index++
            }
            grid.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

        refresh()

        AlertDialog.Builder(context)
            .setTitle(R.string.color_dialog_title)
            .setView(root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val typed = Colors.parseHex(hexInput.text.toString(), selected)
                selected = typed
                onPicked(selected)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
