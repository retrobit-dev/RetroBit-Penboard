package dev.retrobit.penboard.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import dev.retrobit.penboard.R
import dev.retrobit.penboard.data.ConfigStore
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.ink.HandwritingInput
import dev.retrobit.penboard.ui.BoardPanel
import dev.retrobit.penboard.util.GridMath
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

    /** Lebar sebenarnya area keyboard (0 = belum diketahui). */
    private var availableWidthPx = 0

    /** Sisa tarikan sudut yang belum genap 1 dp (agar tarikan kecil tetap terasa). */
    private var pendingResizeDp = 0f

    private val configListener: (PenboardConfig) -> Unit = { config ->
        panel?.applyConfig(config, availableHeightPx, availableWidthPx)
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
        created.root.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            // Tinggi yang tersedia untuk papan = tinggi area keyboard - ruang gerak
            // yang sedang dipakai, supaya papan tidak tumbuh sendiri saat diangkat.
            val height = (bottom - top) - created.panelLiftPx
            val width = right - left
            if (height <= 0) return@addOnLayoutChangeListener
            if (height == availableHeightPx && width == availableWidthPx) {
                return@addOnLayoutChangeListener
            }
            availableHeightPx = height
            availableWidthPx = width
            view.post {
                panel?.applyConfig(store.config, height, width)
                restorePanelPosition()
            }
        }
        panel = created
        handwriting = HandwritingInput(
            context = this,
            board = created.board,
            config = { store.config },
            commit = { text, newlines -> commitRecognized(text, newlines) },
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
        // Setiap kali keyboard muncul, pastikan ukuran papan masih pas dan posisinya
        // kembali seperti terakhir dipindah pengguna.
        val current = panel
        val height = (current?.root?.height ?: 0) - (current?.panelLiftPx ?: 0)
        val width = current?.root?.width ?: 0
        if (height > 0) {
            availableHeightPx = height
            if (width > 0) availableWidthPx = width
            current?.applyConfig(store.config, height, availableWidthPx)
        }
        restorePanelPosition()
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

    // ------------------------------------------------------------------ geser & ubah ukuran

    /**
     * Bilah judul ditarik: papan dipindah ke mana saja di dalam area keyboard
     * (mode melayang), atau dikunci di tengah kalau fiturnya dimatikan.
     */
    override fun onMoveBy(dxPx: Int, dyPx: Int) {
        val current = panel ?: return
        if (!store.config.floatingPanel) return
        // Tarik ke atas (dyPx negatif) = papan diangkat, jadi ruang di bawahnya bertambah.
        current.placePanel(current.panelLeftPx + dxPx, current.panelLiftPx - dyPx)
    }

    /**
     * Sudut kanan bawah ditarik: **kanan/kiri** mengubah lebar papan (area tulis ikut
     * melebar/menyempit), **atas/bawah** mengubah tinggi papan — ukuran tombol kalau
     * ukurannya diatur manual, atau tinggi area tulis kalau ukurannya otomatis.
     */
    override fun onResizeBy(dxPx: Int, dyPx: Int) {
        val current = panel ?: return
        val areaWidth = current.root.width
        if (areaWidth <= 0) return
        val config = store.config
        val density = resources.displayMetrics.density

        var next = config
        if (dxPx != 0) {
            val fraction = (current.panelWidthPx + dxPx).toFloat() / areaWidth
            next = next.copy(
                panelWidthFraction = fraction.coerceIn(PenboardConfig.MIN_PANEL_WIDTH_FRACTION, 1f)
            )
        }
        if (dyPx != 0) {
            pendingResizeDp += dyPx / density
            val deltaDp = pendingResizeDp.toInt()
            if (deltaDp != 0) {
                pendingResizeDp -= deltaDp
                next = if (next.gridCellDp > 0) {
                    next.copy(gridCellDp = (next.gridCellDp + deltaDp).coerceIn(
                        PenboardConfig.MIN_CELL_DP,
                        PenboardConfig.MAX_CELL_DP
                    ))
                } else {
                    // Tinggi papan = baris x jarak baris, jadi tarikan vertikal mengubah
                    // jarak baris; ukuran tombol otomatis mengikuti tinggi baris itu.
                    next.copy(lineGapDp = (next.lineGapDp + deltaDp).coerceIn(
                        PenboardConfig.MIN_LINE_GAP_DP,
                        PenboardConfig.MAX_LINE_GAP_DP
                    ))
                }
            }
        }
        if (next != config) store.update { next }
    }

    /** Tarikan selesai: simpan posisi papan supaya tetap di situ saat keyboard muncul lagi. */
    override fun onGestureFinished() {
        val current = panel ?: return
        val areaWidth = current.root.width
        val allowed = current.root.maxHeightPx
        if (areaWidth <= 0) return
        val left = GridMath.toFraction(current.panelLeftPx, areaWidth)
        val lift = if (allowed > 0) GridMath.toFraction(current.panelLiftPx, allowed) else 0f
        if (left == store.config.panelOffsetX && lift == store.config.panelOffsetY) return
        store.update { it.copy(panelOffsetX = left, panelOffsetY = lift) }
    }

    /** Pasang posisi papan dari pengaturan (0 = rapat kiri bawah). */
    private fun restorePanelPosition() {
        val current = panel ?: return
        val areaWidth = current.root.width
        if (areaWidth <= 0) return
        if (!store.config.floatingPanel) {
            current.placePanel(0, 0)
            return
        }
        val allowed = current.root.maxHeightPx
        val lift = if (allowed > 0) GridMath.fromFraction(store.config.panelOffsetY, allowed) else 0
        current.placePanel(GridMath.fromFraction(store.config.panelOffsetX, areaWidth), lift)
    }

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

    /**
     * Kirim hasil tulisan ke kolom isian. Bila pengguna menulis di baris kedua papan,
     * satu enter dikirim lebih dulu supaya teksnya mendarat satu baris di bawah kursor;
     * baris ketiga = dua enter, dan seterusnya ([BoardView.newlineOffsetForInk]).
     *
     * Di kolom isian yang hanya satu baris, enter dilewati (kolomnya tidak bisa
     * menerimanya) dan teksnya tetap dikirim seperti biasa.
     */
    private fun commitRecognized(text: String, newlines: Int): Boolean {
        val connection = currentInputConnection ?: return false
        if (newlines > 0 && acceptsNewlines()) {
            connection.commitText("\n".repeat(newlines), 1)
        }
        connection.commitText(text, 1)
        return true
    }

    /** Kolom isian yang hanya satu baris tidak bisa menerima enter (mis. kotak pencarian). */
    private fun acceptsNewlines(): Boolean {
        val type = currentInputEditorInfo?.inputType ?: return true
        if ((type and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
        return (type and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
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
