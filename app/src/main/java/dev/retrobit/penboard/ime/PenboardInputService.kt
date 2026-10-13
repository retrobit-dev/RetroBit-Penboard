package dev.retrobit.penboard.ime

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.inputmethodservice.InputMethodService
import android.widget.LinearLayout
import android.widget.Toast
import dev.retrobit.penboard.R
import dev.retrobit.penboard.board.BoardExporter
import dev.retrobit.penboard.board.KeyButton
import dev.retrobit.penboard.data.Colors
import dev.retrobit.penboard.data.BoardLayoutMode
import dev.retrobit.penboard.data.GlobalPrefs
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.PresetRepository
import dev.retrobit.penboard.data.PenButtonAction
import dev.retrobit.penboard.data.Presets
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.data.ToolbarSide
import dev.retrobit.penboard.ink.HandwritingInput
import dev.retrobit.penboard.ink.InkRecognition
import dev.retrobit.penboard.databinding.KeyboardLayoutBinding
import dev.retrobit.penboard.overlay.PenboardOverlayService
import dev.retrobit.penboard.ui.KeyGridBuilder
import dev.retrobit.penboard.ui.KeyRowsBuilder

/**
 * Keyboard Penboard: papan tulis stylus yang bisa diatur penuh
 * (ukuran, transparansi, tombol, dan tata letaknya).
 */
class PenboardInputService : InputMethodService() {

    private var binding: KeyboardLayoutBinding? = null
    private var keyButtons: List<KeyButton> = emptyList()

    private val repository by lazy { PresetRepository(this) }
    private val prefs by lazy { GlobalPrefs(this) }

    private var config: PenboardConfig = Presets.defaultConfig()
    private var tool: ToolMode = ToolMode.WRITE
    private var penColorIndex = -1
    private var loadedProfileId: String? = null
    private lateinit var handwriting: HandwritingInput

    override fun onCreate() {
        super.onCreate()
        instance = this
        config = repository.active()
        handwriting = HandwritingInput(
            boardProvider = { binding?.boardView },
            languageProvider = { config.recognitionLanguage },
            configAutoRecognize = { config.autoRecognize },
            configDelayMs = { config.autoRecognizeDelayMs },
            configLines = { config.recognitionLines },
            configClearAfter = { config.clearAfterRecognize },
            configAutoSpace = { config.autoSpaceAfterWord },
            commit = { text -> commitText(text); true },
            notify = { message -> toast(message) }
        )
    }

