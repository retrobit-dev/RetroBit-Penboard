package dev.retrobit.penboard.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatTextView

/** Pemilih warna sederhana: contoh warna, pengatur transparansi, dan kode heksadesimal. */
object ColorPicker {

    private val swatches = listOf(
        0xFFEF4444.toInt(), 0xFFF97316.toInt(), 0xFFFACC15.toInt(), 0xFF22C55E.toInt(),
        0xFF06B6D4.toInt(), 0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(),
        0xFFFCA5A5.toInt(), 0xFFFDBA74.toInt(), 0xFFFDE68A.toInt(), 0xFF86EFAC.toInt(),
        0xFF67E8F9.toInt(), 0xFF93C5FD.toInt(), 0xFFC4B5FD.toInt(), 0xFFF9A8D4.toInt(),
        0xFF111827.toInt(), 0xFF334155.toInt(), 0xFF94A3B8.toInt(), 0xFFE2E8F0.toInt(),
        0xFFFFFFFF.toInt(), 0xFF0EA5E9.toInt(), 0xFFA3E635.toInt(), 0xFF7C2D12.toInt()
    )

    fun show(context: Context, title: String, initial: Int, onPick: (Int) -> Unit) {
        var chosen = initial

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(context, 20f), Ui.dp(context, 12f), Ui.dp(context, 20f), Ui.dp(context, 4f))
        }
        val preview = View(context)
        val previewDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = Ui.dp(context, 12f).toFloat()
            setColor(chosen)
            setStroke(Ui.dp(context, 1f), 0x33000000)
        }
        preview.background = previewDrawable
        content.addView(
            preview,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 42f))
        )

        val hexField = EditText(context).apply {
            setText(hex(chosen))
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "#RRGGBB"
            textSize = 14f
            setSingleLine(true)
            setPadding(0, Ui.dp(context, 10f), 0, 0)
        }
        content.addView(hexField)

        val alphaLabel = AppCompatTextView(context).apply {
            textSize = 13f
            text = alphaText(chosen)
        }
        content.addView(alphaLabel)
        val alphaBar = SeekBar(context).apply {
            max = 255
            progress = Color.alpha(chosen)
        }
        alphaBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                chosen = Color.argb(progress, Color.red(chosen), Color.green(chosen), Color.blue(chosen))
                previewDrawable.setColor(chosen)
                alphaLabel.text = alphaText(chosen)
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
        content.addView(alphaBar)

        val grid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        var row: LinearLayout? = null
        swatches.forEachIndexed { index, color ->
            if (index % 6 == 0) {
                row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                grid.addView(row)
            }
            val cell = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = Ui.dp(context, 10f).toFloat()
                    setColor(color)
                    setStroke(Ui.dp(context, 1f), 0x33000000)
                }
                setOnClickListener {
                    chosen = Color.argb(Color.alpha(chosen), Color.red(color), Color.green(color), Color.blue(color))
                    previewDrawable.setColor(chosen)
                    hexField.setText(hex(chosen))
                }
            }
            row?.addView(
                cell,
                LinearLayout.LayoutParams(0, Ui.dp(context, 34f), 1f).apply {
                    marginEnd = Ui.dp(context, 6f)
                    topMargin = Ui.dp(context, 6f)
                }
            )
        }
        content.addView(grid)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(content)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parseHex(hexField.text.toString())?.let { parsed ->
                    chosen = Color.argb(Color.alpha(chosen), Color.red(parsed), Color.green(parsed), Color.blue(parsed))
                }
                onPick(chosen)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun alphaText(color: Int): String = "Transparansi warna: ${Color.alpha(color) * 100 / 255}%"

    private fun hex(color: Int): String = String.format("#%06X", color and 0xFFFFFF)

    private fun parseHex(text: String): Int? {
        val clean = text.trim().removePrefix("#")
        if (clean.length != 6) return null
        return try {
            (0xFF000000L or clean.toLong(16)).toInt()
        } catch (error: NumberFormatException) {
            null
        }
    }
}
