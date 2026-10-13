package dev.retrobit.penboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.GlobalPrefs
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.PresetRepository
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.databinding.ActivityMainBinding
import dev.retrobit.penboard.ui.Preview
import dev.retrobit.penboard.ui.Ui

/** Dasbor: status keyboard, papan aktif, daftar papan, preferensi, dan panduan. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository by lazy { PresetRepository(this) }
    private val prefs by lazy { GlobalPrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.headerTitle.text = getString(R.string.app_name)
        binding.header.headerSubtitle.text = getString(R.string.app_subtitle)
        binding.header.headerIcon.setImageResource(R.drawable.ic_launcher_foreground)

        if (prefs.showGuideOnStart) {
            prefs.showGuideOnStart = false
            showGuideDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ------------------------------------------------------------------ render

    private fun render() {
        val content = binding.content
        content.removeAllViews()
        val config = repository.active()
        renderStatus(content)
        renderActiveBoard(content, config)
        renderProfiles(content, config)
        renderPreferences(content)
        renderGuide(content)
    }

    private fun renderStatus(parent: LinearLayout) {
        val card = Ui.card(parent, getString(R.string.status_title))
        val message = when {
            isImeSelected() -> R.string.status_selected
            isImeEnabled() -> R.string.status_enabled_not_selected
            else -> R.string.status_disabled
        }
        Ui.bodyText(card, getString(message))
        Ui.buttonRow(
            card,
            getString(R.string.action_pick_keyboard) to { showImePicker() },
            getString(R.string.action_system_settings) to { openImeSettings() }
        )
    }

    private fun renderActiveBoard(parent: LinearLayout, config: PenboardConfig) {
        val density = resources.displayMetrics.density
        val card = Ui.card(parent, getString(R.string.section_active_board) + " · " + config.name)

        val board = BoardView(this)
        card.addView(board, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        Preview.boardPreview(card, board, config, density, maxHeightFraction = 0.42f)

        // Tulisan disimpan otomatis per papan, sehingga tetap ada setelah aplikasi ditutup.
        board.restore(repository.loadBoard(config.id))
        val persist = {
            repository.saveBoard(config.id, board.snapshot())
            Unit
        }
        board.onContentChanged = persist
        board.onEraseHappened = persist
        board.onStrokeCommitted = persist

        Ui.buttonRow(
            card,
            getString(R.string.action_scroll_older) to { board.scrollByLines(3) },
            getString(R.string.action_scroll_newer) to { board.scrollByLines(-3) }
        )
        Ui.bodyText(card, getString(R.string.board_saved_hint))

        val keys = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(keys, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        Preview.populate(keys, config, density, currentTool = ToolMode.WRITE)

        Ui.buttonRow(
            card,
            getString(R.string.action_test) to { startActivity(Intent(this, TestActivity::class.java)) },
            getString(R.string.action_edit) to { openEditor(config) },
            getString(R.string.action_duplicate) to { duplicateProfile(config) }
        )
    }

    private fun renderProfiles(parent: LinearLayout, active: PenboardConfig) {
        val card = Ui.card(
            parent,
            getString(R.string.section_profiles),
            getString(R.string.section_profiles_summary)
        )
        repository.profiles().forEach { profile ->
            val lines = (profile.boardHeightDp / profile.lineGapDp).coerceAtLeast(1)
            val keyCount = profile.toolbarRows.sumOf { it.keys.size } + profile.boardRows.sumOf { it.keys.size }
            Ui.listRow(
                parent = card,
                title = profile.name,
                subtitle = getString(R.string.profile_detail, profile.boardHeightDp, lines, keyCount),
                badge = if (profile.id == active.id) getString(R.string.badge_active) else null,
                actionLabel = getString(R.string.action_edit),
                onAction = { openEditor(profile) },
                onMore = { showProfileOptions(profile) },
                onClick = {
                    repository.setActive(profile.id)
                    prefs.lastUsedProfileId = profile.id
                    render()
                }
            )
        }
        Ui.fullButton(card, getString(R.string.action_new_profile)) { duplicateProfile(repository.active()) }
    }

    private fun renderPreferences(parent: LinearLayout) {
        val card = Ui.card(parent, getString(R.string.section_preferences))
        Ui.switchRow(
            parent = card,
            title = getString(R.string.pref_haptics),
            summary = getString(R.string.pref_haptics_summary),
            checked = prefs.hapticsEnabled
        ) { prefs.hapticsEnabled = it }

        Ui.switchRow(
            parent = card,
            title = getString(R.string.pref_guide),
            checked = prefs.showGuideOnStart
        ) { prefs.showGuideOnStart = it }

        Ui.divider(card)
        Ui.fullButton(card, getString(R.string.pref_reset), outlined = true) {
            Ui.confirmDialog(
                context = this,
                title = getString(R.string.dialog_reset_title),
                message = getString(R.string.dialog_reset_message),
                confirmLabel = getString(R.string.action_yes)
            ) {
                repository.resetEverything()
                render()
            }
        }
        Ui.bodyText(card, getString(R.string.pref_reset_summary))
    }

    private fun renderGuide(parent: LinearLayout) {
        val card = Ui.card(parent, getString(R.string.section_guide))
        Ui.bodyText(card, getString(R.string.guide_body))
    }

    // ------------------------------------------------------------------ aksi

    private fun openEditor(config: PenboardConfig) {
        startActivity(
            Intent(this, BoardEditorActivity::class.java)
                .putExtra(BoardEditorActivity.EXTRA_CONFIG_ID, config.id)
        )
    }

    private fun duplicateProfile(source: PenboardConfig) {
        val copy = repository.duplicate(source.id, source.name + " (salinan)") ?: return
        repository.setActive(copy.id)
        render()
    }

    private fun showProfileOptions(profile: PenboardConfig) {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        options.add(getString(R.string.action_edit))
        actions.add { openEditor(profile) }

        options.add(getString(R.string.action_duplicate))
        actions.add { duplicateProfile(profile) }

        options.add(getString(R.string.action_rename))
        actions.add {
            Ui.textInputDialog(this, getString(R.string.dialog_rename_title), profile.name) { name ->
                if (name.isNotBlank()) {
                    repository.rename(profile.id, name)
                    render()
                }
            }
        }

        if (profile.builtinId != null) {
            options.add(getString(R.string.action_restore))
            actions.add {
                repository.restoreFactory(profile.id)
                render()
            }
        }

        if (repository.profiles().size > 1) {
            options.add(getString(R.string.action_delete))
            actions.add {
                Ui.confirmDialog(
                    context = this,
                    title = getString(R.string.dialog_delete_title, profile.name),
                    message = null,
                    confirmLabel = getString(R.string.action_delete)
                ) {
                    repository.delete(profile.id)
                    render()
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle(profile.name)
            .setItems(options.toTypedArray()) { _, which -> actions[which].invoke() }
            .show()
    }

    private fun showGuideDialog() {
        val switch = com.google.android.material.materialswitch.MaterialSwitch(this).apply {
            text = getString(R.string.guide_dont_show)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@MainActivity, 20f), Ui.dp(this@MainActivity, 8f), Ui.dp(this@MainActivity, 20f), 0)
        }
        container.addView(switch, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        AlertDialog.Builder(this)
            .setTitle(R.string.guide_first_title)
            .setMessage(R.string.guide_body)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ -> prefs.showGuideOnStart = !switch.isChecked }
            .show()
    }

    // ------------------------------------------------------------------ status sistem

    private fun isImeEnabled(): Boolean {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return manager.enabledInputMethodList.any { it.packageName == packageName }
    }

    private fun isImeSelected(): Boolean {
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return current != null && current.contains(packageName)
    }

    private fun showImePicker() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.showInputMethodPicker()
    }

    private fun openImeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
