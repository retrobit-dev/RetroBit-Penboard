package dev.retrobit.penboard.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt

/** Pembuat tampilan sederhana (semua kartu pengaturan dibuat dari kode). */
object Ui {

    /** Garis tepi kartu tema retro (abu gelap). */
    private val RETRO_BORDER = 0xFF3A3A3A.toInt()

    fun dp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    fun card(parent: LinearLayout, title: String, subtitle: String? = null): LinearLayout {
        val context = parent.context
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(context, 12f) }
            radius = dp(context, 10f).toFloat()
            cardElevation = 0f
            strokeWidth = dp(context, 1f)
            // Dipanggil sebagai fungsi: properti `strokeColor` memilih versi Int.
            setStrokeColor(ColorStateList.valueOf(RETRO_BORDER))
            setContentPadding(dp(context, 16f), dp(context, 14f), dp(context, 16f), dp(context, 16f))
        }
        val inner = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        card.addView(inner)
        inner.addView(
            AppCompatTextView(context).apply {
                text = title.uppercase()
                textSize = 14.5f
                letterSpacing = 0.08f
                typeface = Typeface.MONOSPACE
                setPadding(0, 0, 0, dp(context, 2f))
            }
        )
        if (subtitle != null) {
            inner.addView(
                AppCompatTextView(context).apply {
                    text = subtitle
                    textSize = 12.5f
                    alpha = 0.7f
                    typeface = Typeface.MONOSPACE
                    setPadding(0, 0, 0, dp(context, 8f))
                }
            )
        }
        parent.addView(card)
        return inner
    }

    fun body(parent: LinearLayout, text: String): AppCompatTextView =
        AppCompatTextView(parent.context).apply {
            this.text = text
            textSize = 12.5f
            typeface = Typeface.MONOSPACE
            alpha = 0.85f
            setPadding(0, dp(parent.context, 2f), 0, dp(parent.context, 6f))
            parent.addView(this)
        }

    fun divider(parent: LinearLayout) {
        val context = parent.context
        parent.addView(
            View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, 1f)
                ).apply { topMargin = dp(context, 8f); bottomMargin = dp(context, 6f) }
                setBackgroundColor(0x22000000)
            }
        )
    }

    fun switchRow(
        parent: LinearLayout,
        title: String,
        summary: String? = null,
        checked: Boolean,
        onChange: (Boolean) -> Unit
    ): SwitchCompat {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(context, 6f), 0, dp(context, 6f))
        }
        val labels = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(
            AppCompatTextView(context).apply {
                text = title
                textSize = 13.5f
                typeface = Typeface.MONOSPACE
            }
        )
        if (summary != null) {
            labels.addView(
                AppCompatTextView(context).apply {
                    text = summary
                    textSize = 11.5f
                    alpha = 0.65f
                    typeface = Typeface.MONOSPACE
                }
            )
        }
        row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val switch = SwitchCompat(context).apply {
            isChecked = checked
            setOnCheckedChangeListener { _: CompoundButton, value -> onChange(value) }
        }
        row.addView(switch)
        parent.addView(row)
        return switch
    }

    fun sliderRow(
        parent: LinearLayout,
        title: String,
        value: Int,
        min: Int,
        max: Int,
        unit: String,
        onChange: (Int) -> Unit
    ): SeekBar {
        val context = parent.context
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 6f), 0, 0)
        }
        val titleView = AppCompatTextView(context).apply {
            text = title
            textSize = 13.5f
            typeface = Typeface.MONOSPACE
        }
        val valueView = AppCompatTextView(context).apply {
            text = format(value, unit)
            textSize = 13f
            gravity = Gravity.END
            typeface = Typeface.MONOSPACE
        }
        header.addView(titleView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(valueView)
        parent.addView(header)

        val step = ((max - min).toFloat() / 200f)
        val seek = SeekBar(context).apply {
            this.max = max - min
            progress = (value - min).coerceIn(0, max - min)
            setPadding(0, dp(context, 2f), 0, dp(context, 2f))
        }
        var lastNotified = value
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                val current = min + progress
                valueView.text = format(current, unit)
                if (fromUser && kotlin.math.abs(current - lastNotified) >= keepStep(step)) {
                    lastNotified = current
                    onChange(current)
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit

            override fun onStopTrackingTouch(bar: SeekBar) {
                val current = min + bar.progress
                lastNotified = current
                onChange(current)
            }
        })
        parent.addView(seek)
        return seek
    }

    private fun keepStep(step: Float): Int = if (step <= 0f) 1 else step.toInt().coerceAtLeast(1)

    private fun format(value: Int, unit: String): String =
        if (unit.isEmpty()) value.toString() else "$value $unit"

    fun buttonRow(parent: LinearLayout, vararg buttons: Pair<String, () -> Unit>): LinearLayout {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 4f), 0, 0)
        }
        buttons.forEach { (label, onClick) ->
            val button = MaterialButton(context).apply {
                text = label
                isAllCaps = false
                textSize = 12.5f
                typeface = Typeface.MONOSPACE
                setOnClickListener { onClick() }
            }
            row.addView(
                button,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(context, 8f)
                }
            )
        }
        parent.addView(row)
        return row
    }

    fun outlinedButton(parent: LinearLayout, label: String, onClick: () -> Unit): MaterialButton {
        val context = parent.context
        val button = MaterialButton(context).apply {
            text = label
            isAllCaps = false
            textSize = 12.5f
            typeface = Typeface.MONOSPACE
            strokeWidth = dp(context, 1f)
            strokeColor = ColorStateList.valueOf(RETRO_BORDER)
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            setOnClickListener { onClick() }
        }
        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, 6f) }
        )
        return button
    }

    fun spinnerRow(
        parent: LinearLayout,
        title: String,
        items: List<String>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit
    ): Spinner {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(context, 6f), 0, dp(context, 6f))
        }
        row.addView(
            AppCompatTextView(context).apply {
                text = title
                textSize = 13.5f
                typeface = Typeface.MONOSPACE
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
            setSelection(selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != selectedIndex) onSelect(position)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) = Unit
        }
        row.addView(
            spinner,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f)
        )
        parent.addView(row)
        return spinner
    }

    /** Baris dengan contoh warna yang bisa diketuk untuk memilih warna. */
    fun colorRow(
        parent: LinearLayout,
        title: String,
        color: Int,
        onPick: (Int) -> Unit
    ): View {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            setPadding(0, dp(context, 8f), 0, dp(context, 8f))
            setOnClickListener { onPick(color) }
        }
        row.addView(
            AppCompatTextView(context).apply {
                text = title
                textSize = 13.5f
                typeface = Typeface.MONOSPACE
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        val swatch = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 8f).toFloat()
                setColor(color)
                setStroke(dp(context, 1f), 0x33000000)
            }
        }
        row.addView(swatch, LinearLayout.LayoutParams(dp(context, 34f), dp(context, 24f)))
        parent.addView(row)
        return row
    }

    /** Baris informasi kecil (mis. status izin / status keyboard). */
    fun statusText(parent: LinearLayout, text: String): TextView =
        AppCompatTextView(parent.context).apply {
            this.text = text
            textSize = 12.5f
            typeface = Typeface.MONOSPACE
            alpha = 0.9f
            setPadding(0, dp(parent.context, 2f), 0, dp(parent.context, 4f))
            parent.addView(this)
        }
}
