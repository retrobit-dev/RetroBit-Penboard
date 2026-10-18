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
import android.widget.ScrollView
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.doAfterTextChanged
import dev.retrobit.penboard.R

/** Pemilih warna: contoh, merah/hijau/biru, transparansi, dan kode heksadesimal. */
object ColorPicker {

    /**
     * Contoh warna: baris pertama-tama tangga abu-abu (sesuai tema retro grayscale
     * ePaper), baru warna lain untuk yang ingin menyimpang dari tema.
     */
    private val swatches = listOf(
        0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF333333.toInt(), 0xFF4D4D4D.toInt(),
        0xFF666666.toInt(), 0xFF808080.toInt(), 0xFF999999.toInt(), 0xFFB3B3B3.toInt(),
        0xFFCCCCCC.toInt(), 0xFFE6E6E6.toInt(), 0xFFFFFFFF.toInt(), 0xFF8C8C8C.toInt(),
        0xFFEF4444.toInt(), 0xFFF97316.toInt(), 0xFFFACC15.toInt(), 0xFF22C55E.toInt(),
        0xFF06B6D4.toInt(), 0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(),
        0xFF111827.toInt(), 0xFF334155.toInt(), 0xFF94A3B8.toInt(), 0xFF7C2D12.toInt()
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

        var writingHex = false
        val hexField = EditText(context).apply {
            setText(hex(chosen))
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "#RRGGBB"
            textSize = 14f
            setSingleLine(true)
            setPadding(0, Ui.dp(context, 10f), 0, 0)
        }
        content.addView(hexField)

        val redBar = channelBar(context, content, context.getString(R.string.color_channel_red), Color.red(chosen))
        val greenBar = channelBar(context, content, context.getString(R.string.color_channel_green), Color.green(chosen))
        val blueBar = channelBar(context, content, context.getString(R.string.color_channel_blue), Color.blue(chosen))
        fun paintFromChannels() {
            chosen = Color.argb(Color.alpha(chosen), redBar.progress, greenBar.progress, blueBar.progress)
            previewDrawable.setColor(chosen)
            writingHex = true
            hexField.setText(hex(chosen))
            writingHex = false
        }
        val channelListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) paintFromChannels()
            }
            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        }
        redBar.setOnSeekBarChangeListener(channelListener)
        greenBar.setOnSeekBarChangeListener(channelListener)
        blueBar.setOnSeekBarChangeListener(channelListener)
        hexField.doAfterTextChanged {
            if (writingHex) return@doAfterTextChanged
            val parsed = parseHex(it?.toString().orEmpty()) ?: return@doAfterTextChanged
            chosen = Color.argb(Color.alpha(chosen), Color.red(parsed), Color.green(parsed), Color.blue(parsed))
            previewDrawable.setColor(chosen)
            redBar.progress = Color.red(chosen)
            greenBar.progress = Color.green(chosen)
            blueBar.progress = Color.blue(chosen)
        }

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
                    redBar.progress = Color.red(chosen)
                    greenBar.progress = Color.green(chosen)
                    blueBar.progress = Color.blue(chosen)
                    writingHex = true
                    hexField.setText(hex(chosen))
                    writingHex = false
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

        val scroll = ScrollView(context).apply {
            isFillViewport = false
            val height = (context.resources.displayMetrics.heightPixels * 0.55f).toInt()
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            addView(content)
        }
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(scroll)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parseHex(hexField.text.toString())?.let { parsed ->
                    chosen = Color.argb(Color.alpha(chosen), Color.red(parsed), Color.green(parsed), Color.blue(parsed))
                }
                onPick(chosen)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun channelBar(context: Context, parent: LinearLayout, label: String, value: Int): SeekBar {
        parent.addView(AppCompatTextView(context).apply {
            text = label
            textSize = 13f
            setPadding(0, Ui.dp(context, 8f), 0, 0)
        })
        val bar = SeekBar(context).apply {
            max = 255
            progress = value
        }
        parent.addView(bar)
        return bar
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
