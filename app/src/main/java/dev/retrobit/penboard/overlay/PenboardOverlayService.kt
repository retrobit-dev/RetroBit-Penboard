package dev.retrobit.penboard.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.retrobit.penboard.MainActivity
import dev.retrobit.penboard.R
import dev.retrobit.penboard.data.ConfigStore
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.ime.PenboardInputService
import dev.retrobit.penboard.ink.HandwritingInput
import dev.retrobit.penboard.ink.InkRecognition
import dev.retrobit.penboard.ui.BoardPanel
import dev.retrobit.penboard.util.OverlayMath
import dev.retrobit.penboard.util.SystemHandwriting

/**
 * Papan tulis melayang di atas aplikasi lain.
 *
 * Memakai susunan yang sama dengan keyboard (papan di kiri, grid tombol di kanan) dan
 * bisa digeser lewat pegangan di atasnya — **jari naik berarti papan naik**.
 *
 * Saat papan dipakai, keyboard sistem disembunyikan (bila pengaturan
 * "Sembunyikan keyboard sistem" menyala) supaya yang tampil hanya papan melayang:
 * keyboard Penboard ditutup sendiri, atau lewat layanan "Penboard Tulis Otomatis".
 *
 * Bila keyboard Penboard sedang aktif, teks langsung dikirim ke kolom isian; bila
 * tidak, lewat layanan Tulis Otomatis; cadangan terakhir: salin ke papan klip.
 */
class PenboardOverlayService : Service(), BoardPanel.Listener {

    private lateinit var store: ConfigStore
    private lateinit var windowManager: WindowManager
    private var panel: BoardPanel? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var handwriting: HandwritingInput? = null

    private var dragStartX = 0
    private var dragStartY = 0
    private var windowStartX = 0
    private var windowStartY = 0

    private val prefs by lazy { getSharedPreferences("penboard_overlay", MODE_PRIVATE) }

    private val configListener: (PenboardConfig) -> Unit = { config -> panel?.applyConfig(config) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = ConfigStore.get(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (panel == null) showOverlay()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        store.removeListener(configListener)
        handwriting?.release()
        panel?.let { view ->
            try {
                windowManager.removeView(view.root)
            } catch (error: Throwable) {
                Log.w(TAG, "Gagal melepas papan melayang", error)
            }
        }
        panel = null
        handwriting = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal menghentikan notifikasi", error)
        }
        super.onDestroy()
    }

