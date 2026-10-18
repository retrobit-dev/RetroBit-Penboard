package dev.retrobit.penboard

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import dev.retrobit.penboard.data.ConfigStore
import dev.retrobit.penboard.data.IconStyle
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenButtonAction
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.ink.InkRecognition
import dev.retrobit.penboard.ink.InkReport
import dev.retrobit.penboard.ink.ModelStatus
import dev.retrobit.penboard.ink.RecognitionLanguages
import dev.retrobit.penboard.overlay.PenboardTypingService
import dev.retrobit.penboard.ui.BoardPanel
import dev.retrobit.penboard.ui.CharPicker
import dev.retrobit.penboard.ui.ColorPicker
import dev.retrobit.penboard.ui.Ui
import dev.retrobit.penboard.util.GmsStatus
import dev.retrobit.penboard.util.GridMath
import dev.retrobit.penboard.util.KeyboardChars
import dev.retrobit.penboard.util.StylusCompat
import dev.retrobit.penboard.util.SystemHandwriting
import dev.retrobit.penboard.util.TextInsert
import kotlin.math.roundToInt

/** Layar pengaturan: mengaktifkan keyboard Penboard dan seluruh pilihan tampilan. */
class MainActivity : AppCompatActivity() {

    private lateinit var store: ConfigStore
    private lateinit var content: LinearLayout
    private lateinit var testField: EditText

    /** Status model bahasa terakhir yang terbaca (dipakai laporan diagnostik). */
    private var modelStatusText: String? = null
    private var previewPanel: BoardPanel? = null

    /** Sisa tarikan sudut yang belum genap 1 dp (lebar papan memakai pecahan, tinggi memakai dp). */
    private var pendingResizeDp = 0f