    override fun onDestroy() {
        handwriting.cancelPending()
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** Sembunyikan jendela keyboard (dipakai tombol "papan melayang" & pegangan overlay). */
    fun hideKeyboardWindow() {
        requestHideSelf(0)
    }

    /** Kirim teks dari papan melayang lewat koneksi keyboard yang sedang aktif. */
    fun commitFromExternal(text: String): Boolean {
        val connection = currentInputConnection ?: return false
        connection.commitText(text, 1)
        return true
    }

    /** Hapus satu huruf dari papan melayang. */
    fun deleteBackwardFromExternal(): Boolean {
        val connection = currentInputConnection ?: return false
        connection.deleteSurroundingText(1, 0)
        return true
    }

    override fun onCreateInputView(): View {
        val binding = KeyboardLayoutBinding.inflate(layoutInflater)
        this.binding = binding
        binding.imeRoot.setOnClickListener { /* telan sentuhan di area kosong */ }
        // Keyboard tidak boleh gagal tampil hanya karena satu pengaturan bermasalah:
        // bila gagal, jatuh kembali ke profil bawaan.
        try {
            applyConfig()
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal menerapkan pengaturan, memakai bawaan", error)
            config = Presets.defaultConfig()
            try {
                applyConfig()
            } catch (fatal: Throwable) {
                Log.e(TAG, "Gagal menyiapkan tampilan keyboard", fatal)
            }
        }
        return binding.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        config = repository.active()
        applyConfig()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        persistBoard()
        super.onFinishInputView(finishingInput)
    }

    /** Papan stylus tidak boleh dipaksa mode layar penuh (mis. saat lanskap). */
    override fun onEvaluateFullscreenMode(): Boolean = false

    /**
     * Penboard **tidak** memakai mode tulisan tangan dari sistem: papan tulisnya sendiri yang
     * menangani stylus. Mengembalikan false mencegah sistem (mis. Huawei/EMUI atau Android 14+)
     * memunculkan panel tulisan tangan bawaan yang bertabrakan dengan papan Penboard.
     */
    override fun onStartStylusHandwriting(): Boolean {
        Log.i(TAG, "Permintaan tulisan tangan sistem diabaikan; Penboard memakai papan sendiri")
        return false
    }

    override fun onPrepareStylusHandwriting() {
        // tidak ada yang perlu disiapkan: papan Penboard sudah tampil
    }

    override fun onStylusHandwritingMotionEvent(motionEvent: android.view.MotionEvent) {
        // abaikan aliran stylus dari sistem; papan Penboard menerima sentuhan langsung
    }

    override fun onEvaluateInputViewShown(): Boolean = true

    // ------------------------------------------------------------ penerapan UI

    private fun applyConfig() {
        val binding = binding ?: return
        val density = resources.displayMetrics.density
        config = config.sanitized()

        val radius = 20f * density
        binding.panel.background = panelBackground(radius)
        binding.panelDivider.visibility = View.VISIBLE

        // Batasi tinggi papan agar keyboard tidak melebihi jendela IME di layar kecil.
        val maxBoardHeight = (resources.displayMetrics.heightPixels *
            if (config.boardLayout == BoardLayoutMode.BOARD_LEFT_GRID) 0.5f else 0.72f).toInt()
        val newBoardHeight = (config.boardHeightDp * density).toInt().coerceAtMost(maxBoardHeight)
        binding.boardView.layoutParams = binding.boardView.layoutParams.also { params ->
            params.height = newBoardHeight
        }
        binding.boardView.cornerRadiusPx = radius
        binding.boardView.applyConfig(config)
        binding.boardView.tool = tool
        binding.boardView.penColorOverride = currentPenColor()
        binding.boardView.onContentChanged = { persistBoard() }
        binding.boardView.onEraseHappened = { persistBoard() }
        binding.boardView.onStrokeCommitted = {
            persistBoard()
            handwriting.onStrokeCommitted()
        }
        binding.boardView.onPenButtonAction = { action -> runPenButtonAction(action) }
        binding.boardView.onToolChangedByPen = { penTool ->
            tool = penTool
            refreshToolHighlight()
        }
        loadBoardIfNeeded()

        binding.toolbarTop.removeAllViews()
        binding.toolbarSide.removeAllViews()
        binding.toolbarBottom.removeAllViews()
        binding.imeGrid.removeAllViews()

        val gridMode = config.boardLayout == BoardLayoutMode.BOARD_LEFT_GRID
        binding.imeGrid.visibility = if (gridMode) View.VISIBLE else View.GONE
        binding.keyRows.visibility = if (gridMode) View.GONE else View.VISIBLE
        binding.toolbarTop.visibility = if (gridMode) View.GONE else View.VISIBLE
        binding.toolbarBottom.visibility = if (gridMode) View.GONE else View.VISIBLE
        binding.toolbarSide.visibility = if (gridMode) View.GONE else View.VISIBLE

        val buttons = mutableListOf<KeyButton>()
        if (gridMode) {
            // Papan tulis di kiri, tombol operasi grid 3x3 di kanan.
            buttons += KeyGridBuilder.populate(
                container = binding.imeGrid,
                config = config,
                density = density,
                haptics = prefs.hapticsEnabled,
                currentTool = tool,
                interactive = true,
                boardHeightPx = newBoardHeight,
                availableWidthPx = resources.displayMetrics.widthPixels,
                onKey = { onKey(it) }
            )
        } else if (config.showToolbar && config.toolbarRows.isNotEmpty()) {
            val host: LinearLayout = when (config.toolbarSide) {
                ToolbarSide.TOP -> binding.toolbarTop
                ToolbarSide.SIDE -> binding.toolbarSide
                ToolbarSide.BOTTOM -> binding.toolbarBottom
            }
            val fixedWidth = if (config.toolbarSide == ToolbarSide.SIDE) {
                (config.keyHeightDp * density).toInt()
            } else {
                null
            }
            buttons += KeyRowsBuilder.populate(
                container = host,
                rows = config.toolbarRows,
                config = config,
                density = density,
                haptics = prefs.hapticsEnabled,
                currentTool = tool,
                fixedKeyWidthPx = fixedWidth,
                interactive = true,
                onKey = { onKey(it) }
            )
        }

        if (!gridMode) {
            buttons += KeyRowsBuilder.populate(
                container = binding.keyRows,
                rows = config.boardRows,
                config = config,
                density = density,
                haptics = prefs.hapticsEnabled,
                currentTool = tool,
                fixedKeyWidthPx = null,
                interactive = true,
                onKey = { onKey(it) }
            )
        }

        keyButtons = buttons
        refreshToolHighlight()
    }

    private fun panelBackground(radius: Float): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        setColor(Colors.withOpacity(config.panelColor, config.panelOpacity))
    }

