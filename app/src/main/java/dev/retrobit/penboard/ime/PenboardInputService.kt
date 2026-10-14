package dev.retrobit.penboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import dev.retrobit.penboard.R
import dev.retrobit.penboard.data.ConfigStore
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.ink.HandwritingInput
import dev.retrobit.penboard.ui.BoardPanel
import dev.retrobit.penboard.util.SystemHandwriting

/**
 * Keyboard Penboard: papan tulis di kiri, grid tombol 3x3 di kanan.
 *
 * Tulisan di papan dikenali menjadi teks (input otomatis setelah jeda tanpa
 * goresan baru, atau lewat tombol "Teks"/tombol samping pena).
 */
class PenboardInputService : InputMethodService(), BoardPanel.Listener {

    private lateinit var store: ConfigStore
    private var panel: BoardPanel? = null
    private var handwriting: HandwritingInput? = null

    /** Tinggi yang disediakan keyboard (0 = belum diketahui). */
    private var availableHeightPx = 0

    private val configListener: (PenboardConfig) -> Unit = { config ->
        panel?.applyConfig(config, availableHeightPx)
    }

    override fun onCreate() {
        super.onCreate()
        store = ConfigStore.get(this)
        setActive(this)
    }

    override fun onDestroy() {
        handwriting?.release()
        store.removeListener(configListener)
        if (current() === this) setActive(null)
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val created = BoardPanel(this)
        created.listener = this
        created.applyConfig(store.config)
        // Tinggi keyboard baru diketahui setelah tampilannya diukur; papan dipangkas
        // sekali lagi dengan angka itu supaya bingkai + tombol bawah tidak terpotong.
        created.root.addOnLayoutChangeListener { view, _, top, _, bottom, _, _, _, _ ->
            val height = bottom - top
            if (height <= 0 || height == availableHeightPx) return@addOnLayoutChangeListener
            availableHeightPx = height
            view.post { panel?.applyConfig(store.config, height) }
        }
        panel = created
        handwriting = HandwritingInput(
            context = this,
            board = created.board,
            config = { store.config },
            commit = { text -> commitText(text) },
            notify = { message -> toast(message) },
            // Huruf yang sudah ada sebelum kursor: membantu mesin memilih kata.
            preContext = {
                try {
                    currentInputConnection?.getTextBeforeCursor(20, 0)?.toString()
                } catch (error: Throwable) {
                    null
                }
            },
            onUnavailable = { message -> handleEngineUnavailable(message) }
        )
        store.addListener(configListener)
        return created.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Setiap kali keyboard muncul, pastikan ukuran papan masih pas.
        val height = panel?.root?.height ?: 0
        if (height > 0) {
            availableHeightPx = height
            panel?.applyConfig(store.config, height)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        handwriting?.cancelAuto()
        super.onFinishInputView(finishingInput)
    }

    // ------------------------------------------------------------------ aksi tombol

    override fun onKey(key: KeyDef, action: KeyAction) {
        handleAction(action, key.text)
    }

    override fun onStrokeStarted() {
        handwriting?.onStrokeStarted()
    }

    override fun onStrokeCommitted() {
        handwriting?.onStrokeCommitted()
    }

    override fun onPenButton(primary: Boolean) {
        val config = store.config
        val chosen = if (primary) config.penButtonPrimary else config.penButtonSecondary
        chosen.key?.let { handleAction(it, "") }
    }

    override fun onContentChanged() = Unit

    private fun handleAction(action: KeyAction, text: String) {
        when (action) {
            KeyAction.TEXT -> commitText(text.ifBlank { " " })
            KeyAction.SPACE -> commitText(" ")
            KeyAction.ENTER -> sendEnter()
            KeyAction.BACKSPACE -> deleteBackward()
            KeyAction.DELETE_WORD -> deleteWord()
            KeyAction.DELETE_FORWARD -> sendKey(KeyEvent.KEYCODE_FORWARD_DEL)
            KeyAction.TAB -> sendKey(KeyEvent.KEYCODE_TAB)

            // Penempatan kursor (tombol panah ala HiWrite).
            KeyAction.CURSOR_LEFT -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT)
            KeyAction.CURSOR_RIGHT -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            KeyAction.CURSOR_UP -> sendKey(KeyEvent.KEYCODE_DPAD_UP)
            KeyAction.CURSOR_DOWN -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN)
            KeyAction.CURSOR_HOME -> sendKey(KeyEvent.KEYCODE_MOVE_HOME)
            KeyAction.CURSOR_END -> sendKey(KeyEvent.KEYCODE_MOVE_END)

            // Papan klip & pilihan teks.
            KeyAction.SELECT_ALL -> contextMenu(android.R.id.selectAll)
            KeyAction.CUT -> contextMenu(android.R.id.cut)
            KeyAction.COPY -> contextMenu(android.R.id.copy)
            KeyAction.PASTE -> contextMenu(android.R.id.paste)
            KeyAction.UNDO -> panel?.board?.undo()
            KeyAction.REDO -> panel?.board?.redo()
            KeyAction.CLEAR -> {
                // Tidak ada lagi yang perlu dijadikan teks setelah papan dibersihkan.
                handwriting?.cancelAuto()
                panel?.board?.clear()
            }
            KeyAction.DELETE_STROKE -> panel?.board?.deleteLastStroke()
            KeyAction.CYCLE_PEN -> cyclePen()
            KeyAction.RECOGNIZE -> handwriting?.recognize(announce = true)
            KeyAction.SYSTEM_HANDWRITING -> bridgeToSystemHandwriting()
            KeyAction.KEYBOARD_PICKER -> openKeyboardPicker()
            KeyAction.HIDE -> requestHideSelf(0)
        }
    }

    /**
     * Tombol minimize di bilah jendela: sembunyikan papan dengan menutup keyboard.
     * Keyboard muncul lagi begitu pengguna menyentuh kolom isian.
     */
    override fun onMinimize() {
        handwriting?.cancelAuto()
        requestHideSelf(0)
    }

    // ------------------------------------------------------------------ teks

    /** Kirim teks ke kolom isian. */
    fun commitFromExternal(text: String): Boolean = commitText(text)

    fun deleteBackwardFromExternal(): Boolean {
        if (currentInputConnection == null) return false
        deleteBackward()
        return true
    }

    /** Hapus satu kata utuh dari kolom isian (bukan satu huruf demi satu huruf). */
    fun deleteWordFromExternal(): Boolean {
        if (currentInputConnection == null) return false
        deleteWord()
        return true
    }

    /**
     * Jalankan aksi tombol untuk kolom isian (panah kursor, tab, papan klip).
     * Mengembalikan false bila tidak ada kolom isian yang sedang aktif.
     */
    fun performExternal(action: KeyAction): Boolean {
        if (currentInputConnection == null) return false
        handleAction(action, "")
        return true
    }

    /** Kirim satu penekanan tombol ke kolom isian (dipakai panah kursor dan tab). */
    private fun sendKey(code: Int) {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    /** Aksi menu teks: pilih semua, potong, salin, tempel. */
    private fun contextMenu(id: Int) {
        val connection = currentInputConnection ?: return
        connection.performContextMenuAction(id)
    }

    private fun commitText(text: String): Boolean {
        val connection = currentInputConnection ?: return false
        connection.commitText(text, 1)
        return true
    }

    private fun deleteBackward() {
        val connection = currentInputConnection ?: return
        if (!connection.getSelectedText(0).isNullOrEmpty()) {
            connection.commitText("", 1)
            return
        }
        val before = connection.getTextBeforeCursor(1, 0)
        if (before.isNullOrEmpty()) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        } else {
            connection.deleteSurroundingText(1, 0)
        }
    }

    private fun deleteWord() {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        if (before.isEmpty()) {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            return
        }
        val trailing = before.length - before.trimEnd().length
        val word = before.trimEnd().takeLastWhile { !it.isWhitespace() }.length
        val count = (trailing + word).coerceAtLeast(1)
        connection.deleteSurroundingText(count, 0)
    }

    private fun sendEnter() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    // ------------------------------------------------------------------ lain-lain

    private fun cyclePen() {
        val config = store.config
        if (config.penColors.size <= 1) return
        val next = (config.penColorIndex + 1) % config.penColors.size
        store.update { it.copy(penColorIndex = next) }
        toast(getString(R.string.msg_pen_changed, next + 1))
    }

    /**
     * Pindah ke keyboard tulisan tangan bawaan perangkat (mis. Huawei/Celia) — mesin
     * mereka tidak bisa dipanggil langsung oleh aplikasi lain.
     */
    private fun bridgeToSystemHandwriting() {
        val target = SystemHandwriting.switchFromIme(this)
        if (target != null) {
            toast(getString(R.string.system_handwriting_switched, target.label))
        } else {
            toast(getString(R.string.system_handwriting_picker))
        }
    }

    /**
     * Mesin pengenalan bawaan tidak bisa dipakai (mis. perangkat Huawei tanpa Google
     * Play services). Bila pengguna mengizinkan, Penboard memindahkan dirinya ke
     * keyboard tulisan tangan perangkat supaya pengguna tetap bisa menulis.
     */
    private fun handleEngineUnavailable(message: String) {
        toast(message)
        if (!store.config.autoSwitchToSystemHandwriting) return
        val target = SystemHandwriting.pickBest(SystemHandwriting.candidates(this), packageName) ?: return
        val switched = SystemHandwriting.switchFromIme(this)
        if (switched != null) {
            toast(getString(R.string.system_handwriting_switched, target.label))
        }
    }

    /** Buka pemilih keyboard Android (untuk berpindah ke keyboard lain dan kembali). */
    private fun openKeyboardPicker() {
        SystemHandwriting.openPicker(this)
        toast(getString(R.string.msg_keyboard_picker))
    }

    private fun toast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (error: Throwable) {
            // Perangkat tertentu melarang toast dari keyboard; abaikan.
        }
    }

    companion object {
        @Volatile
        private var active: PenboardInputService? = null

        /** Keyboard Penboard yang sedang aktif. */
        fun current(): PenboardInputService? = active

        private fun setActive(service: PenboardInputService?) {
            active = service
        }
    }
}
