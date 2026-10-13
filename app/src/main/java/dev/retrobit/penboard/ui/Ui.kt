package dev.retrobit.penboard.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import dev.retrobit.penboard.data.Colors

/** Pembantu pembuatan tampilan secara programatik (agar pengaturan mudah ditambah). */
object Ui {

    fun dp(context: Context, value: Float): Int = (value * context.resources.displayMetrics.density).toInt()

    fun dpF(context: Context, value: Float): Float = value * context.resources.displayMetrics.density

    fun card(parent: ViewGroup, title: String? = null, subtitle: String? = null): LinearLayout {
        val context = parent.context
        val card = MaterialCardView(context)
        card.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = dp(context, 12f)
        }
        card.radius = dpF(context, 18f)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16f), dp(context, 14f), dp(context, 16f), dp(context, 14f))
        }
        card.addView(content, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        if (title != null) sectionTitle(content, title)
        if (subtitle != null) bodyText(content, subtitle)
        parent.addView(card)
        return content
    }

    fun sectionTitle(parent: ViewGroup, text: String): TextView {
        val view = TextView(parent.context).apply {
            this.text = text
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        parent.addView(view)
        return view
    }

    fun bodyText(parent: ViewGroup, text: String): TextView {
        val view = TextView(parent.context).apply {
            this.text = text
            textSize = 13.5f
            alpha = 0.78f
            setLineSpacing(dpF(context, 3f), 1f)
        }
        val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        params.topMargin = dp(parent.context, 4f)
        parent.addView(view, params)
        return view
    }

    fun space(parent: ViewGroup, heightDp: Float) {
        val view = View(parent.context)
        parent.addView(view, LinearLayout.LayoutParams(1, dp(parent.context, heightDp)))
    }

    fun divider(parent: ViewGroup) {
        val view = View(parent.context).apply { setBackgroundColor(Color.argb(38, 128, 128, 128)) }
        val params = LinearLayout.LayoutParams(MATCH_PARENT, dp(parent.context, 1f))
        params.topMargin = dp(parent.context, 10f)
        params.bottomMargin = dp(parent.context, 10f)
        parent.addView(view, params)
    }

    fun switchRow(
        parent: ViewGroup,
        title: String,
        summary: String? = null,
        checked: Boolean,
        onChange: (Boolean) -> Unit
    ): MaterialSwitch {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val titleView = TextView(context).apply {
            text = title
            textSize = 14.5f
        }
        column.addView(titleView)
        if (summary != null) {
            column.addView(TextView(context).apply {
                text = summary
                textSize = 12f
                alpha = 0.65f
            })
        }
        row.addView(column, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        val switch = MaterialSwitch(context).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
        row.addView(switch, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

        val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        params.topMargin = dp(context, 6f)
        parent.addView(row, params)
        row.setOnClickListener { switch.isChecked = !switch.isChecked }
        return switch
    }

    fun sliderRow(
        parent: ViewGroup,
        title: String,
        value: Int,
        min: Int,
        max: Int,
        unit: String = "",
        suffix: String? = null,
        onChange: (Int) -> Unit
    ): Slider {
        val context = parent.context
        val safeMax = max.coerceAtLeast(min + 1)
        val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val titleView = TextView(context).apply { text = title; textSize = 14.5f }
        header.addView(titleView, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val valueView = TextView(context).apply {
            text = if (unit.isNotEmpty()) "$value $unit" else value.toString()
            textSize = 14f
            alpha = 0.8f
        }
        header.addView(valueView)
        val headerParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        headerParams.topMargin = dp(context, 8f)
        parent.addView(header, headerParams)

        val slider = Slider(context).apply {
            valueFrom = min.toFloat()
            valueTo = safeMax.toFloat()
            stepSize = 1f
            this.value = value.coerceIn(min, safeMax).toFloat()
            addOnChangeListener { _, newValue, _ ->
                val intValue = newValue.toInt()
                valueView.text = if (unit.isNotEmpty()) "$intValue $unit" else intValue.toString()
                suffix?.let { valueView.text = "$intValue $it" }
                onChange(intValue)
            }
        }
        parent.addView(slider, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return slider
    }

    fun colorRow(parent: ViewGroup, title: String, color: Int, onChange: (Int) -> Unit) {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
        }
        row.addView(TextView(context).apply { text = title; textSize = 14.5f },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        val hexView = TextView(context).apply {
            text = Colors.toHex(color)
            textSize = 12.5f
            alpha = 0.7f
        }
        row.addView(hexView)

        val swatch = View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dpF(context, 8f)
                setColor(color)
                setStroke(dp(context, 1f), Color.argb(60, 128, 128, 128))
            }
        }
        val swatchParams = LinearLayout.LayoutParams(dp(context, 34f), dp(context, 24f))
        swatchParams.marginStart = dp(context, 10f)
        row.addView(swatch, swatchParams)

        val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        params.topMargin = dp(context, 8f)
        parent.addView(row, params)

        row.setOnClickListener {
            ColorPickerDialog(context, color) { picked ->
                (swatch.background as GradientDrawable).setColor(picked)
                hexView.text = Colors.toHex(picked)
                onChange(picked)
            }.show()
        }
    }

    fun button(context: Context, text: String, outlined: Boolean = false): MaterialButton {
        val button = if (outlined) {
            MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        } else {
            MaterialButton(context)
        }
        button.text = text
        button.isAllCaps = false
        button.textSize = 13.5f
        button.minWidth = 0
        button.insetTop = 0
        button.insetBottom = 0
        return button
    }

    fun buttonRow(parent: ViewGroup, vararg actions: Pair<String, () -> Unit>) {
        val context = parent.context
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        actions.forEachIndexed { index, (label, action) ->
            val button = button(context, label, outlined = index > 0)
            button.setOnClickListener { action() }
            val params = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            if (index > 0) params.marginStart = dp(context, 8f)
            row.addView(button, params)
        }
        val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        params.topMargin = dp(context, 10f)
        parent.addView(row, params)
    }

    fun fullButton(parent: ViewGroup, text: String, outlined: Boolean = false, onClick: () -> Unit) {
        val context = parent.context
        val button = button(context, text, outlined)
        button.setOnClickListener { onClick() }
        val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        params.topMargin = dp(context, 8f)
        parent.addView(button, params)
    }

    fun listRow(
        parent: ViewGroup,
        title: String,
        subtitle: String?,
        badge: String?,
        actionLabel: String?,
        onAction: (() -> Unit)?,
        onMore: (() -> Unit)?,
        onClick: () -> Unit
    ) {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            setPadding(0, dp(context, 8f), 0, dp(context, 8f))
        }
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val titleRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(TextView(context).apply { text = title; textSize = 15f })
        if (badge != null) {
            val badgeView = TextView(context).apply {
                text = badge
                textSize = 11f
                setTextColor(Color.WHITE)
                setPadding(dp(context, 7f), dp(context, 2f), dp(context, 7f), dp(context, 2f))
                background = GradientDrawable().apply {
                    cornerRadius = dpF(context, 20f)
                    setColor(0xFF16A34A.toInt())
                }
            }
            val badgeParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            badgeParams.marginStart = dp(context, 8f)
            titleRow.addView(badgeView, badgeParams)
        }
        column.addView(titleRow)
        if (subtitle != null) {
            column.addView(TextView(context).apply { text = subtitle; textSize = 12.5f; alpha = 0.65f })
        }
        row.addView(column, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        if (actionLabel != null && onAction != null) {
            val actionButton = button(context, actionLabel, outlined = true).apply {
                setOnClickListener { onAction() }
            }
            val params = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            params.marginStart = dp(context, 6f)
            row.addView(actionButton, params)
        }
        if (onMore != null) {
            val moreButton = button(context, "⋯", outlined = true).apply {
                setOnClickListener { onMore() }
            }
            val params = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            params.marginStart = dp(context, 6f)
            row.addView(moreButton, params)
        }

        row.setOnClickListener { onClick() }
        parent.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        divider(parent)
    }

    fun textInputDialog(context: Context, title: String, initial: String, hint: String? = null, onValue: (String) -> Unit) {
        val input = EditText(context).apply {
            setText(initial)
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT
            hint?.let { this.hint = it }
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20f), dp(context, 8f), dp(context, 20f), 0)
        }
        container.addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ -> onValue(input.text.toString().trim()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun confirmDialog(context: Context, title: String, message: String?, confirmLabel: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(context).setTitle(title)
        if (message != null) builder.setMessage(message)
        builder.setPositiveButton(confirmLabel) { _, _ -> onConfirm() }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }
}
