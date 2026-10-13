package dev.retrobit.penboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import dev.retrobit.penboard.board.KeyButton
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.PresetRepository
import dev.retrobit.penboard.data.RowDef
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.data.ToolbarSide
import dev.retrobit.penboard.databinding.ActivityBoardEditorBinding
import dev.retrobit.penboard.ui.Preview
import dev.retrobit.penboard.ui.Ui
import kotlin.math.roundToInt

/**
 * Editor papan: atur nama, tombol apa saja yang tampil, tata letak baris,
 * ukuran papan, transparansi, dan warna. Semua perubahan langsung tersimpan.
 */
class BoardEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoardEditorBinding
    private val repository by lazy { PresetRepository(this) }

    private lateinit var config: PenboardConfig
    private var selectedIsToolbar = true
    private var selectedRowIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra(EXTRA_CONFIG_ID)
        config = id?.let { repository.profile(it) } ?: repository.active()

        binding.backButton.setOnClickListener { finish() }
        binding.doneButton.setOnClickListener { finish() }

        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_keys))
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_look))
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val keysTab = tab.position == 0
                binding.scrollKeys.visibility = if (keysTab) View.VISIBLE else View.GONE
                binding.scrollLook.visibility = if (keysTab) View.GONE else View.VISIBLE
                if (keysTab) buildKeysTab() else buildLookTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        buildKeysTab()
        buildLookTab()
        applyPreview()
        updateTitle()
    }

    // ------------------------------------------------------------ simpan & pratinjau

    private fun update(mutator: (PenboardConfig) -> PenboardConfig) {
        config = mutator(config).sanitized()
        repository.upsert(config)
        applyPreview()
        updateTitle()
    }

    private fun updateTitle() {
        binding.editorTitle.text = getString(R.string.editor_title) + " · " + config.name
    }

    private fun applyPreview() {
        val density = resources.displayMetrics.density
        val maxHeight = (resources.displayMetrics.heightPixels * 0.40f).toInt()
        val height = (config.boardHeightDp * density).toInt().coerceAtMost(maxHeight)
        binding.previewBoard.layoutParams = binding.previewBoard.layoutParams.also { it.height = height }
        binding.previewBoard.cornerRadiusPx = Ui.dpF(this, 16f)
        binding.previewBoard.applyConfig(config)
        binding.previewBoard.tool = ToolMode.WRITE
        Preview.populate(binding.previewKeys, config, density, currentTool = ToolMode.WRITE)
    }

    private fun rowsOf(isToolbar: Boolean): List<RowDef> =
        if (isToolbar) config.toolbarRows else config.boardRows

    private fun setRows(isToolbar: Boolean, rows: List<RowDef>) {
        update { if (isToolbar) it.copy(toolbarRows = rows) else it.copy(boardRows = rows) }
    }

    // ------------------------------------------------------------ tab "Tombol"

    private fun buildKeysTab() {
        val parent = binding.contentKeys
        parent.removeAllViews()
        normalizeSelection()
        buildNameCard(parent)
        buildRowSelectorCard(parent)
        buildRowActionsCard(parent)
        buildKeysListCard(parent)
    }

    private fun normalizeSelection() {
        if (selectedIsToolbar && selectedRowIndex > config.toolbarRows.lastIndex) {
            selectedRowIndex = config.toolbarRows.lastIndex.coerceAtLeast(0)
        }
        if (!selectedIsToolbar && selectedRowIndex > config.boardRows.lastIndex) {
            selectedRowIndex = config.boardRows.lastIndex.coerceAtLeast(0)
        }
        if (selectedIsToolbar && config.toolbarRows.isEmpty()) selectedIsToolbar = false
        if (!selectedIsToolbar && config.boardRows.isEmpty()) selectedIsToolbar = true
    }

    private fun buildNameCard(parent: LinearLayout) {
        val card = Ui.card(parent, getString(R.string.lbl_profile_name))
        val input = EditText(this).apply {
            setText(config.name)
            setSelection(text.length)
            setSingleLine(true)
            hint = getString(R.string.lbl_profile_name)
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                config = config.copy(name = s?.toString().orEmpty())
                repository.upsert(config)
                updateTitle()
            }
        })
        card.addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun buildRowSelectorCard(parent: LinearLayout) {
        val card = Ui.card(parent, getString(R.string.lbl_row_selector))
        val labels = mutableListOf<String>()
        val refs = mutableListOf<Pair<Boolean, Int>>()
        config.toolbarRows.forEachIndexed { index, _ ->
            labels.add(getString(R.string.row_toolbar, index + 1))
            refs.add(true to index)
        }
        config.boardRows.forEachIndexed { index, _ ->
            labels.add(getString(R.string.row_board, index + 1))
            refs.add(false to index)
        }
        if (labels.isEmpty()) return

        var currentIndex = refs.indexOfFirst { it.first == selectedIsToolbar && it.second == selectedRowIndex }
        if (currentIndex < 0) currentIndex = 0
        selectedIsToolbar = refs[currentIndex].first
        selectedRowIndex = refs[currentIndex].second

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinner.setSelection(currentIndex, false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val ref = refs.getOrNull(position) ?: return
                if (ref.first != selectedIsToolbar || ref.second != selectedRowIndex) {
                    selectedIsToolbar = ref.first
                    selectedRowIndex = ref.second
                    buildKeysTab()
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) = Unit
        }
        card.addView(spinner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    private fun buildRowActionsCard(parent: LinearLayout) {
        val card = Ui.card(parent)
        Ui.buttonRow(
            card,
            getString(R.string.btn_move_up) to { moveRow(-1) },
            getString(R.string.btn_move_down) to { moveRow(1) }
        )
        Ui.buttonRow(
            card,
            getString(R.string.btn_add_row) to { addRow() },
            getString(R.string.btn_delete_row) to { deleteRow() }
        )
        Ui.fullButton(card, getString(R.string.btn_add_key)) { showKeyDialog(null) }
    }

    private fun moveRow(delta: Int) {
        val rows = rowsOf(selectedIsToolbar).toMutableList()
        val target = selectedRowIndex + delta
        if (target !in rows.indices) return
        val row = rows.removeAt(selectedRowIndex)
        rows.add(target, row)
        selectedRowIndex = target
        setRows(selectedIsToolbar, rows)
        buildKeysTab()
    }

    private fun addRow() {
        val rows = rowsOf(selectedIsToolbar).toMutableList()
        if (rows.size >= MAX_ROWS) return
        rows.add(RowDef(keys = listOf(KeyDef.action(KeyAction.SPACE, flex = 0.4f))))
        selectedRowIndex = rows.lastIndex
        setRows(selectedIsToolbar, rows)
        buildKeysTab()
    }

    private fun deleteRow() {
        val targetIsToolbar = selectedIsToolbar
        val rows = rowsOf(targetIsToolbar).toMutableList()
        val otherRows = if (targetIsToolbar) config.boardRows else config.toolbarRows
        if (rows.size <= 1 && otherRows.isEmpty()) {
            Toast.makeText(this, R.string.notice_min_row, Toast.LENGTH_SHORT).show()
            return
        }
        rows.removeAt(selectedRowIndex.coerceIn(0, rows.lastIndex))
        setRows(targetIsToolbar, rows)
        if (rows.isEmpty()) {
            selectedIsToolbar = !targetIsToolbar
            selectedRowIndex = 0
        } else {
            selectedRowIndex = selectedRowIndex.coerceAtMost(rows.lastIndex)
        }
        buildKeysTab()
    }

    private fun buildKeysListCard(parent: LinearLayout) {
        val rows = rowsOf(selectedIsToolbar)
        val row = rows.getOrNull(selectedRowIndex) ?: return
        val rowLabel = if (selectedIsToolbar) {
            getString(R.string.row_toolbar, selectedRowIndex + 1)
        } else {
            getString(R.string.row_board, selectedRowIndex + 1)
        }
        val card = Ui.card(parent, getString(R.string.lbl_row_keys) + " — " + rowLabel)

        if (row.keys.isEmpty()) {
            Ui.bodyText(card, getString(R.string.key_empty_row))
        }

        val density = resources.displayMetrics.density
        row.keys.forEachIndexed { index, key ->
            val holder = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                setPadding(0, Ui.dp(this@BoardEditorActivity, 8f), 0, Ui.dp(this@BoardEditorActivity, 8f))
            }
            holder.setOnClickListener { showKeyDialog(index) }

            val preview = KeyButton(this).apply {
                this.key = key
                applyPresentation(config, density, haptics = false)
                isEnabled = false
            }
            holder.addView(
                preview,
                LinearLayout.LayoutParams(
                    Ui.dp(this, 62f),
                    Ui.dp(this, config.keyHeightDp.toFloat() * row.height)
                )
            )

            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(Ui.dp(this@BoardEditorActivity, 12f), 0, 0, 0)
            }
            column.addView(TextView(this).apply { text = keyTitle(key); textSize = 15f })
            column.addView(TextView(this).apply {
                text = keySummary(key)
                textSize = 12f
                alpha = 0.65f
            })
            holder.addView(column, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

            val moveLeft = Ui.button(this, "◀", outlined = true).apply {
                setOnClickListener { moveKey(index, -1) }
            }
            val leftParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            leftParams.marginStart = Ui.dp(this, 6f)
            holder.addView(moveLeft, leftParams)

            val moveRight = Ui.button(this, "▶", outlined = true).apply {
                setOnClickListener { moveKey(index, 1) }
            }
            val rightParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            rightParams.marginStart = Ui.dp(this, 6f)
            holder.addView(moveRight, rightParams)

            card.addView(holder, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            Ui.divider(card)
        }
    }

    private fun keyTitle(key: KeyDef): String = if (key.action == KeyAction.INSERT) {
        getString(R.string.lbl_text) + ": “" + key.text + "”"
    } else {
        key.action.label
    }

    private fun keySummary(key: KeyDef): String {
        val builder = StringBuilder()
        builder.append(getString(R.string.lbl_width)).append(": ").append(key.width)
        if (key.flex > 0f) {
            builder.append(" · ").append(getString(R.string.lbl_flex)).append(": ")
                .append((key.flex * 100).roundToInt()).append("%")
        }
        if (key.repeat) builder.append(" · ").append(getString(R.string.lbl_repeat))
        return builder.toString()
    }

    private fun moveKey(index: Int, delta: Int) {
        val rows = rowsOf(selectedIsToolbar).toMutableList()
        val row = rows.getOrNull(selectedRowIndex) ?: return
        val target = index + delta
        if (target !in row.keys.indices) return
        val keys = row.keys.toMutableList()
        val key = keys.removeAt(index)
        keys.add(target, key)
        rows[selectedRowIndex] = row.copy(keys = keys)
        setRows(selectedIsToolbar, rows)
        buildKeysTab()
    }

    private fun showKeyDialog(keyIndex: Int?) {
        val rows = rowsOf(selectedIsToolbar).toMutableList()
        val row = rows.getOrNull(selectedRowIndex) ?: return
        val isNew = keyIndex == null
        val original = if (isNew) {
            KeyDef(action = KeyAction.INSERT, text = ".", width = 1f)
        } else {
            row.keys.getOrNull(keyIndex ?: return) ?: return
        }

        var action = original.action
        var width = original.width
        var flex = original.flex
        var repeat = original.repeat

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@BoardEditorActivity, 20f), Ui.dp(this@BoardEditorActivity, 8f), Ui.dp(this@BoardEditorActivity, 20f), 0)
        }

        container.addView(TextView(this).apply { text = getString(R.string.lbl_action); textSize = 14f; alpha = 0.7f })

        val actions = KeyAction.entries
        val actionSpinner = Spinner(this)
        actionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions.map { it.label })
        actionSpinner.setSelection(actions.indexOf(original.action).coerceAtLeast(0), false)
        container.addView(actionSpinner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        container.addView(TextView(this).apply {
            text = getString(R.string.lbl_text)
            textSize = 14f
            alpha = 0.7f
            setPadding(0, Ui.dp(this@BoardEditorActivity, 10f), 0, 0)
        })
        val textInput = EditText(this).apply {
            setText(original.text)
            hint = getString(R.string.lbl_text_hint)
            isEnabled = original.action == KeyAction.INSERT
        }
        container.addView(textInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        actionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                action = actions[position]
                textInput.isEnabled = action == KeyAction.INSERT
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) = Unit
        }

        Ui.sliderRow(
            container,
            getString(R.string.lbl_width),
            (original.width * 10).roundToInt(),
            2,
            60,
            "×0,1"
        ) { width = it / 10f }

        Ui.sliderRow(
            container,
            getString(R.string.lbl_flex),
            (original.flex * 100).roundToInt(),
            0,
            85,
            "%"
        ) { flex = it / 100f }

        Ui.switchRow(container, getString(R.string.lbl_repeat), checked = original.repeat) { repeat = it }

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.key_dialog_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newKey = original.copy(
                    action = action,
                    text = textInput.text.toString(),
                    width = width,
                    flex = flex,
                    repeat = repeat
                )
                val keys = row.keys.toMutableList()
                if (isNew) {
                    keys.add(newKey)
                } else {
                    keys[keyIndex ?: 0] = newKey
                }
                rows[selectedRowIndex] = row.copy(keys = keys)
                setRows(selectedIsToolbar, rows)
                buildKeysTab()
            }
            .setNegativeButton(android.R.string.cancel, null)

        if (!isNew) {
            builder.setNeutralButton(R.string.key_dialog_delete) { _, _ ->
                val keys = row.keys.toMutableList()
                keys.removeAt(keyIndex ?: 0)
                rows[selectedRowIndex] = row.copy(keys = keys)
                setRows(selectedIsToolbar, rows)
                buildKeysTab()
            }
        }
        builder.show()
    }

    // ------------------------------------------------------------ tab "Tampilan"

    private fun buildLookTab() {
        val parent = binding.contentLook
        parent.removeAllViews()

        val size = Ui.card(parent, getString(R.string.sec_size))
        Ui.sliderRow(size, getString(R.string.size_board_height), config.boardHeightDp, 140, 600, "dp") { value ->
            update { it.copy(boardHeightDp = value) }
        }
        Ui.sliderRow(size, getString(R.string.size_line_gap), config.lineGapDp, 22, 96, "dp") { value ->
            update { it.copy(lineGapDp = value) }
        }
        Ui.sliderRow(size, getString(R.string.size_key_height), config.keyHeightDp, 30, 96, "dp") { value ->
            update { it.copy(keyHeightDp = value) }
        }
        Ui.sliderRow(size, getString(R.string.size_key_corner), config.keyCornerDp, 0, 28, "dp") { value ->
            update { it.copy(keyCornerDp = value) }
        }
        Ui.sliderRow(size, getString(R.string.size_key_text), config.keyTextSizeSp, 10, 30, "sp") { value ->
            update { it.copy(keyTextSizeSp = value) }
        }
        Ui.sliderRow(size, getString(R.string.size_stroke), config.strokeWidthDp.roundToInt(), 1, 16, "dp") { value ->
            update { it.copy(strokeWidthDp = value.toFloat()) }
        }
        Ui.sliderRow(size, getString(R.string.size_eraser), config.eraserWidthDp.roundToInt(), 8, 80, "dp") { value ->
            update { it.copy(eraserWidthDp = value.toFloat()) }
        }

        val opacity = Ui.card(parent, getString(R.string.sec_opacity))
        Ui.sliderRow(opacity, getString(R.string.opacity_panel), config.panelOpacity, 0, 100, "%") { value ->
            update { it.copy(panelOpacity = value) }
        }
        Ui.sliderRow(opacity, getString(R.string.opacity_board), config.boardBgOpacity, 0, 100, "%") { value ->
            update { it.copy(boardBgOpacity = value) }
        }
        Ui.sliderRow(opacity, getString(R.string.opacity_grid), config.gridOpacity, 0, 100, "%") { value ->
            update { it.copy(gridOpacity = value) }
        }
        Ui.sliderRow(opacity, getString(R.string.opacity_keys), config.keyBgOpacity, 0, 100, "%") { value ->
            update { it.copy(keyBgOpacity = value) }
        }

        val colors = Ui.card(parent, getString(R.string.sec_colors))
        Ui.colorRow(colors, getString(R.string.color_panel), config.panelColor) { value ->
            update { it.copy(panelColor = value) }
        }
        Ui.colorRow(colors, getString(R.string.color_board), config.boardBgColor) { value ->
            update { it.copy(boardBgColor = value) }
        }
        Ui.colorRow(colors, getString(R.string.color_grid), config.gridColor) { value ->
            update { it.copy(gridColor = value) }
        }
        Ui.colorRow(colors, getString(R.string.color_ink), config.strokeColor) { value ->
            update { it.copy(strokeColor = value) }
        }
        Ui.colorRow(colors, getString(R.string.color_key_bg), config.keyBgColor) { value ->
            update { it.copy(keyBgColor = value) }
        }
        Ui.colorRow(colors, getString(R.string.color_key_text), config.keyTextColor) { value ->
            update { it.copy(keyTextColor = value) }
        }
        Ui.colorRow(colors, getString(R.string.color_accent), config.keyAccentColor) { value ->
            update { it.copy(keyAccentColor = value) }
        }

        val boardCard = Ui.card(parent, getString(R.string.sec_board))
        Ui.switchRow(boardCard, getString(R.string.sw_grid), checked = config.gridEnabled) { value ->
            update { it.copy(gridEnabled = value) }
        }
        Ui.switchRow(boardCard, getString(R.string.sw_baseline), checked = config.showBaseline) { value ->
            update { it.copy(showBaseline = value) }
        }
        Ui.switchRow(
            boardCard,
            getString(R.string.sw_finger),
            getString(R.string.sw_finger_summary),
            config.allowFinger
        ) { value -> update { it.copy(allowFinger = value) } }
        Ui.switchRow(
            boardCard,
            getString(R.string.sw_autoshift),
            getString(R.string.sw_autoshift_summary),
            config.autoShift
        ) { value -> update { it.copy(autoShift = value) } }

        val layoutCard = Ui.card(parent, getString(R.string.sec_layout))
        Ui.switchRow(layoutCard, getString(R.string.sw_toolbar), checked = config.showToolbar) { value ->
            update { it.copy(showToolbar = value) }
        }
        Ui.bodyText(layoutCard, getString(R.string.layout_position))
        val sideSpinner = Spinner(this)
        val sides = ToolbarSide.entries
        sideSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sides.map { it.label })
        sideSpinner.setSelection(sides.indexOf(config.toolbarSide).coerceAtLeast(0), false)
        sideSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val side = sides.getOrNull(position) ?: return
                if (side != config.toolbarSide) update { it.copy(toolbarSide = side) }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) = Unit
        }
        layoutCard.addView(sideSpinner, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val actions = Ui.card(parent)
        Ui.buttonRow(
            actions,
            getString(R.string.action_duplicate) to { duplicateProfile() },
            getString(R.string.action_restore) to { restoreProfile() }
        )
        Ui.fullButton(actions, getString(R.string.action_delete), outlined = true) { confirmDelete() }
    }

    private fun duplicateProfile() {
        val copy = repository.duplicate(config.id, config.name + " (salinan)") ?: return
        config = copy
        selectedIsToolbar = true
        selectedRowIndex = 0
        repository.setActive(copy.id)
        buildKeysTab()
        applyPreview()
        updateTitle()
        Toast.makeText(this, getString(R.string.action_duplicate) + ": " + copy.name, Toast.LENGTH_SHORT).show()
    }

    private fun restoreProfile() {
        if (config.builtinId == null) {
            Toast.makeText(this, R.string.action_restore, Toast.LENGTH_SHORT).show()
            return
        }
        Ui.confirmDialog(
            context = this,
            title = getString(R.string.action_restore) + ": " + config.name,
            message = getString(R.string.pref_reset_summary),
            confirmLabel = getString(R.string.action_restore)
        ) {
            val restored = repository.restoreFactory(config.id) ?: return@confirmDialog
            config = restored
            selectedIsToolbar = true
            selectedRowIndex = 0
            buildKeysTab()
            buildLookTab()
            applyPreview()
            updateTitle()
        }
    }

    private fun confirmDelete() {
        if (repository.profiles().size <= 1) {
            Toast.makeText(this, R.string.notice_min_row, Toast.LENGTH_SHORT).show()
            return
        }
        Ui.confirmDialog(
            context = this,
            title = getString(R.string.dialog_delete_title, config.name),
            message = null,
            confirmLabel = getString(R.string.action_delete)
        ) {
            repository.delete(config.id)
            finish()
        }
    }

    companion object {
        const val EXTRA_CONFIG_ID = "config_id"
        private const val MAX_ROWS = 4
    }
}
