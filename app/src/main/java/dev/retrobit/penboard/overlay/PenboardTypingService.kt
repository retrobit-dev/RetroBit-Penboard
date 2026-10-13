package dev.retrobit.penboard.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import dev.retrobit.penboard.R
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.util.AccessibilityIds
import dev.retrobit.penboard.util.TextInsert

/**
 * **Penboard Tulis Otomatis** — menuliskan hasil tulisan tangan langsung ke kolom
 * isian aplikasi lain, **tanpa** Penboard harus menjadi keyboard aktif.
 *
 * Keyboard biasa hanya bisa mengetik ketika ia yang dipilih sebagai keyboard; papan
 * melayang Penboard sering dipakai bersama keyboard lain (mis. keyboard Huawei),
 * sehingga teksnya hanya tersalin ke papan klip. Layanan aksesibilitas ini menutup
 * celah itu:
 *
 *  1. menyisipkan teks pada posisi kursor lewat `ACTION_SET_TEXT`
 *     (hitungannya di [TextInsert] supaya bisa diuji unit),
 *  2. bila kolom isian tidak mendukungnya, memakai tempel (`ACTION_PASTE`) dengan
 *     isi papan klip dikembalikan seperti semula,
 *  3. melayani tombol kursor, hapus, dan papan klip saat keyboard Penboard tidak aktif.
 */
class PenboardTypingService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 200
        }
        setActive(this)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        setActive(null)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        setActive(null)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    // ------------------------------------------------------------------ teks

    /** Tulis [text] ke kolom isian yang sedang fokus. */
    fun typeText(text: String): Boolean {
        if (text.isEmpty()) return false
        val node = focusedEditable() ?: return false
        if (setNodeText(node, TextInsert.into(node.text?.toString().orEmpty(), node.textSelectionStart, node.textSelectionEnd, text))) {
            return true
        }
        return pasteText(node, text)
    }

    /** Apakah keyboard sistem sedang tampil di layar. */
    fun isKeyboardVisible(): Boolean = try {
        windows.any { window -> window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal memeriksa keyboard", error)
        false
    }

    /**
     * Tutup keyboard sistem (kalau sedang tampil) dengan menekan tombol kembali.
     * Hanya dilakukan bila keyboard benar-benar ada, supaya tombol kembali tidak
     * malah keluar dari aplikasi yang sedang dipakai.
     */
    fun hideKeyboard(): Boolean {
        if (!isKeyboardVisible()) return false
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal menutup keyboard", error)
            false
        }
    }

    /** Beberapa huruf sebelum kursor pada kolom yang sedang fokus (boleh null). */
    fun textBeforeCursor(limit: Int): String? {
        val node = focusedEditable() ?: return null
        val text = node.text?.toString() ?: return null
        val caret = node.textSelectionStart
        if (caret <= 0) return null
        val from = if (caret > text.length) text.length else caret
        return text.substring((from - limit).coerceAtLeast(0), from)
    }

    /** Jalankan aksi tombol keyboard tanpa keyboard Penboard aktif. */
    fun perform(action: KeyAction): Boolean = when (action) {
        KeyAction.CURSOR_LEFT -> moveCaret(-1)
        KeyAction.CURSOR_RIGHT -> moveCaret(1)
        KeyAction.CURSOR_HOME -> setSelection(0, 0)
        KeyAction.CURSOR_END -> setSelection(-1, -1)
        KeyAction.BACKSPACE -> edit { text, start, end -> TextInsert.deleteBackward(text, start, end) }
        KeyAction.DELETE_FORWARD -> edit { text, start, end -> TextInsert.deleteForward(text, start, end) }
        KeyAction.DELETE_WORD -> edit { text, start, end -> TextInsert.deleteWord(text, start, end) }
        KeyAction.SELECT_ALL -> selectAll()
        KeyAction.COPY -> act(AccessibilityNodeInfo.ACTION_COPY)
        KeyAction.CUT -> act(AccessibilityNodeInfo.ACTION_CUT)
        KeyAction.PASTE -> act(AccessibilityNodeInfo.ACTION_PASTE)
        else -> false
    }

    private fun moveCaret(delta: Int): Boolean {
        val node = focusedEditable() ?: return false
        val text = node.text?.toString().orEmpty()
        val caret = TextInsert.moveCaret(text, node.textSelectionStart, delta)
        return setSelection(caret, caret)
    }

    private fun selectAll(): Boolean {
        val node = focusedEditable() ?: return false
        val length = node.text?.length ?: 0
        return setSelection(node, 0, length)
    }

    private fun act(action: Int): Boolean {
        val node = focusedEditable() ?: return false
        return try {
            node.performAction(action)
        } catch (error: Throwable) {
            Log.w(TAG, "Aksi $action gagal", error)
            false
        }
    }

    private fun edit(transform: (String, Int, Int) -> TextInsert.Result): Boolean {
        val node = focusedEditable() ?: return false
        val text = node.text?.toString().orEmpty()
        val result = transform(text, node.textSelectionStart, node.textSelectionEnd)
        // Tidak ada yang berubah (mis. hapus di awal kolom): dianggap sudah ditangani.
        if (result.text == text) return true
        return setNodeText(node, result)
    }

    private fun setSelection(start: Int, end: Int): Boolean {
        val node = focusedEditable() ?: return false
        val length = node.text?.length ?: 0
        val from = if (start < 0) length else start.coerceIn(0, length)
        val to = if (end < 0) length else end.coerceIn(0, length)
        return setSelection(node, from, to)
    }

    private fun setSelection(node: AccessibilityNodeInfo, start: Int, end: Int): Boolean = try {
        val bundle = Bundle()
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bundle)
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal memindahkan kursor", error)
        false
    }

    private fun setNodeText(node: AccessibilityNodeInfo, result: TextInsert.Result): Boolean = try {
        val bundle = Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, result.text)
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, result.caret)
        bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, result.caret)
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal menulis teks", error)
        false
    }

    /** Cadangan: tempel lewat papan klip, lalu isi papan klip dikembalikan. */
    private fun pasteText(node: AccessibilityNodeInfo, text: String): Boolean = try {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val previous = clipboard.primaryClip?.let { clip ->
            if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(this).toString() else null
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        if (previous != null) {
            handler.postDelayed({
                try {
                    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), previous))
                } catch (error: Throwable) {
                    Log.w(TAG, "Gagal mengembalikan papan klip", error)
                }
            }, CLIP_RESTORE_MS)
        }
        pasted
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal menempel teks", error)
        false
    }

    private fun focusedEditable(): AccessibilityNodeInfo? {
        val focused = try {
            findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal mencari kolom isian", error)
            null
        }
        return focused?.takeIf { it.isEditable }
    }

    companion object {
        private const val TAG = "PenboardTyping"
        private const val CLIP_RESTORE_MS = 1200L

        @Volatile
        private var active: PenboardTypingService? = null

        /** Layanan yang sedang tersambung (untuk papan melayang). */
        fun current(): PenboardTypingService? = active

        /** Apakah layanan "Penboard Tulis Otomatis" menyala di pengaturan aksesibilitas. */
        fun isEnabled(context: Context): Boolean {
            if (active != null) return true
            return try {
                val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                val wanted = ComponentName(context, PenboardTypingService::class.java)
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                    .any { info -> AccessibilityIds.matches(info.id, context.packageName, wanted.className) }
            } catch (error: Throwable) {
                Log.w(TAG, "Gagal membaca daftar aksesibilitas", error)
                false
            }
        }

        private fun setActive(service: PenboardTypingService?) {
            active = service
        }
    }
}
