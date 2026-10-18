package dev.retrobit.penboard.ui

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import dev.retrobit.penboard.util.KeyboardChars

/**
 * Baris pemilih karakter untuk editor tombol: angka, huruf, tanda baca, simbol.
 * Ketukan mengisi kotak teks dan mengalihkan fungsi tombol ke Karakter.
 */
object CharPicker {

    fun addRows(parent: LinearLayout, onPick: (String) -> Unit) {
        val context = parent.context
        KeyboardChars.groups.forEach { group ->
            parent.addView(
                AppCompatTextView(context).apply {
                    text = group.title
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    alpha = 0.7f
                    setPadding(0, Ui.dp(context, 8f), 0, Ui.dp(context, 2f))
                }
            )
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            group.chars.forEach { ch ->
                row.addView(chip(parent, ch.toString(), onPick))
            }
            parent.addView(
                HorizontalScrollView(context).apply {
                    isHorizontalScrollBarEnabled = false
                    isFocusable = false
                    addView(row)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun chip(parent: LinearLayout, text: String, onPick: (String) -> Unit): AppCompatTextView {
        val context = parent.context
        val size = Ui.dp(context, 36f)
        return AppCompatTextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.MONOSPACE
            isFocusable = false
            contentDescription = text
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = Ui.dp(context, 6f).toFloat()
                setColor(0xFF1C1C1C.toInt())
                setStroke(Ui.dp(context, 1f), 0xFF5A5A5A.toInt())
            }
            setOnClickListener { onPick(text) }
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = Ui.dp(context, 4f)
            }
        }
    }
}
