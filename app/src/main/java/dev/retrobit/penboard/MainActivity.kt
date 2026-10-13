package dev.retrobit.penboard

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.button.MaterialButton
import dev.retrobit.penboard.data.ConfigStore
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenButtonAction
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.ink.InkRecognition
import dev.retrobit.penboard.ink.ModelStatus
import dev.retrobit.penboard.ink.RecognitionLanguages
import dev.retrobit.penboard.overlay.PenboardOverlayService
import dev.retrobit.penboard.ui.BoardPanel
import dev.retrobit.penboard.ui.ColorPicker
import dev.retrobit.penboard.ui.Ui
import dev.retrobit.penboard.util.GridMath
import dev.retrobit.penboard.util.StylusCompat
import dev.retrobit.penboard.util.SystemHandwriting

/** Layar pengaturan: mengaktifkan keyboard, papan melayang, dan seluruh pilihan tampilan. */
class MainActivity : AppCompatActivity() {

    private lateinit var store: ConfigStore
    private lateinit var content: LinearLayout
    private lateinit var testField: EditText
    private var previewPanel: BoardPanel? = null

    private val configListener: (PenboardConfig) -> Unit = { config ->
        previewPanel?.applyConfig(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ConfigStore.get(this)
        testField = EditText(this).apply {
            hint = getString(R.string.test_hint)
            minLines = 2
            gravity = Gravity.TOP or Gravity.START
            textSize = 15f
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            StylusCompat.disableSystemHandwriting(this)
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(16f), dp(16f), dp(28f))
        }
        setContentView(ScrollView(this).apply { addView(content) })
        store.addListener(configListener)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) render()
    }

    override fun onDestroy() {
        store.removeListener(configListener)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startOverlay()
        }
    }

    // ------------------------------------------------------------------ tata letak

    private fun render() {
        content.removeAllViews()
        renderHeader()
        renderKeyboardCard()
        renderOverlayCard()
        renderDiagnosticsCard()
        renderPreviewCard()
        renderTestCard()
        renderBoardCard()
        renderGridCard()
        renderRecognitionCard()
        renderLookCard()
        renderHuaweiCard()
        renderAboutCard()
    }

    private fun renderHeader() {
        content.addView(
            AppCompatTextView(this).apply {
                text = getString(R.string.app_name)
                textSize = 25f
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        content.addView(
            AppCompatTextView(this).apply {
                text = getString(R.string.app_tagline)
                textSize = 13.5f
                alpha = 0.75f
                setPadding(0, dp(4f), 0, dp(14f))
            }
        )
    }

    private fun renderKeyboardCard() {
        val card = Ui.card(content, getString(R.string.card_keyboard), getString(R.string.card_keyboard_sub))
        Ui.statusText(
            card,
            getString(if (isPenboardActive()) R.string.status_keyboard_active else R.string.status_keyboard_inactive)
        )
        Ui.buttonRow(
            card,
            getString(R.string.action_pick_keyboard) to { showKeyboardPicker() },
            getString(R.string.action_keyboard_settings) to { openKeyboardSettings() }
        )
    }

    private fun renderOverlayCard() {
        val card = Ui.card(content, getString(R.string.card_overlay), getString(R.string.card_overlay_sub))
        val canDraw = Settings.canDrawOverlays(this)
        Ui.statusText(
            card,
            getString(
                when {
                    !canDraw -> R.string.status_overlay_permission_missing
                    PenboardOverlayService.running -> R.string.status_overlay_running
                    else -> R.string.status_overlay_ready
                }
            )
        )
        if (canDraw) {
            val running = PenboardOverlayService.running
            Ui.buttonRow(
                card,
                getString(if (running) R.string.action_overlay_hide else R.string.action_overlay_show) to {
                    if (running) {
                        PenboardOverlayService.stop(this)
                    } else {
                        requestOverlayStart()
                    }
                    content.postDelayed({ render() }, 500)
                }
            )
        } else {
            Ui.buttonRow(
                card,
                getString(R.string.action_overlay_permission) to { requestOverlayPermission() }
            )
        }
        Ui.body(card, getString(R.string.overlay_note))
    }

    /** Panduan tiga langkah + pemeriksaan perangkat. */
    private fun renderDiagnosticsCard() {
        val card = Ui.card(content, getString(R.string.card_check), getString(R.string.card_check_sub))
        val keyboardActive = isPenboardActive()
        step(card, getString(R.string.step_keyboard_enable), keyboardActive)
        step(card, getString(R.string.step_keyboard_pick), keyboardActive)
        step(card, getString(R.string.step_test), !testField.text.isNullOrBlank())
        Ui.buttonRow(
            card,
            getString(R.string.action_keyboard_settings) to { openKeyboardSettings() },
            getString(R.string.action_pick_keyboard) to { showKeyboardPicker() }
        )

        Ui.divider(card)
        val engineReady = InkRecognition.isServicesAvailable(this)
        Ui.statusText(card, getString(if (engineReady) R.string.check_engine_ok else R.string.check_engine_no))
        val deviceInk = SystemHandwriting.pickBest(SystemHandwriting.candidates(this), packageName)
        Ui.statusText(
            card,
            if (deviceInk != null) {
                getString(R.string.check_device_ink_ok, deviceInk.label)
            } else {
                getString(R.string.check_device_ink_no)
            }
        )
        Ui.statusText(
            card,
            getString(if (Settings.canDrawOverlays(this)) R.string.check_overlay_ok else R.string.check_overlay_no)
        )
    }

    /** Satu langkah panduan dengan tanda selesai/belum. */
    private fun step(parent: LinearLayout, text: String, done: Boolean) {
        Ui.statusText(parent, getString(if (done) R.string.step_done else R.string.step_todo) + " " + text)
    }

    private fun renderPreviewCard() {
        val card = Ui.card(content, getString(R.string.card_preview), getString(R.string.card_preview_sub))
        val panel = previewPanel ?: BoardPanel(this, showHandle = true).also { previewPanel = it }
        detach(panel.root)
        panel.listener = object : BoardPanel.Listener {
            override fun onKey(key: KeyDef, action: KeyAction) {
                toast(getString(R.string.preview_key_tap, store.config.labelFor(key)))
            }

            override fun onStrokeStarted() = Unit
            override fun onStrokeCommitted() = Unit
            override fun onPenButton(primary: Boolean) = Unit
            override fun onContentChanged() = Unit
        }
        // Tinggi pratinjau mengikuti papan atau grid tombol, mana yang lebih tinggi.
        val density = resources.displayMetrics.density
        val spacingPx = (GridMath.SPACING_DP * density).toInt()
        val boardHeightPx = (store.config.boardHeightDp * density).toInt()
        val cellPx = GridMath.cellSizePx(
            boardHeightPx,
            resources.displayMetrics.widthPixels,
            spacingPx,
            (44 * density).toInt(),
            store.config.gridRows
        )
        val gridHeightPx = cellPx * store.config.gridRows + spacingPx * (store.config.gridRows - 1)
        val previewHeight = maxOf(boardHeightPx, gridHeightPx) + (24 * density).toInt()
        card.addView(
            panel.root,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, previewHeight)
        )
        panel.applyConfig(store.config)
    }

    private fun renderTestCard() {
        val card = Ui.card(content, getString(R.string.card_test), getString(R.string.card_test_sub))
        detach(testField)
        card.addView(
            testField,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        Ui.outlinedButton(card, getString(R.string.action_clear_text)) { testField.setText("") }
    }

    private fun renderBoardCard() {
        val config = store.config
        val card = Ui.card(content, getString(R.string.card_board), getString(R.string.card_board_sub))
        Ui.sliderRow(
            card, getString(R.string.setting_board_height), config.boardHeightDp,
            PenboardConfig.MIN_BOARD_HEIGHT_DP, PenboardConfig.MAX_BOARD_HEIGHT_DP, "dp"
        ) { value -> store.update { it.copy(boardHeightDp = value) } }
        Ui.sliderRow(
            card, getString(R.string.setting_line_gap), config.lineGapDp,
            PenboardConfig.MIN_LINE_GAP_DP, PenboardConfig.MAX_LINE_GAP_DP, "dp"
        ) { value -> store.update { it.copy(lineGapDp = value) } }
        Ui.sliderRow(
            card, getString(R.string.setting_pen_width), config.penWidthDp,
            PenboardConfig.MIN_PEN_WIDTH_DP, PenboardConfig.MAX_PEN_WIDTH_DP, "dp"
        ) { value -> store.update { it.copy(penWidthDp = value) } }

        Ui.divider(card)
        Ui.body(card, getString(R.string.setting_pen_colors))
        config.penColors.forEachIndexed { index, color ->
            Ui.colorRow(card, getString(R.string.setting_pen_color_n, index + 1), color) { picked ->
                store.update { current ->
                    current.copy(penColors = current.penColors.toMutableList().also { it[index] = picked })
                }
                render()
            }
        }
        Ui.buttonRow(
            card,
            getString(R.string.action_add_pen_color) to { addPenColor() },
            getString(R.string.action_remove_pen_color) to { removePenColor() }
        )

        Ui.divider(card)
        Ui.switchRow(
            card, getString(R.string.setting_show_lines), getString(R.string.setting_show_lines_sub), config.showLines
        ) { value -> store.update { it.copy(showLines = value) } }
        Ui.switchRow(
            card, getString(R.string.setting_pressure), getString(R.string.setting_pressure_sub), config.pressureEnabled
        ) { value -> store.update { it.copy(pressureEnabled = value) } }
        Ui.switchRow(
            card, getString(R.string.setting_allow_finger), getString(R.string.setting_allow_finger_sub), config.allowFinger
        ) { value -> store.update { it.copy(allowFinger = value) } }
        Ui.switchRow(
            card, getString(R.string.setting_haptics), getString(R.string.setting_haptics_sub), config.haptics
        ) { value -> store.update { it.copy(haptics = value) } }

        Ui.divider(card)
        Ui.body(card, getString(R.string.setting_pen_buttons))
        val penActions = PenButtonAction.entries
        Ui.spinnerRow(
            card,
            getString(R.string.setting_pen_button_primary),
            penActions.map { it.label },
            penActions.indexOf(config.penButtonPrimary).coerceAtLeast(0)
        ) { index ->
            store.update { it.copy(penButtonPrimary = penActions[index]) }
        }
        Ui.spinnerRow(
            card,
            getString(R.string.setting_pen_button_secondary),
            penActions.map { it.label },
            penActions.indexOf(config.penButtonSecondary).coerceAtLeast(0)
        ) { index ->
            store.update { it.copy(penButtonSecondary = penActions[index]) }
        }
    }

    private fun renderGridCard() {
        val config = store.config
        val card = Ui.card(content, getString(R.string.card_grid), getString(R.string.card_grid_sub))
        Ui.sliderRow(
            card, getString(R.string.setting_grid_rows), config.gridRows,
            PenboardConfig.MIN_GRID_ROWS, PenboardConfig.MAX_GRID_ROWS, getString(R.string.unit_rows)
        ) { value ->
            // Menambah baris langsung mengisi tombol di baris baru; mengurangi baris
            // otomatis memangkas tombol yang tidak punya tempat lagi.
            store.update { it.copy(gridRows = value, gridButtonCount = value * GridMath.COLUMNS) }
        }
        Ui.sliderRow(
            card, getString(R.string.setting_grid_count), config.gridButtonCount,
            PenboardConfig.MIN_GRID_KEYS, config.gridRows * GridMath.COLUMNS, getString(R.string.unit_keys)
        ) { value -> store.update { it.copy(gridButtonCount = value) } }

        Ui.divider(card)
        for (index in 0 until PenboardConfig.GRID_SLOTS) {
            val key = config.gridKeys.getOrNull(index) ?: PenboardConfig.defaultGridKeys()[index]
            val enabled = index < config.gridButtonCount
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5f), 0, dp(5f))
            }
            row.addView(
                AppCompatTextView(this).apply {
                    text = getString(R.string.grid_slot_label, index + 1, config.labelFor(key))
                    textSize = 14f
                    alpha = if (enabled) 1f else 0.4f
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            row.addView(
                MaterialButton(this).apply {
                    text = getString(R.string.action_change)
                    isAllCaps = false
                    textSize = 12.5f
                    isEnabled = enabled
                    setOnClickListener { showKeyEditor(index) }
                }
            )
            card.addView(row)
        }
        Ui.divider(card)
        Ui.outlinedButton(card, getString(R.string.action_grid_default)) {
            store.update {
                it.copy(
                    gridKeys = PenboardConfig.defaultGridKeys(),
                    gridRows = PenboardConfig.DEFAULT_ROWS,
                    gridButtonCount = PenboardConfig.DEFAULT_ROWS * GridMath.COLUMNS
                )
            }
            render()
        }
    }

    private fun renderRecognitionCard() {
        val config = store.config
        val card = Ui.card(content, getString(R.string.card_recognition), getString(R.string.card_recognition_sub))
        Ui.switchRow(
            card, getString(R.string.setting_auto_recognize), getString(R.string.setting_auto_recognize_sub), config.autoRecognize
        ) { value -> store.update { it.copy(autoRecognize = value) } }
        Ui.sliderRow(
            card, getString(R.string.setting_auto_delay), config.autoRecognizeDelayMs,
            PenboardConfig.MIN_AUTO_DELAY_MS, PenboardConfig.MAX_AUTO_DELAY_MS, "ms"
        ) { value -> store.update { it.copy(autoRecognizeDelayMs = value) } }
        Ui.switchRow(
            card, getString(R.string.setting_auto_space), getString(R.string.setting_auto_space_sub), config.autoSpace
        ) { value -> store.update { it.copy(autoSpace = value) } }
        Ui.switchRow(
            card, getString(R.string.setting_clear_after), getString(R.string.setting_clear_after_sub), config.clearAfterRecognize
        ) { value -> store.update { it.copy(clearAfterRecognize = value) } }

        Ui.divider(card)
        val languages = RecognitionLanguages.all
        Ui.spinnerRow(
            card,
            getString(R.string.setting_language),
            languages.map { it.second },
            languages.indexOfFirst { it.first == config.recognitionLanguage }.coerceAtLeast(0)
        ) { position ->
            store.update { it.copy(recognitionLanguage = languages[position].first) }
            render()
        }
        Ui.switchRow(
            card,
            getString(R.string.setting_auto_switch),
            getString(R.string.setting_auto_switch_sub),
            config.autoSwitchToSystemHandwriting
        ) { value -> store.update { it.copy(autoSwitchToSystemHandwriting = value) } }

        val status = Ui.statusText(card, getString(R.string.status_model_checking))
        showModelStatus(status)
        Ui.buttonRow(card, getString(R.string.action_download_model) to { downloadModel(status) })
        Ui.body(card, getString(R.string.recognition_note))
    }

    private fun renderLookCard() {
        val config = store.config
        val card = Ui.card(content, getString(R.string.card_look), getString(R.string.card_look_sub))
        Ui.body(card, getString(R.string.theme_note))
        Ui.outlinedButton(card, getString(R.string.action_theme_retro)) {
            store.update { it.withRetroTheme() }
            render()
        }
        Ui.switchRow(
            card,
            getString(R.string.setting_scanlines),
            getString(R.string.setting_scanlines_sub),
            config.scanlines
        ) { value -> store.update { it.copy(scanlines = value) } }
        Ui.divider(card)
        Ui.sliderRow(card, getString(R.string.setting_panel_opacity), config.panelOpacity, 0, 100, "%") { value ->
            store.update { it.copy(panelOpacity = value) }
        }
        Ui.colorRow(card, getString(R.string.setting_panel_color), config.panelColor) { picked ->
            store.update { it.copy(panelColor = picked) }
            render()
        }
        Ui.sliderRow(card, getString(R.string.setting_board_opacity), config.boardOpacity, 0, 100, "%") { value ->
            store.update { it.copy(boardOpacity = value) }
        }
        Ui.colorRow(card, getString(R.string.setting_board_color), config.boardColor) { picked ->
            store.update { it.copy(boardColor = picked) }
            render()
        }
        Ui.colorRow(card, getString(R.string.setting_line_color), config.lineColor) { picked ->
            store.update { it.copy(lineColor = picked) }
            render()
        }
        Ui.sliderRow(card, getString(R.string.setting_button_opacity), config.buttonOpacity, 0, 100, "%") { value ->
            store.update { it.copy(buttonOpacity = value) }
        }
        Ui.colorRow(card, getString(R.string.setting_button_color), config.buttonColor) { picked ->
            store.update { it.copy(buttonColor = picked) }
            render()
        }
        Ui.colorRow(card, getString(R.string.setting_button_text_color), config.buttonTextColor) { picked ->
            store.update { it.copy(buttonTextColor = picked) }
            render()
        }
        Ui.colorRow(card, getString(R.string.setting_accent_color), config.accentColor) { picked ->
            store.update { it.copy(accentColor = picked) }
            render()
        }
        Ui.sliderRow(
            card, getString(R.string.setting_corner_radius), config.cornerRadiusDp,
            PenboardConfig.MIN_RADIUS_DP, PenboardConfig.MAX_RADIUS_DP, "dp"
        ) { value -> store.update { it.copy(cornerRadiusDp = value) } }
    }

    private fun renderHuaweiCard() {
        if (!isHuaweiFamily()) return
        val card = Ui.card(content, getString(R.string.card_huawei), getString(R.string.card_huawei_sub))
        Ui.body(card, getString(R.string.huawei_body))
        Ui.buttonRow(
            card,
            getString(R.string.action_keyboard_settings) to { openKeyboardSettings() },
            getString(R.string.action_pick_keyboard) to { showKeyboardPicker() }
        )
    }

    private fun renderAboutCard() {
        val card = Ui.card(content, getString(R.string.card_about))
        Ui.body(card, getString(R.string.about_version, appVersion()))
        Ui.body(card, getString(R.string.about_privacy))
        card.addView(
            AppCompatTextView(this).apply {
                text = getString(R.string.about_repo)
                textSize = 13.5f
                setTextColor(Color.parseColor("#0EA5E9"))
                setPadding(0, dp(4f), 0, dp(10f))
                setOnClickListener { openRepo() }
            }
        )
        Ui.outlinedButton(card, getString(R.string.action_reset_all)) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.action_reset_all))
                .setMessage(getString(R.string.reset_confirm))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    store.reset()
                    previewPanel?.applyConfig(store.config)
                    render()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    // ------------------------------------------------------------------ tombol grid

    private fun showKeyEditor(index: Int) {
        val config = store.config
        val current = config.gridKeys.getOrNull(index) ?: KeyDef()
        var action = current.action
        var repeat = current.repeatOnHold

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(8f), dp(20f), dp(4f))
        }
        val actions = KeyAction.entries.toList()
        Ui.spinnerRow(
            body,
            getString(R.string.setting_action),
            actions.map { "${it.label} — ${actionHint(it)}" },
            actions.indexOf(action).coerceAtLeast(0)
        ) { position -> action = actions[position] }

        val labelField = EditText(this).apply {
            hint = getString(R.string.setting_custom_label)
            setText(current.label)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        body.addView(labelField)
        val textField = EditText(this).apply {
            hint = getString(R.string.setting_custom_text)
            setText(current.text)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        body.addView(textField)
        Ui.switchRow(
            body, getString(R.string.setting_repeat), getString(R.string.setting_repeat_sub), current.repeatOnHold
        ) { value -> repeat = value }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.grid_slot_title, index + 1))
            .setView(body)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val updated = current.copy(
                    action = action,
                    label = labelField.text.toString().trim(),
                    text = textField.text.toString(),
                    repeatOnHold = repeat
                )
                store.update { cfg ->
                    cfg.copy(gridKeys = cfg.gridKeys.toMutableList().also { it[index] = updated })
                }
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun actionHint(action: KeyAction): String = getString(
        when (action) {
            KeyAction.TEXT -> R.string.hint_text
            KeyAction.SPACE -> R.string.hint_space
            KeyAction.ENTER -> R.string.hint_enter
            KeyAction.BACKSPACE -> R.string.hint_backspace
            KeyAction.DELETE_WORD -> R.string.hint_delete_word
            KeyAction.DELETE_FORWARD -> R.string.hint_delete_forward
            KeyAction.TAB -> R.string.hint_tab
            KeyAction.CURSOR_LEFT -> R.string.hint_cursor_left
            KeyAction.CURSOR_RIGHT -> R.string.hint_cursor_right
            KeyAction.CURSOR_UP -> R.string.hint_cursor_up
            KeyAction.CURSOR_DOWN -> R.string.hint_cursor_down
            KeyAction.CURSOR_HOME -> R.string.hint_cursor_home
            KeyAction.CURSOR_END -> R.string.hint_cursor_end
            KeyAction.SELECT_ALL -> R.string.hint_select_all
            KeyAction.CUT -> R.string.hint_cut
            KeyAction.COPY -> R.string.hint_copy
            KeyAction.PASTE -> R.string.hint_paste
            KeyAction.UNDO -> R.string.hint_undo
            KeyAction.REDO -> R.string.hint_redo
            KeyAction.CLEAR -> R.string.hint_clear
            KeyAction.DELETE_STROKE -> R.string.hint_delete_stroke
            KeyAction.CYCLE_PEN -> R.string.hint_cycle_pen
            KeyAction.RECOGNIZE -> R.string.hint_recognize
            KeyAction.SYSTEM_HANDWRITING -> R.string.hint_system_handwriting
            KeyAction.OVERLAY_TOGGLE -> R.string.hint_overlay
            KeyAction.HIDE -> R.string.hint_hide
        }
    )

    // ------------------------------------------------------------------ bantuan

    private fun addPenColor() {
        val config = store.config
        if (config.penColors.size >= PenboardConfig.MAX_PEN_COLORS) {
            toast(getString(R.string.msg_pen_color_full, PenboardConfig.MAX_PEN_COLORS))
            return
        }
        store.update { current ->
            val next = current.penColors.toMutableList()
            next.add(PenboardConfig.DEFAULT_PEN_COLORS[next.size % PenboardConfig.DEFAULT_PEN_COLORS.size])
            current.copy(penColors = next)
        }
        render()
    }

    private fun removePenColor() {
        val config = store.config
        if (config.penColors.size <= 1) return
        store.update { current ->
            val next = current.penColors.dropLast(1)
            current.copy(penColors = next, penColorIndex = current.penColorIndex.coerceAtMost(next.size - 1))
        }
        render()
    }

    private fun showModelStatus(statusView: TextView) {
        if (!InkRecognition.isServicesAvailable(this)) {
            statusView.text = getString(R.string.msg_engine_missing)
            return
        }
        InkRecognition.modelStatus(store.config.recognitionLanguage) { status ->
            statusView.text = getString(
                when (status) {
                    ModelStatus.READY -> R.string.status_model_ready
                    ModelStatus.NOT_DOWNLOADED -> R.string.status_model_not_downloaded
                    ModelStatus.MISSING -> R.string.status_model_unsupported
                    ModelStatus.UNKNOWN -> R.string.status_model_unknown
                }
            )
        }
    }

    private fun downloadModel(statusView: TextView) {
        if (!InkRecognition.isServicesAvailable(this)) {
            statusView.text = getString(R.string.msg_engine_missing)
            return
        }
        statusView.text = getString(R.string.status_model_downloading)
        InkRecognition.downloadModel(store.config.recognitionLanguage) { ok ->
            statusView.text = getString(if (ok) R.string.status_model_ready else R.string.status_model_failed)
        }
    }

    private fun isPenboardActive(): Boolean {
        val active = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return active?.startsWith(packageName) == true
    }

    private fun showKeyboardPicker() {
        try {
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showInputMethodPicker()
        } catch (error: Throwable) {
            toast(getString(R.string.msg_picker_failed))
        }
    }

    private fun openKeyboardSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        } catch (error: Throwable) {
            toast(getString(R.string.msg_picker_failed))
        }
    }

    private fun requestOverlayPermission() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (error: Throwable) {
            toast(getString(R.string.msg_overlay_permission_failed))
        }
    }

    private fun requestOverlayStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        startOverlay()
    }

    private fun startOverlay() {
        try {
            PenboardOverlayService.start(this)
            toast(getString(R.string.msg_overlay_started))
        } catch (error: Throwable) {
            toast(getString(R.string.msg_overlay_failed))
        }
    }

    private fun openRepo() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.repo_url))))
        } catch (error: Throwable) {
            toast(getString(R.string.repo_url))
        }
    }

    private fun appVersion(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "-"
    } catch (error: Throwable) {
        "-"
    }

    private fun isHuaweiFamily(): Boolean {
        val brand = (Build.BRAND + " " + Build.MANUFACTURER + " " + Build.DEVICE).lowercase()
        return brand.contains("huawei") || brand.contains("honor") || brand.contains("harmony")
    }

    private fun detach(view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
    }

    private fun dp(value: Float): Int = Ui.dp(this, value)

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 40
    }
}