    private val configListener: (PenboardConfig) -> Unit = { config ->
        // Kartu pratinjau selalu memakai ukuran terbaru, jadi tingginya ikut disesuaikan
        // tanpa perlu menyusun ulang seluruh layar (yang akan menggeser posisi pengguna).
        previewPanel?.let { panel ->
            panel.applyConfig(config)
            (panel.root.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                params.height = panel.panelHeightPx.coerceAtLeast(dp(120f))
                panel.root.layoutParams = params
            }
        }
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

    // ------------------------------------------------------------------ tata letak

    private fun render() {
        content.removeAllViews()
        renderHeader()
        renderKeyboardCard()
        renderWindowCard()
        renderBottomCard()
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

        // Penboard menulis lewat keyboard yang aktif; layanan aksesibilitas tetap tersedia.
        val typing = PenboardTypingService.isEnabled(this)
        Ui.divider(card)
        Ui.statusText(card, getString(if (typing) R.string.status_typing_on else R.string.status_typing_off))
        if (!typing) {
            Ui.buttonRow(
                card,
                getString(R.string.action_typing_enable) to { openAccessibilitySettings() }
            )
        }
        Ui.body(card, getString(R.string.typing_note))
    }

    /** Buka halaman Google Play services di Play Store (untuk memperbaruinya). */
    private fun openPlayStoreForGms() {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"))
        try {
            startActivity(market)
        } catch (error: Throwable) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms")
                    )
                )
            } catch (inner: Throwable) {
                toast(getString(R.string.msg_engine_no_services))
            }
        }
    }

    /** Buka daftar layanan aksesibilitas agar "Penboard Tulis Otomatis" bisa dinyalakan. */
    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            toast(getString(R.string.msg_typing_settings))
        } catch (error: Throwable) {
            toast(getString(R.string.msg_typing_settings_failed))
        }
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
        val gms = InkRecognition.servicesStatus(this)
        val engineReady = InkRecognition.isServicesAvailable(this)
        Ui.statusText(card, getString(if (engineReady) R.string.check_engine_ok else R.string.check_engine_no))
        // Perangkat Huawei tanpa GMS / GMS versi lama: sebutkan keadaannya yang sebenarnya.
        if (gms != GmsStatus.READY) {
            Ui.statusText(card, getString(R.string.check_gms_detail, gms.summary))
            Ui.buttonRow(
                card,
                *buildList {
                    if (gms == GmsStatus.UPDATE_REQUIRED) {
                        add(getString(R.string.action_update_gms) to { openPlayStoreForGms() })
                    }
                    add(getString(R.string.action_open_handwriting_keyboard) to { showKeyboardPicker() })
                }.toTypedArray()
            )
        }
        val deviceInk = SystemHandwriting.pickBest(SystemHandwriting.candidates(this), packageName)
        Ui.statusText(
            card,
            if (deviceInk != null) {
                getString(R.string.check_device_ink_ok, deviceInk.label)
            } else {
                getString(R.string.check_device_ink_no)
            }
        )
        // Bahasa & model: penyebab paling umum tulisan "tidak terbaca".
        if (engineReady) {
            val language = RecognitionLanguages.label(store.config.recognitionLanguage)
            val modelLine = Ui.statusText(card, getString(R.string.status_model_checking))
            InkRecognition.modelStatus(store.config.recognitionLanguage) { status ->
                val text = getString(
                    when (status) {
                        ModelStatus.READY -> R.string.check_model_ready
                        ModelStatus.NOT_DOWNLOADED -> R.string.check_model_not_downloaded
                        else -> R.string.check_model_unknown
                    },
                    language
                )
                modelStatusText = text
                modelLine.text = text
            }
        }
        // Pesan terakhir mesin: membantu menelusuri bila pengenalan gagal.
        val engineMessage = InkRecognition.lastErrorMessage()
        Ui.statusText(
            card,
            if (engineMessage.isNullOrBlank()) {
                getString(R.string.diag_engine_none)
            } else {
                getString(R.string.diag_engine_message, engineMessage.take(160))
            }
        )
        val candidates = InkRecognition.lastCandidateTexts()
        Ui.statusText(
            card,
            if (candidates.isEmpty()) {
                getString(R.string.diag_candidates_none)
            } else {
                getString(R.string.diag_candidates, candidates.joinToString(", "))
            }
        )
        Ui.buttonRow(card, getString(R.string.action_copy_report) to { copyEngineReport(engineReady) })
    }

    /** Salin laporan mesin pengenalan ke papan klip supaya mudah diperiksa. */
    private fun copyEngineReport(engineReady: Boolean) {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (error: Throwable) {
            "?"
        }
        val report = InkReport.lines(
            appVersion = version,
            engineAvailable = engineReady,
            language = RecognitionLanguages.label(store.config.recognitionLanguage),
            modelStatus = modelStatusText,
            message = InkRecognition.lastErrorMessage(),
            candidates = InkRecognition.lastCandidateTexts(),
            ink = InkRecognition.lastInkSummary()
        )
        val copied = try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.app_name), report))
            true
        } catch (error: Throwable) {
            false
        }
        toast(getString(if (copied) R.string.msg_report_copied else R.string.msg_report_failed))
    }

    /** Satu langkah panduan dengan tanda selesai/belum. */
    private fun step(parent: LinearLayout, text: String, done: Boolean) {
        Ui.statusText(parent, getString(if (done) R.string.step_done else R.string.step_todo) + " " + text)
    }

    private fun renderPreviewCard() {
        val card = Ui.card(
            content,
            getString(R.string.card_preview),
            getString(R.string.card_preview_sub, appVersion())
        )
        val panel = previewPanel ?: BoardPanel(this).also { previewPanel = it }
        detach(panel.root)
        panel.listener = object : BoardPanel.Listener {
            override fun onKey(key: KeyDef, action: KeyAction) {
                if (applyPreviewEdit(action, key.text)) return
                toast(getString(R.string.preview_key_tap, store.config.labelFor(key)))
            }

            override fun onStrokeStarted() = Unit
            override fun onStrokeCommitted() = Unit
            override fun onPenButton(primary: Boolean) = Unit
            override fun onContentChanged() = Unit
            override fun onMinimize() {
                // Di keyboard sungguhan papan benar-benar ditutup; di pratinjau hanya diumumkan.
                toast(getString(R.string.msg_minimize_preview))
            }

            // Di pratinjau tarikan hanya memindah/mengubah ukuran gambarnya; angka yang
            // tersimpan diatur dari kartu pengaturan di bawahnya.
            override fun onMoveBy(dxPx: Int, dyPx: Int) {
                // Di pratinjau tingginya sudah ditentukan kartu, jadi hanya bisa digeser
                // ke kiri/kanan (di keyboard sungguhan papan juga bisa diangkat ke atas).
                panel.placePanel(panel.panelLeftPx + dxPx, panel.panelLiftPx)
            }

            override fun onResizeBy(dxPx: Int, dyPx: Int) = resizeFromPreview(dxPx, dyPx)

            override fun onGestureFinished() = Unit
        }
        // Kartu ini memakai ukuran yang **sama persis** dengan keyboard sungguhan:
        // semuanya dihitung [BoardPanel.applyConfig], jadi tidak ada angka kembar yang
        // bisa berbeda dari tampilan aslinya.
        panel.applyConfig(store.config)
        panel.placePanel(0, 0)
        card.addView(
            panel.root,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                panel.panelHeightPx.coerceAtLeast(dp(120f))
            )
        )

        // Bukti terukur: tombol harus persegi, dan tinggi kolom tombol harus sama dengan
        // tinggi area tulis supaya keduanya sejajar di atas maupun di bawah.
        val square = Ui.statusText(card, getString(R.string.preview_square_wait))
        val align = Ui.statusText(card, getString(R.string.preview_align_wait))
        val bottom = Ui.statusText(card, getString(R.string.preview_bottom_wait))
        Ui.body(card, getString(R.string.preview_cell_style, store.config.iconStyle.label))
        Ui.body(
            card,
            if (store.config.lineOffsetEnabled) {
                getString(R.string.preview_line_on, store.config.effectiveGridRows)
            } else {
                getString(R.string.preview_line_off)
            }
        )
        panel.root.post { updatePreviewStatus(square, align, bottom, panel) }
    }

    /** Ubah ukuran papan dari tarikan sudut di pratinjau (lebar dan tinggi). */
    private fun resizeFromPreview(dxPx: Int, dyPx: Int) {
        val width = resources.displayMetrics.widthPixels
        if (width <= 0) return
        val config = store.config
        val density = resources.displayMetrics.density
        var next = config
        if (dxPx != 0) {
            val fraction = (width * config.panelWidthFraction + dxPx).toFloat() / width
            next = next.copy(
                panelWidthFraction = fraction.coerceIn(PenboardConfig.MIN_PANEL_WIDTH_FRACTION, 1f)
            )
        }
        if (dyPx != 0) {
            pendingResizeDp += dyPx / density
            val delta = pendingResizeDp.toInt()
            if (delta != 0) {
                pendingResizeDp -= delta
                next = if (next.gridCellDp > 0) {
                    next.copy(
                        gridCellDp = (next.gridCellDp + delta).coerceIn(
                            PenboardConfig.MIN_CELL_DP,
                            PenboardConfig.MAX_CELL_DP
                        )
                    )
                } else {
                    // Tinggi papan = baris x jarak baris, jadi tarikan vertikal
                    // mengubah jarak baris (dan ukuran tombolnya ikut menyesuaikan).
                    next.copy(
                        lineGapDp = (next.lineGapDp + delta).coerceIn(
                            PenboardConfig.MIN_LINE_GAP_DP,
                            PenboardConfig.MAX_LINE_GAP_DP
                        )
                    )
                }
            }
        }
        if (next != config) store.update { next }
    }

    /** Tampilkan hasil pengukuran pratinjau: tombol persegi & kolom tombol sejajar. */
    private fun updatePreviewStatus(
        square: TextView,
        align: TextView,
        bottom: TextView,
        panel: BoardPanel
    ) {
        val cell = panel.cellPx
        val columns = store.config.gridColumns
        val rows = store.config.effectiveGridRows
        square.text = if (cell > 0) {
            getString(R.string.preview_square_ok, cell, columns, rows)
        } else {
            getString(R.string.preview_square_bad)
        }
        val (gridRows, boardRows) = panel.measuredRows()
        align.text = when {
            // Diatur sendiri: jumlahnya memang boleh berbeda, yang penting rata atas.
            !store.config.gridRowsFollow && gridRows > 0 ->
                getString(R.string.preview_align_manual, gridRows, boardRows)
            gridRows > 0 && gridRows == boardRows ->
                getString(R.string.preview_align_ok, gridRows, boardRows)
            else -> getString(R.string.preview_align_bad, gridRows, boardRows)
        }
        // Bilah atas selalu satu baris: beri tahu bila ada tombol yang tidak muat.
        val shown = panel.bottomShownCount
        val requested = panel.bottomRequestedCount
        bottom.text = when {
            requested <= 0 -> getString(R.string.preview_bottom_none)
            shown >= requested -> getString(R.string.preview_bottom_ok, shown)
            else -> getString(R.string.preview_bottom_partial, shown, requested)
        }
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
        Ui.switchRow(
            card,
            getString(R.string.setting_line_offset),
            getString(R.string.setting_line_offset_sub),
            config.lineOffsetEnabled
        ) { value -> store.update { it.copy(lineOffsetEnabled = value) } }
        Ui.body(card, getString(R.string.line_offset_note))
        Ui.sliderRow(
            card, getString(R.string.setting_board_lines), config.boardLines,
            PenboardConfig.MIN_BOARD_LINES, PenboardConfig.MAX_BOARD_LINES, getString(R.string.unit_rows)
        ) { value -> store.update { it.copy(boardLines = value) } }
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
        val columns = GridMath.columnsOr(config.gridColumns)
        // Baris tombol samping: mengikuti area tulis (bawaan) atau diatur sendiri.
        val rows = GridMath.rowsOr(config.effectiveGridRows)

        Ui.sliderRow(
            card, getString(R.string.setting_grid_columns), columns,
            PenboardConfig.MIN_GRID_COLUMNS, PenboardConfig.MAX_GRID_COLUMNS, getString(R.string.unit_columns)
        ) { value ->
            // Bila sebelumnya semua sel terpakai, kolom baru langsung terisi; kalau
            // pengguna memang membatasi jumlah tombol, batas itu yang dipertahankan.
            // Mengurangi kolom memangkas tombol yang tidak punya tempat lagi.
            store.update {
                val full = it.gridButtonCount >= GridMath.slots(columns, rows)
                val slots = GridMath.slots(value, rows)
                it.copy(
                    gridColumns = value,
                    gridButtonCount = if (full) slots else it.gridButtonCount.coerceIn(1, slots)
                )
            }
        }
        // Baris tombol: ikut area tulis (bawaan) atau diatur sendiri 1-6.
        Ui.switchRow(
            card,
            getString(R.string.setting_grid_rows_follow),
            getString(R.string.setting_grid_rows_follow_sub),
            config.gridRowsFollow
        ) { value ->
            store.update {
                val nextRows = if (value) it.boardLines else it.effectiveGridRows
                it.copy(
                    gridRowsFollow = value,
                    // Simpan jumlah yang sedang tampil saat beralih ke pengaturan sendiri.
                    gridRows = nextRows,
                    gridButtonCount = it.gridButtonCount
                        .coerceAtMost(GridMath.slots(it.gridColumns, nextRows))
                )
            }
            render()
        }
        if (config.gridRowsFollow) {
            Ui.body(card, getString(R.string.grid_rows_follow, rows))
        } else {
            Ui.sliderRow(
                card,
                getString(R.string.setting_grid_rows),
                config.gridRows,
                PenboardConfig.MIN_GRID_ROWS,
                PenboardConfig.MAX_GRID_ROWS,
                getString(R.string.unit_rows)
            ) { value ->
                store.update {
                    // Bila sebelumnya semua sel terpakai, baris baru langsung terisi;
                    // kalau pengguna membatasi jumlah tombol, batas itu dipertahankan.
                    val oldSlots = GridMath.slots(it.gridColumns, it.effectiveGridRows)
                    val full = it.gridButtonCount >= oldSlots
                    val slots = GridMath.slots(it.gridColumns, value)
                    it.copy(
                        gridRows = value,
                        gridButtonCount = if (full) slots else it.gridButtonCount.coerceAtMost(slots)
                    )
                }
            }
            Ui.body(card, getString(R.string.grid_rows_manual, rows))
        }
        Ui.sliderRow(
            card, getString(R.string.setting_grid_count), config.gridButtonCount,
            PenboardConfig.MIN_GRID_KEYS, GridMath.slots(columns, rows), getString(R.string.unit_keys)
        ) { value -> store.update { it.copy(gridButtonCount = value) } }

        // Ukuran tombol: otomatis (mengikuti tinggi papan) atau diatur sendiri.
        Ui.switchRow(
            card,
            getString(R.string.setting_cell_auto),
            getString(R.string.setting_cell_auto_sub),
            config.gridCellDp <= 0
        ) { automatic ->
            store.update {
                it.copy(gridCellDp = if (automatic) 0 else (autoCellFallbackDp(it)))
            }
        }
        if (config.gridCellDp > 0) {
            Ui.sliderRow(
                card, getString(R.string.setting_cell_size), config.gridCellDp,
                PenboardConfig.MIN_CELL_DP, PenboardConfig.MAX_CELL_DP, getString(R.string.unit_dp)
            ) { value -> store.update { it.copy(gridCellDp = value) } }
        }
        Ui.body(card, getString(R.string.grid_note))
        Ui.body(card, getString(R.string.char_function_note))

        Ui.divider(card)
        for (index in 0 until GridMath.slots(columns, rows)) {
            val key = config.gridKeys.getOrNull(index)
                ?: PenboardConfig.defaultGridKeys().getOrElse(index) { KeyDef() }
            val enabled = index < config.gridButtonCount
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(5f), 0, dp(5f))
            }
            row.addView(
                AppCompatTextView(this).apply {
                    text = if (enabled) {
                        getString(R.string.grid_slot_label, index + 1, config.labelFor(key))
                    } else {
                        getString(R.string.bottom_slot_hidden, index + 1, config.labelFor(key))
                    }
                    textSize = 14f
                    alpha = if (enabled) 1f else 0.55f
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            row.addView(
                MaterialButton(this).apply {
                    text = getString(R.string.action_change)
                    isAllCaps = false
                    textSize = 12.5f
                    // Semua sel bisa diatur; menyimpan sel baru menaikkan jumlah tombol.
                    setOnClickListener { showKeyEditor(index) }
                }
            )
            card.addView(row)
        }
        Ui.divider(card)
        Ui.outlinedButton(card, getString(R.string.action_grid_default)) {
            store.update {
                val lines = GridMath.DEFAULT_LINES
                it.copy(
                    gridKeys = PenboardConfig.defaultGridKeys(),
                    gridColumns = PenboardConfig.COLUMNS,
                    gridRows = lines,
                    gridCellDp = 0,
                    gridButtonCount = lines * PenboardConfig.COLUMNS
                )
            }
            render()
        }
    }

    /**
     * Ukuran tombol yang dipakai saat pengguna mematikan mode otomatis: sisi tombol
     * terakhir yang benar-benar dihitung papan (atau nilai tengah bila belum diukur).
     */
    private fun autoCellFallbackDp(config: PenboardConfig): Int {
        val measured = previewPanel?.cellPx ?: 0
        if (measured > 0) {
            return (measured / resources.displayMetrics.density).roundToInt()
                .coerceIn(PenboardConfig.MIN_CELL_DP, PenboardConfig.MAX_CELL_DP)
        }
        val screen = resources.displayMetrics.widthPixels
        val spacing = (GridMath.SPACING_DP * resources.displayMetrics.density).toInt()
        val usable = screen - 2 * dp(3f)
        val boardWidth = GridMath.boardWidthFor(
            usableWidthPx = usable,
            fraction = config.boardWidthFraction,
            minBoardWidthPx = dp(72f),
            gridMinWidthPx = GridMath.gridWidthPx(
                GridMath.columnsOr(config.gridColumns),
                (GridMath.MIN_TOUCH_DP * resources.displayMetrics.density).toInt(),
                spacing
            ),
            spacingPx = spacing
        )
        val cell = GridMath.cellSizePx(
            boardHeightPx = GridMath.boardHeightForLines(
                config.boardLines,
                (config.lineGapDp * resources.displayMetrics.density).toInt()
            ),
            availableWidthPx = usable,
            spacingPx = spacing,
            minCellPx = (GridMath.MIN_TOUCH_DP * resources.displayMetrics.density).toInt(),
            // Ukuran otomatis = tinggi satu baris tulisan, bukan dibagi baris tombol.
            rows = GridMath.linesOr(config.boardLines),
            columns = GridMath.columnsOr(config.gridColumns),
            minBoardWidthPx = boardWidth
        )
        return (cell / resources.displayMetrics.density).roundToInt()
            .coerceIn(PenboardConfig.MIN_CELL_DP, PenboardConfig.MAX_CELL_DP)
    }

    /**
     * Bingkai jendela: garis tepi + bilah judul dengan tombol **minimize** yang
     * menyembunyikan papan (keyboard ditutup).
     */
    private fun renderWindowCard() {
        val config = store.config
        val card = Ui.card(content, getString(R.string.card_window), getString(R.string.card_window_sub))
        Ui.switchRow(
            card,
            getString(R.string.setting_window_frame),
            getString(R.string.setting_window_frame_sub),
            config.windowFrame
        ) { value -> store.update { it.copy(windowFrame = value) } }
        Ui.switchRow(
            card,
            getString(R.string.setting_minimize),
            getString(R.string.setting_minimize_sub),
            config.minimizeButton
        ) { value -> store.update { it.copy(minimizeButton = value) } }
        Ui.textRow(
            card,
            getString(R.string.setting_window_title),
            config.windowTitle,
            getString(R.string.setting_window_title_hint)
        ) { value -> store.update { it.copy(windowTitle = value) } }
        Ui.divider(card)
        Ui.switchRow(
            card,
            getString(R.string.setting_panel_floating),
            getString(R.string.setting_panel_floating_sub),
            config.floatingPanel
        ) { value ->
            store.update { it.copy(floatingPanel = value, panelOffsetX = 0f, panelOffsetY = 0f) }
            render()
        }
        Ui.sliderRow(
            card,
            getString(R.string.setting_panel_width),
            (config.panelWidthFraction * 100).roundToInt(),
            (PenboardConfig.MIN_PANEL_WIDTH_FRACTION * 100).roundToInt(),
            100,
            getString(R.string.unit_percent)
        ) { value -> store.update { it.copy(panelWidthFraction = value / 100f) } }
        // Lebar area tulis: kolom tombol mengambil sisa ruangnya, jadi menaikkan angka
        // ini membuat area menulis lebih lebar dan tombol ikut mengecil (tetap utuh).
        Ui.sliderRow(
            card,
            getString(R.string.setting_board_width),
            (config.boardWidthFraction * 100).roundToInt(),
            (PenboardConfig.MIN_BOARD_WIDTH_FRACTION * 100).roundToInt(),
            (PenboardConfig.MAX_BOARD_WIDTH_FRACTION * 100).roundToInt(),
            getString(R.string.unit_percent)
        ) { value -> store.update { it.copy(boardWidthFraction = value / 100f) } }
        Ui.body(card, getString(R.string.board_width_note))
        Ui.body(card, getString(R.string.window_drag_note))
        Ui.body(card, getString(R.string.window_note))
    }

    /**
     * Bilah tombol **di atas papan tulis**: jumlah dan isi tombolnya diatur pengguna,
     * ikonnya pixel art (`util/PixelArt.kt`).
     */
    private fun renderBottomCard() {
        val config = store.config
        val card = Ui.card(content, getString(R.string.card_bottom), getString(R.string.card_bottom_sub))
        Ui.switchRow(
            card,
            getString(R.string.setting_bottom_show),
            getString(R.string.setting_bottom_show_sub),
            config.bottomButtonCount > 0
        ) { value ->
            store.update {
                it.copy(bottomButtonCount = if (value) PenboardConfig.DEFAULT_BOTTOM_KEYS else 0)
            }
            render()
        }
        Ui.sliderRow(
            card,
            getString(R.string.setting_bottom_count),
            config.bottomButtonCount,
            PenboardConfig.MIN_BOTTOM_KEYS,
            PenboardConfig.MAX_BOTTOM_KEYS,
            getString(R.string.unit_keys)
        ) { value -> store.update { it.copy(bottomButtonCount = value) } }
        Ui.body(card, getString(R.string.bottom_single_row_note))

        Ui.divider(card)
        // SEMUA slot bisa diatur kapan saja, termasuk yang belum tampil di papan:
        // pengguna sering ingin menyiapkan tombol ke-6 dst. sebelum menaikkan jumlahnya.
        // Slot yang belum tampil hanya diredupkan, bukan dimatikan.
        for (index in 0 until PenboardConfig.BOTTOM_SLOTS) {
            val key = config.bottomKeys.getOrNull(index)
                ?: PenboardConfig.defaultBottomKeys().getOrElse(index) { KeyDef() }
            val shown = index < config.bottomButtonCount
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4f), 0, dp(4f))
            }
            row.addView(
                AppCompatTextView(this).apply {
                    text = if (shown) {
                        getString(R.string.grid_slot_label, index + 1, config.labelFor(key))
                    } else {
                        getString(R.string.bottom_slot_hidden, index + 1, config.labelFor(key))
                    }
                    textSize = 14f
                    alpha = if (shown) 1f else 0.55f
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            row.addView(
                MaterialButton(this).apply {
                    text = getString(R.string.action_change)
                    isAllCaps = false
                    textSize = 12.5f
                    setOnClickListener { showBottomKeyEditor(index) }
                }
            )
            card.addView(row)
        }
        Ui.body(card, getString(R.string.bottom_edit_all_note))
        Ui.divider(card)
        Ui.outlinedButton(card, getString(R.string.action_bottom_default)) {
            store.update {
                it.copy(
                    bottomKeys = PenboardConfig.defaultBottomKeys(),
                    bottomButtonCount = PenboardConfig.DEFAULT_BOTTOM_KEYS
                )
            }
            render()
        }
        Ui.body(card, getString(R.string.bottom_note))
        Ui.body(card, getString(R.string.char_function_note))
        Ui.outlinedButton(card, getString(R.string.action_add_char_pad)) { addCharPadButton() }
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

    /** Pintasan: satu ketukan menyetel semuanya seperti HiWrite. */
    private fun renderPresetCard() {
        val card = Ui.card(content, getString(R.string.card_preset), getString(R.string.card_preset_sub))
        Ui.outlinedButton(card, getString(R.string.action_preset_huawei)) {
            store.update { it.withHiWritePreset() }
            render()
        }
        Ui.body(card, getString(R.string.preset_note))
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
        val iconStyles = IconStyle.entries
        Ui.spinnerRow(
            card,
            getString(R.string.setting_icon_style),
            iconStyles.map { it.label },
            iconStyles.indexOf(config.iconStyle).coerceAtLeast(0)
        ) { index ->
            store.update { it.copy(iconStyle = iconStyles[index]) }
            render()
        }
        Ui.body(card, getString(R.string.setting_icon_style_sub))
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
        val huawei = isHuaweiFamily()
        val gms = InkRecognition.servicesStatus(this)
        // Perangkat Huawei tanpa/bermasalah Google Play services: mesin bawaan
        // Penboard memang tidak bisa dipakai, jadi panduannya harus jelas.
        if (!huawei && gms == GmsStatus.READY) return
        val card = Ui.card(content, getString(R.string.card_huawei), getString(R.string.card_huawei_sub))
        if (gms != GmsStatus.READY) {
            Ui.statusText(card, getString(R.string.check_gms_detail, gms.summary))
            Ui.body(card, getString(R.string.msg_engine_no_services))
        }
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

    // ------------------------------------------------------------------ tombol bawah

    private fun showBottomKeyEditor(index: Int) {
        val config = store.config
        val current = config.bottomKeys.getOrNull(index)
            ?: PenboardConfig.defaultBottomKeys().getOrElse(index) { KeyDef() }
        var action = current.action
        var repeat = current.repeatOnHold

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(8f), dp(20f), dp(4f))
        }
        val actions = KeyAction.entries.toList()
        val spinner = Ui.spinnerRow(
            body,
            getString(R.string.setting_action),
            actions.map { "${it.label} — ${actionHint(it)}" },
            actions.indexOf(action).coerceAtLeast(0)
        ) { position -> action = actions[position] }
        val textField = addCharacterEditor(body, current, actions, spinner) { action = it }

        val labelField = EditText(this).apply {
            hint = getString(R.string.setting_custom_label)
            setText(current.label)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        body.addView(labelField)
        Ui.switchRow(
            body, getString(R.string.setting_repeat), getString(R.string.setting_repeat_sub), current.repeatOnHold
        ) { value -> repeat = value }

        // Tinggi tombol bawah selalu mengikuti tombol samping; lebarnya diatur di sini.
        var widthMultiplier = current.widthMultiplier
        var heightMultiplier = current.heightMultiplier
        Ui.body(body, getString(R.string.setting_bottom_width_sub))
        Ui.sliderRow(
            body,
            getString(R.string.setting_bottom_width),
            (current.widthMultiplier * 100).roundToInt(),
            (KeyDef.MIN_WIDTH_MULTIPLIER * 100).roundToInt(),
            (KeyDef.MAX_WIDTH_MULTIPLIER * 100).roundToInt(),
            getString(R.string.unit_percent)
        ) { value -> widthMultiplier = value / 100f }
        Ui.sliderRow(
            body,
            getString(R.string.setting_bottom_height),
            (current.heightMultiplier * 100).roundToInt(),
            (KeyDef.MIN_HEIGHT_MULTIPLIER * 100).roundToInt(),
            (KeyDef.MAX_HEIGHT_MULTIPLIER * 100).roundToInt(),
            getString(R.string.unit_percent)
        ) { value -> heightMultiplier = value / 100f }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.bottom_slot_title, index + 1))
            .setView(dialogScroll(body))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val updated = current.copy(
                    action = action,
                    label = labelField.text.toString().trim(),
                    text = KeyboardChars.clamp(textField.text.toString()),
                    repeatOnHold = repeat,
                    widthMultiplier = widthMultiplier,
                    heightMultiplier = heightMultiplier
                )
                store.update { cfg ->
                    val keys = cfg.bottomKeys.toMutableList()
                    // Jaga-jaga bila daftar lebih pendek dari jumlah slot.
                    while (keys.size <= index) keys.add(KeyDef())
                    keys[index] = updated
                    cfg.copy(
                        bottomKeys = keys,
                        // Slot yang baru diatur langsung ditampilkan supaya hasilnya terlihat.
                        bottomButtonCount = maxOf(cfg.bottomButtonCount, index + 1)
                    )
                }
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        val spinner = Ui.spinnerRow(
            body,
            getString(R.string.setting_action),
            actions.map { "${it.label} — ${actionHint(it)}" },
            actions.indexOf(action).coerceAtLeast(0)
        ) { position -> action = actions[position] }
        val textField = addCharacterEditor(body, current, actions, spinner) { action = it }

        val labelField = EditText(this).apply {
            hint = getString(R.string.setting_custom_label)
            setText(current.label)
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        body.addView(labelField)
        Ui.switchRow(
            body, getString(R.string.setting_repeat), getString(R.string.setting_repeat_sub), current.repeatOnHold
        ) { value -> repeat = value }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.grid_slot_title, index + 1))
            .setView(dialogScroll(body))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val updated = current.copy(
                    action = action,
                    label = labelField.text.toString().trim(),
                    text = KeyboardChars.clamp(textField.text.toString()),
                    repeatOnHold = repeat
                )
                store.update { cfg ->
                    val keys = cfg.gridKeys.toMutableList()
                    // Jaga-jaga bila daftar lebih pendek dari jumlah sel.
                    while (keys.size <= index) keys.add(KeyDef())
                    keys[index] = updated
                    cfg.copy(
                        gridKeys = keys,
                        // Sel yang baru diatur langsung ditampilkan.
                        gridButtonCount = maxOf(cfg.gridButtonCount, index + 1)
                    )
                }
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Pasang satu tombol Papan karakter pada slot atas berikutnya yang belum tampil,
     * tanpa menimpa tombol yang sudah ada. Kalau sudah ada, cukup pastikan slotnya tampil.
     */
    private fun addCharPadButton() {
        val shown = previewPanel?.bottomShownCount ?: store.config.bottomButtonCount
        val existing = store.config.bottomKeys.indexOfFirst { it.action == KeyAction.CHAR_PAD }
        // Sudah tampil di bilah: tidak perlu dipasang lagi.
        if (existing in 0 until shown) {
            toast(getString(R.string.msg_char_pad_exists))
            return
        }
        store.update { cfg ->
            val keys = cfg.bottomKeys.toMutableList()
            while (keys.size < PenboardConfig.BOTTOM_SLOTS) keys.add(KeyDef())
            val alreadyCounted = existing in 0 until cfg.bottomButtonCount
            if (existing >= 0) {
                keys.removeAt(existing)
                keys.add(KeyDef())
            }
            // Di kiri: tombol pertama selalu terlihat, walau bilahnya sudah penuh.
            keys.add(0, KeyDef(action = KeyAction.CHAR_PAD))
            val nextCount = if (alreadyCounted) cfg.bottomButtonCount else cfg.bottomButtonCount + 1
            cfg.copy(
                bottomKeys = keys.take(PenboardConfig.BOTTOM_SLOTS),
                bottomButtonCount = nextCount.coerceIn(1, PenboardConfig.BOTTOM_SLOTS)
            )
        }
        toast(getString(R.string.msg_char_pad_added))
        render()
    }

    /**
     * Kotak karakter + pemilih angka/huruf/tanda baca.
     * Memilih atau mengetik karakter mengalihkan fungsi tombol ke Karakter.
     */
    private fun addCharacterEditor(
        body: LinearLayout,
        current: KeyDef,
        actions: List<KeyAction>,
        spinner: Spinner,
        setAction: (KeyAction) -> Unit
    ): EditText {
        Ui.body(body, getString(R.string.char_picker_note))
        val textField = EditText(this).apply {
            hint = getString(R.string.setting_custom_text)
            setText(KeyboardChars.clamp(current.text))
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(KeyboardChars.MAX_LENGTH))
        }
        body.addView(textField)
        fun useTextAction() {
            setAction(KeyAction.TEXT)
            val index = actions.indexOf(KeyAction.TEXT)
            if (index >= 0 && spinner.selectedItemPosition != index) spinner.setSelection(index)
        }
        textField.doAfterTextChanged { value ->
            if (!value.isNullOrEmpty()) useTextAction()
        }
        CharPicker.addRows(body) { character ->
            textField.setText(character)
            textField.setSelection(character.length)
            useTextAction()
        }
        return textField
    }

    /** Dialog editor tombol cukup tinggi; isinya digulir supaya tombol OK tetap terlihat. */
    private fun dialogScroll(body: LinearLayout): ScrollView =
        ScrollView(this).apply {
            isFillViewport = false
            addView(
                body,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.62f).toInt()
            )
        }

    /**
     * Pratinjau: tombol karakter (dan hapus/spasi/panah) benar-benar mengubah kolom uji,
     * supaya papan karakter bisa dicoba tanpa berpindah aplikasi.
     */
    private fun applyPreviewEdit(action: KeyAction, text: String): Boolean {
        val editable = testField.text ?: return false
        val existing = editable.toString()
        var start = testField.selectionStart
        var end = testField.selectionEnd
        if (start < 0) start = existing.length
        if (end < 0) end = existing.length
        if (end < start) {
            val swap = start
            start = end
            end = swap
        }
        val result = when (action) {
            KeyAction.TEXT -> {
                val insert = KeyboardChars.clamp(text)
                if (insert.isEmpty()) return false
                TextInsert.into(existing, start, end, insert)
            }
            KeyAction.SPACE -> TextInsert.into(existing, start, end, " ")
            KeyAction.ENTER -> TextInsert.into(existing, start, end, "\n")
            KeyAction.TAB -> TextInsert.into(existing, start, end, "\t")
            KeyAction.BACKSPACE -> TextInsert.deleteBackward(existing, start, end)
            KeyAction.DELETE_FORWARD -> TextInsert.deleteForward(existing, start, end)
            KeyAction.DELETE_WORD -> TextInsert.deleteWord(existing, start, end)
            KeyAction.CURSOR_LEFT -> {
                testField.setSelection(TextInsert.moveCaret(existing, start, -1))
                return true
            }
            KeyAction.CURSOR_RIGHT -> {
                testField.setSelection(TextInsert.moveCaret(existing, start, 1))
                return true
            }
            else -> return false
        }
        testField.setText(result.text)
        testField.setSelection(result.caret.coerceIn(0, result.text.length))
        return true
    }

    private fun actionHint(action: KeyAction): String = getString(
        when (action) {
            KeyAction.TEXT -> R.string.hint_text
            KeyAction.CHAR_PAD -> R.string.hint_char_pad
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
            KeyAction.KEYBOARD_PICKER -> R.string.hint_keyboard_picker
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
            toast(getString(R.string.msg_handwriting_picker))
        } catch (error: Throwable) {
            toast(getString(R.string.msg_handwriting_picker_failed))
        }
    }

    private fun openKeyboardSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        } catch (error: Throwable) {
            toast(getString(R.string.msg_picker_failed))
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

}