    // ------------------------------------------------------------------ tampilan

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            toast(getString(R.string.overlay_permission_missing))
            stopSelf()
            return
        }
        startForeground(NOTIFICATION_ID, buildNotification())

        val created = BoardPanel(this, showHandle = true)
        created.listener = this
        created.applyConfig(store.config)
        panel = created
        handwriting = HandwritingInput(
            context = this,
            board = created.board,
            config = { store.config },
            commit = { text -> commitText(text) },
            notify = { message -> toast(message) },
            // Huruf sebelum kursor (bila "Penboard Tulis Otomatis" menyala).
            preContext = { PenboardTypingService.current()?.textBeforeCursor(20) },
            onUnavailable = { message -> toast(message) }
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = prefs.getInt(KEY_X, 0)
            // Posisi dari sesi sebelumnya dibatasi lagi supaya papan tidak nyangkut
            // di luar layar (mis. setelah putaran layar atau papan diubah tingginya).
            y = OverlayMath.defaultY(
                boardHeightPx = created.root.height.takeIf { it > 0 } ?: dp(200f),
                screenHeightPx = screenHeight(),
                preferredY = prefs.getInt(KEY_Y, dp(110f)),
                topInsetPx = dp(24f),
                bottomInsetPx = dp(16f)
            )
        }
        layoutParams = params
        try {
            windowManager.addView(created.root, params)
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal menampilkan papan melayang", error)
            toast(getString(R.string.msg_overlay_failed))
            stopSelf()
            return
        }
        created.handle?.setOnTouchListener(dragListener())
        store.addListener(configListener)
        running = true
        // Keyboard yang sudah terbuka sebelum papan dipakai ikut ditutup.
        created.root.postDelayed({ hideSystemKeyboardIfEnabled() }, HIDE_KEYBOARD_DELAY_MS)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun dragListener() = View.OnTouchListener { _, event ->
        val params = layoutParams ?: return@OnTouchListener false
        val root = panel?.root ?: return@OnTouchListener false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX.toInt()
                dragStartY = event.rawY.toInt()
                windowStartX = params.x
                windowStartY = params.y
                true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = windowStartX + (event.rawX.toInt() - dragStartX)
                // Gravitasi BAWAH: jari naik (rawY mengecil) harus menaikkan papan,
                // jadi arahnya dibalik dari perpindahan jari. Hitungannya diuji unit.
                val desiredY = OverlayMath.dragY(windowStartY, dragStartY.toFloat(), event.rawY)
                params.y = OverlayMath.clampY(
                    desiredY,
                    screenHeight(),
                    panel?.root?.height ?: 0,
                    topInsetPx = dp(24f),
                    bottomInsetPx = dp(16f)
                )
                try {
                    windowManager.updateViewLayout(root, params)
                } catch (error: Throwable) {
                    Log.w(TAG, "Gagal memindahkan papan melayang", error)
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                prefs.edit().putInt(KEY_X, params.x).putInt(KEY_Y, params.y).apply()
                true
            }
            else -> false
        }
    }

    /** Tinggi layar saat ini (px). */
    private fun screenHeight(): Int {
        val metrics = resources.displayMetrics
        return if (metrics.heightPixels > 0) metrics.heightPixels else 1920
    }

    /**
     * Sembunyikan keyboard sistem supaya yang tampil hanya papan melayang.
     * Dijalankan hanya bila pengaturannya menyala. Keyboard Penboard ditutup oleh
     * dirinya sendiri; keyboard lain ditutup lewat layanan Tulis Otomatis.
     */
    fun hideSystemKeyboardIfEnabled() {
        if (!store.config.hideSystemKeyboard) return
        val ime = PenboardInputService.current()
        if (ime != null && ime.hideKeyboardFromExternal()) return
        PenboardTypingService.current()?.hideKeyboard()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.overlay_channel_desc) }
            manager.createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopOverlay = PendingIntent.getService(
            this,
            1,
            Intent(this, PenboardOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.overlay_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.action_overlay_hide), stopOverlay)
            .setOngoing(true)
            .build()
    }

    // ------------------------------------------------------------------ aksi tombol

    override fun onKey(key: KeyDef, action: KeyAction) {
        // Papan sedang dipakai: keyboard sistem tidak perlu ikut tampil.
        hideSystemKeyboardIfEnabled()
        when (action) {
            KeyAction.TEXT -> commitText(key.text.ifBlank { " " })
            KeyAction.SPACE -> commitText(" ")
            KeyAction.ENTER -> commitText("\n")
            KeyAction.BACKSPACE -> deleteBackward()
            KeyAction.DELETE_WORD -> deleteWord()
            KeyAction.UNDO -> panel?.board?.undo()
            KeyAction.REDO -> panel?.board?.redo()
            KeyAction.CLEAR -> {
                handwriting?.cancelAuto()
                panel?.board?.clear()
            }
            KeyAction.DELETE_STROKE -> panel?.board?.deleteLastStroke()
            KeyAction.CYCLE_PEN -> cyclePen()
            KeyAction.RECOGNIZE -> handwriting?.recognize(announce = true)
            KeyAction.SYSTEM_HANDWRITING -> openHandwritingPicker()
            KeyAction.OVERLAY_TOGGLE, KeyAction.HIDE -> stopSelf()

            // Aksi yang butuh kolom isian: panah kursor, tab, dan papan klip.
            KeyAction.CURSOR_LEFT, KeyAction.CURSOR_RIGHT, KeyAction.CURSOR_UP, KeyAction.CURSOR_DOWN,
            KeyAction.CURSOR_HOME, KeyAction.CURSOR_END, KeyAction.DELETE_FORWARD, KeyAction.TAB,
            KeyAction.SELECT_ALL, KeyAction.CUT, KeyAction.COPY, KeyAction.PASTE -> fieldAction(action)
        }
    }

    /**
     * Teruskan aksi kolom isian: keyboard Penboard lebih dulu (paling tepat),
     * lalu "Penboard Tulis Otomatis", kalau keduanya tidak ada beri tahu.
     */
    private fun fieldAction(action: KeyAction) {
        val ime = PenboardInputService.current()
        if (ime != null && ime.performExternal(action)) return
        val typing = PenboardTypingService.current()
        if (typing != null && typing.perform(action)) return
        toast(getString(R.string.msg_needs_keyboard))
    }

    /**
     * Tulisan tidak bisa dikenali karena mesinnya. Bila Google Play services tidak
     * ada/hangus, tawarkan keyboard tulisan tangan perangkat supaya pengguna tetap
     * bisa menulis — bukan sekadar memberi tahu.
     */
    private fun handleEngineUnavailable(message: String) {
        toast(message)
        if (InkRecognition.servicesStatus(this).canAttempt) return
        SystemHandwriting.openPicker(this)
    }

    override fun onStrokeStarted() {
        // Pena mulai menulis: keyboard sistem tidak perlu ikut tampil.
        hideSystemKeyboardIfEnabled()
        handwriting?.onStrokeStarted()
    }

    override fun onStrokeCommitted() {
        handwriting?.onStrokeCommitted()
    }

    override fun onPenButton(primary: Boolean) {
        val config = store.config
        val chosen = if (primary) config.penButtonPrimary else config.penButtonSecondary
        chosen.key?.let { action -> onKey(KeyDef(action), action) }
    }

    override fun onContentChanged() = Unit

    // ------------------------------------------------------------------ teks

    /**
     * Kirim teks ke kolom isian: lewat keyboard Penboard bila aktif, kalau tidak
     * lewat "Penboard Tulis Otomatis" (aksesibilitas), dan salin sebagai cadangan.
     */
    private fun commitText(text: String): Boolean {
        val ime = PenboardInputService.current()
        if (ime != null && ime.commitFromExternal(text)) return true
        val typing = PenboardTypingService.current()
        if (typing != null && typing.typeText(text)) return true
        return try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
            toast(getString(R.string.overlay_copied))
            true
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal menyalin teks", error)
            false
        }
    }

    private fun deleteBackward() {
        val ime = PenboardInputService.current()
        if (ime != null && ime.deleteBackwardFromExternal()) return
        if (PenboardTypingService.current()?.perform(KeyAction.BACKSPACE) == true) return
        toast(getString(R.string.msg_needs_keyboard))
    }

    private fun deleteWord() {
        val ime = PenboardInputService.current()
        if (ime != null && ime.deleteWordFromExternal()) return
        if (PenboardTypingService.current()?.perform(KeyAction.DELETE_WORD) == true) return
        toast(getString(R.string.msg_needs_keyboard))
    }

    private fun cyclePen() {
        val config = store.config
        if (config.penColors.size <= 1) return
        val next = (config.penColorIndex + 1) % config.penColors.size
        store.update { it.copy(penColorIndex = next) }
        toast(getString(R.string.msg_pen_changed, next + 1))
    }

    /**
     * Papan melayang tidak bisa berpindah keyboard sendiri (hanya keyboard yang boleh),
     * jadi yang ditampilkan adalah pemilih keyboard dengan pesan yang jujur.
     */
    private fun openHandwritingPicker() {
        toast(getString(R.string.system_handwriting_picker))
        SystemHandwriting.openPicker(this)
    }

    private fun toast(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (error: Throwable) {
            // abaikan
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        /** Jeda sebelum keyboard sistem ditutup saat papan baru tampil (ms). */
        private const val HIDE_KEYBOARD_DELAY_MS = 420L

        const val ACTION_START = "dev.retrobit.penboard.action.SHOW_OVERLAY"
        const val ACTION_STOP = "dev.retrobit.penboard.action.HIDE_OVERLAY"

        private const val CHANNEL_ID = "penboard_overlay"
        private const val NOTIFICATION_ID = 4711
        private const val TAG = "PenboardOverlay"
        private const val KEY_X = "overlay_x"
        private const val KEY_Y = "overlay_y"

        @Volatile
        var running: Boolean = false
            internal set

        /** Tampilkan papan melayang. */
        fun start(context: Context) {
            val intent = Intent(context, PenboardOverlayService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Sembunyikan papan melayang. */
        fun stop(context: Context) {
            val intent = Intent(context, PenboardOverlayService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (error: Throwable) {
                Log.w(TAG, "Gagal menghentikan papan melayang", error)
            }
        }
    }
}