    private fun refreshToolHighlight() {
        keyButtons.forEach { button ->
            val key = button.key ?: return@forEach
            button.active = KeyRowsBuilder.isActive(key, tool)
        }
    }

    // ------------------------------------------------------------ aksi tombol

    private fun onKey(key: KeyDef) {
        try {
            handleKey(key)
        } catch (error: Throwable) {
            Log.w(TAG, "Aksi tombol gagal: " + key.action.id, error)
        }
        refreshToolHighlight()
    }

    private fun handleKey(key: KeyDef) {
        val board = binding?.boardView
        when (key.action) {
            KeyAction.INSERT -> commitText(key.text)
            KeyAction.SPACE -> commitText(" ")
            KeyAction.ENTER -> performEnter()
            KeyAction.BACKSPACE -> deleteBackward()
            KeyAction.DELETE_STROKE -> board?.removeLastStroke()
            KeyAction.CLEAR -> board?.clearBoard()
            KeyAction.UNDO -> board?.undo()
            KeyAction.REDO -> board?.redo()
            KeyAction.CURSOR_LEFT -> sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
            KeyAction.CURSOR_RIGHT -> sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
            KeyAction.RECOGNIZE -> handwriting.recognize(announce = true)
            KeyAction.OVERLAY_TOGGLE -> toggleOverlay()
            KeyAction.SCROLL_OLDER -> board?.scrollByLines(SCROLL_STEP_LINES)
            KeyAction.SCROLL_NEWER -> board?.scrollByLines(-SCROLL_STEP_LINES)
            KeyAction.COLOR -> cyclePenColor()
            KeyAction.PROFILE_PREV -> switchProfile(-1)
            KeyAction.PROFILE_NEXT -> switchProfile(1)
            KeyAction.SAVE_IMAGE -> saveImage()
            KeyAction.ERASE -> {
                tool = ToolMode.ERASE
                board?.tool = tool
            }
            KeyAction.WRITE -> {
                tool = ToolMode.WRITE
                board?.tool = tool
            }
            KeyAction.HIDE -> requestHideSelf(0)
        }
    }

    private fun commitText(text: String) {
        if (text.isEmpty()) return
        currentInputConnection?.commitText(text, 1)
    }

    private fun performEnter() {
        val connection = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: 0
        val hasAction = action != 0 &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        if (hasAction && !isMultiline(info)) {
            connection.performEditorAction(action)
            return
        }
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun isMultiline(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        return inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
    }

    private fun deleteBackward() {
        val connection = currentInputConnection ?: return
        if (!connection.getSelectedText(0).isNullOrEmpty()) {
            connection.commitText("", 1)
            return
        }
        connection.deleteSurroundingText(1, 0)
    }

    private fun sendKeyCode(code: Int) {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    /** Warna pena bergiliran dan tetap (sampai ditekan lagi untuk kembali ke warna profil). */
    private fun cyclePenColor() {
        val palette = if (config.penColors.isNotEmpty()) config.penColors else Colors.penPalette
        val next = penColorIndex + 1
        if (next >= palette.size) {
            penColorIndex = -1
            binding?.boardView?.penColorOverride = null
            toast(getString(R.string.ime_pen_color, Colors.toHex(config.strokeColor)))
            return
        }
        penColorIndex = next
        binding?.boardView?.penColorOverride = palette[next]
        toast(getString(R.string.ime_pen_color, Colors.toHex(palette[next])))
    }

    private fun switchProfile(direction: Int) {
        val profiles = repository.profiles()
        if (profiles.size < 2) return
        val currentIndex = profiles.indexOfFirst { it.id == repository.activeId() }
        val size = profiles.size
        val nextIndex = ((currentIndex + direction) % size + size) % size
        val next = profiles[nextIndex]
        persistBoard(loadedProfileId)
        repository.setActive(next.id)
        prefs.lastUsedProfileId = next.id
        config = next
        loadedProfileId = null
        applyConfig()
        toast(getString(R.string.ime_profile_switched, next.name))
    }

    /** Aksi tombol samping pena, dapat diatur per profil. */
    private fun runPenButtonAction(action: PenButtonAction) {
        val board = binding?.boardView
        when (action) {
            PenButtonAction.RECOGNIZE -> handwriting.recognize(announce = true)
            PenButtonAction.ERASE -> {
                tool = ToolMode.ERASE
                board?.tool = tool
            }

            PenButtonAction.UNDO -> board?.undo()
            PenButtonAction.CLEAR -> board?.clearBoard()
            PenButtonAction.OVERLAY -> toggleOverlay()
            PenButtonAction.HIDE -> requestHideSelf(0)
            PenButtonAction.NONE -> Unit
        }
        refreshToolHighlight()
    }

    /** Tampilkan atau sembunyikan papan tulis melayang di atas aplikasi lain. */
    private fun toggleOverlay() {
        if (PenboardOverlayService.running) {
            startService(
                Intent(this, PenboardOverlayService::class.java)
                    .setAction(PenboardOverlayService.ACTION_STOP)
            )
            toast(getString(R.string.overlay_stopped))
            return
        }
        if (!android.provider.Settings.canDrawOverlays(this)) {
            toast(getString(R.string.overlay_permission_needed))
            return
        }
        persistBoard()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(
                    Intent(this, PenboardOverlayService::class.java)
                        .setAction(PenboardOverlayService.ACTION_START)
                )
            } else {
                startService(
                    Intent(this, PenboardOverlayService::class.java)
                        .setAction(PenboardOverlayService.ACTION_START)
                )
            }
            toast(getString(R.string.overlay_started))
        }.onFailure { error ->
            Log.w(TAG, "Gagal menjalankan overlay", error)
            toast(getString(R.string.overlay_permission_needed))
        }
    }

    /** Simpan tulisan papan ke profil yang sedang dipakai. */
    private fun persistBoard(profileId: String? = null) {
        val board = binding?.boardView ?: return
        val target = profileId ?: loadedProfileId ?: repository.activeId()
        repository.saveBoard(target, board.snapshot())
    }

    /** Muat tulisan tersimpan sekali per profil (agar urungkan/ulangi tidak hilang). */
    private fun loadBoardIfNeeded() {
        val board = binding?.boardView ?: return
        val activeId = repository.activeId()
        if (loadedProfileId == activeId) return
        board.restore(repository.loadBoard(activeId))
        loadedProfileId = activeId
    }

    private fun currentPenColor(): Int? {
        val palette = if (config.penColors.isNotEmpty()) config.penColors else Colors.penPalette
        return palette.getOrNull(penColorIndex)
    }

    private fun saveImage() {
        val board = binding?.boardView ?: return
        if (!board.hasContent) {
            toast(getString(R.string.ime_board_empty))
            return
        }
        val uri = BoardExporter.saveToGallery(this, board)
        toast(getString(if (uri != null) R.string.ime_saved else R.string.ime_save_failed))
    }

    private fun toast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {
            // mengabaikan kegagalan menampilkan pesan
        }
    }

    companion object {
        private const val TAG = "PenboardIME"

        /** Instance yang sedang aktif, dipakai papan melayang untuk mengirim teks. */
        @Volatile
        var instance: PenboardInputService? = null
            private set

        const val SCROLL_STEP_LINES = 3
    }
}
