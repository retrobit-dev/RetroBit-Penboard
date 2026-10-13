package dev.retrobit.penboard.overlay

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.retrobit.penboard.MainActivity
import dev.retrobit.penboard.R
import dev.retrobit.penboard.board.BoardExporter
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.board.KeyButton
import dev.retrobit.penboard.data.BoardLayoutMode
import dev.retrobit.penboard.data.Colors
import dev.retrobit.penboard.data.GlobalPrefs
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenButtonAction
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.data.PresetRepository
import dev.retrobit.penboard.data.ToolMode
import dev.retrobit.penboard.data.ToolbarSide
import dev.retrobit.penboard.databinding.OverlayBoardBinding
import dev.retrobit.penboard.ime.PenboardInputService
import dev.retrobit.penboard.ink.HandwritingInput
import dev.retrobit.penboard.ui.KeyGridBuilder
import dev.retrobit.penboard.ui.KeyRowsBuilder

/**
 * Papan tulis melayang: jendela overlay yang bisa digeser di atas aplikasi lain.
 *
 * Teks hasil tulisan dikirim lewat keyboard Penboard (InputConnection) bila Penboard
 * sedang menjadi keyboard aktif; jika tidak, teks disalin ke papan klip sebagai
 * cadangan supaya tetap bisa ditempel.
 */
class PenboardOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: GlobalPrefs
    private lateinit var repository: PresetRepository

    private var binding: OverlayBoardBinding? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var keyButtons: List<KeyButton> = emptyList()
    private var config: PenboardConfig = PenboardConfig()
    private var tool: ToolMode = ToolMode.WRITE
    private var penColorIndex = -1
    private var loadedProfileId: String? = null

    private lateinit var handwriting: HandwritingInput

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = GlobalPrefs(this)
        repository = PresetRepository(this)
        config = repository.active()
        handwriting = HandwritingInput(
            boardProvider = { binding?.overlayBoard },
            languageProvider = { config.recognitionLanguage },
            configAutoRecognize = { config.autoRecognize },
            configDelayMs = { config.autoRecognizeDelayMs },
            configLines = { config.recognitionLines },
            configClearAfter = { config.clearAfterRecognize },
            configAutoSpace = { config.autoSpaceAfterWord },
            commit = { text -> commitText(text) },
            notify = { message -> toast(message) }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground()
        showOverlay()
        running = true
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handwriting.cancelPending()
        binding?.let { safeBinding ->
            persistBoard()
            runCatching { windowManager.removeView(safeBinding.root) }
        }
        binding = null
        super.onDestroy()
    }

    // ------------------------------------------------------------ jendela overlay

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay() {
        if (binding != null) return
        val viewBinding = OverlayBoardBinding.inflate(LayoutInflater.from(this))
        binding = viewBinding

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Tidak boleh merebut fokus: aplikasi di bawahnya tetap menerima ketikan.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.overlayX
            y = prefs.overlayY
        }
        layoutParams = params

        viewBinding.overlayRoot.setOnClickListener { }
        viewBinding.overlayClose.setOnClickListener { stopSelf() }
        viewBinding.overlayKeyboard.setOnClickListener {
            val service = PenboardInputService.instance
            if (service != null) {
                service.hideKeyboardWindow()
                // naikkan papan melayang supaya tidak tertutup keyboard
                layoutParams?.let { params ->
                    params.y = 0
                    prefs.overlayY = 0
                    runCatching { windowManager.updateViewLayout(viewBinding.root, params) }
                }
                toast(getString(R.string.overlay_keyboard_hidden))
            } else {
                toast(getString(R.string.overlay_keyboard_not_active))
            }
        }
        attachDragHandle(viewBinding)

        applyConfig()
        runCatching { windowManager.addView(viewBinding.root, params) }
            .onFailure { error ->
                Log.e(TAG, "Gagal menampilkan overlay", error)
                toast(getString(R.string.overlay_permission_needed))
                stopSelf()
            }
    }

    private fun attachDragHandle(viewBinding: OverlayBoardBinding) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        viewBinding.overlayHandle.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager.updateViewLayout(viewBinding.root, params) }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    prefs.overlayX = params.x
                    prefs.overlayY = params.y
                    true
                }

                else -> false
            }
        }
        // ketukan singkat pada pegangan = pindah ke sisi kiri/kanan layar
        viewBinding.overlayHandle.setOnClickListener {
            val params = layoutParams ?: return@setOnClickListener
            params.x = 0
            params.y = 0
            prefs.overlayX = params.x
            prefs.overlayY = params.y
            runCatching { windowManager.updateViewLayout(viewBinding.root, params) }
        }
    }

    // ------------------------------------------------------------ isi papan & tombol

    private fun applyConfig() {
        val viewBinding = binding ?: return
        config = repository.active().sanitized()
        val density = resources.displayMetrics.density

        val radius = 18f * density
        viewBinding.overlayPanel.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius)
            setColor(Colors.withOpacity(config.panelColor, config.panelOpacity))
        }
        viewBinding.overlayTitle.text = getString(R.string.overlay_title) + " \u00b7 " + config.name

        val maxHeight = (resources.displayMetrics.heightPixels * 0.7f).toInt()
        val boardHeight = (config.boardHeightDp * density).toInt().coerceAtMost(maxHeight)
        viewBinding.overlayBoard.layoutParams = viewBinding.overlayBoard.layoutParams.also { it.height = boardHeight }
        viewBinding.overlayBoard.cornerRadiusPx = radius
        viewBinding.overlayBoard.applyConfig(config)
        viewBinding.overlayBoard.tool = tool
        viewBinding.overlayBoard.penColorOverride = currentPenColor()
        viewBinding.overlayBoard.onContentChanged = { persistBoard() }
        viewBinding.overlayBoard.onEraseHappened = { persistBoard() }
        viewBinding.overlayBoard.onStrokeCommitted = {
            persistBoard()
            handwriting.onStrokeCommitted()
        }
        viewBinding.overlayBoard.onToolChangedByPen = { penTool ->
            tool = penTool
            refreshToolHighlight()
        }
        viewBinding.overlayBoard.onPenButtonAction = { action -> runPenButtonAction(action) }
        loadBoardIfNeeded()

        viewBinding.overlayToolbarTop.removeAllViews()
        viewBinding.overlayToolbarSide.removeAllViews()
        viewBinding.overlayToolbarBottom.removeAllViews()
        viewBinding.overlayGrid.removeAllViews()

        val gridMode = config.boardLayout == BoardLayoutMode.BOARD_LEFT_GRID
        viewBinding.overlayGrid.visibility = if (gridMode) View.VISIBLE else View.GONE
        viewBinding.overlayKeyRows.visibility = if (gridMode) View.GONE else View.VISIBLE
        viewBinding.overlayToolbarTop.visibility = if (gridMode) View.GONE else View.VISIBLE
        viewBinding.overlayToolbarBottom.visibility = if (gridMode) View.GONE else View.VISIBLE
        viewBinding.overlayToolbarSide.visibility = if (gridMode) View.GONE else View.VISIBLE

        val buttons = mutableListOf<KeyButton>()
        if (gridMode) {
            buttons += KeyGridBuilder.populate(
                container = viewBinding.overlayGrid,
                config = config,
                density = density,
                haptics = prefs.hapticsEnabled,
                currentTool = tool,
                interactive = true,
                boardHeightPx = boardHeight,
                availableWidthPx = resources.displayMetrics.widthPixels,
                onKey = { key -> onKey(key) }
            )
        } else if (config.showToolbar && config.toolbarRows.isNotEmpty()) {
            val host: LinearLayout = when (config.toolbarSide) {
                ToolbarSide.TOP -> viewBinding.overlayToolbarTop
                ToolbarSide.SIDE -> viewBinding.overlayToolbarSide
                ToolbarSide.BOTTOM -> viewBinding.overlayToolbarBottom
            }
            val fixedWidth = if (config.toolbarSide == ToolbarSide.SIDE) (config.keyHeightDp * density).toInt() else null
            buttons += KeyRowsBuilder.populate(
                container = host,
                rows = config.toolbarRows,
                config = config,
                density = density,
                haptics = prefs.hapticsEnabled,
                currentTool = tool,
                fixedKeyWidthPx = fixedWidth,
                interactive = true,
                onKey = { key -> onKey(key) }
            )
        }
        if (!gridMode) {
            buttons += KeyRowsBuilder.populate(
                container = viewBinding.overlayKeyRows,
                rows = config.boardRows,
                config = config,
                density = density,
                haptics = prefs.hapticsEnabled,
                currentTool = tool,
                fixedKeyWidthPx = null,
                interactive = true,
                onKey = { key -> onKey(key) }
            )
        }
        keyButtons = buttons
        refreshToolHighlight()
    }

    private fun refreshToolHighlight() {
        keyButtons.forEach { button ->
            val key = button.key ?: return@forEach
            button.active = KeyRowsBuilder.isActive(key, tool)
        }
    }

    private fun onKey(key: KeyDef) {
        val board = binding?.overlayBoard
        when (key.action) {
            KeyAction.INSERT -> commitText(key.text)
            KeyAction.SPACE -> commitText(" ")
            KeyAction.ENTER -> commitText("\n")
            KeyAction.BACKSPACE -> backspace()
            KeyAction.DELETE_STROKE -> board?.removeLastStroke()
            KeyAction.CLEAR -> board?.clearBoard()
            KeyAction.UNDO -> board?.undo()
            KeyAction.REDO -> board?.redo()
            KeyAction.SCROLL_OLDER -> board?.scrollByLines(3)
            KeyAction.SCROLL_NEWER -> board?.scrollByLines(-3)
            KeyAction.RECOGNIZE -> handwriting.recognize(announce = true)
            KeyAction.COLOR -> cyclePenColor()
            KeyAction.OVERLAY_TOGGLE -> stopSelf()
            KeyAction.SAVE_IMAGE -> saveImage()
            KeyAction.PROFILE_NEXT -> switchProfile(1)
            KeyAction.PROFILE_PREV -> switchProfile(-1)
            KeyAction.ERASE -> {
                tool = ToolMode.ERASE
                board?.tool = tool
            }

            KeyAction.WRITE -> {
                tool = ToolMode.WRITE
                board?.tool = tool
            }

            KeyAction.HIDE -> PenboardInputService.instance?.hideKeyboardWindow()
            KeyAction.CURSOR_LEFT, KeyAction.CURSOR_RIGHT -> {
                // kursor tidak bisa digerakkan tanpa kolom teks aktif; pakai keyboard Penboard
                toast(getString(R.string.overlay_keyboard_not_active))
            }
        }
        refreshToolHighlight()
    }

    private fun runPenButtonAction(action: PenButtonAction) {
        val board = binding?.overlayBoard
        when (action) {
            PenButtonAction.RECOGNIZE -> handwriting.recognize(announce = true)
            PenButtonAction.ERASE -> {
                tool = ToolMode.ERASE
                board?.tool = tool
            }

            PenButtonAction.UNDO -> board?.undo()
            PenButtonAction.CLEAR -> board?.clearBoard()
            PenButtonAction.OVERLAY -> stopSelf()
            PenButtonAction.HIDE -> PenboardInputService.instance?.hideKeyboardWindow()
            PenButtonAction.NONE -> Unit
        }
        refreshToolHighlight()
    }

    // ------------------------------------------------------------ teks & papan klip

    private fun commitText(text: String): Boolean {
        if (text.isEmpty()) return true
        val ime = PenboardInputService.instance
        if (ime != null && ime.commitFromExternal(text)) return true
        // Cadangan: salin ke papan klip supaya bisa ditempel manual.
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Penboard", text))
        toast(getString(R.string.overlay_copied))
        return false
    }

    private fun backspace() {
        val ime = PenboardInputService.instance
        if (ime != null && ime.deleteBackwardFromExternal()) return
        toast(getString(R.string.overlay_keyboard_not_active))
    }

    private fun currentPenColor(): Int? {
        val palette = if (config.penColors.isNotEmpty()) config.penColors else Colors.penPalette
        return palette.getOrNull(penColorIndex)
    }

    private fun cyclePenColor() {
        val palette = if (config.penColors.isNotEmpty()) config.penColors else Colors.penPalette
        val next = penColorIndex + 1
        if (next >= palette.size) {
            penColorIndex = -1
            binding?.overlayBoard?.penColorOverride = null
            toast(getString(R.string.ime_pen_color, Colors.toHex(config.strokeColor)))
            return
        }
        penColorIndex = next
        binding?.overlayBoard?.penColorOverride = palette[next]
        toast(getString(R.string.ime_pen_color, Colors.toHex(palette[next])))
    }

    private fun switchProfile(direction: Int) {
        val profiles = repository.profiles()
        if (profiles.size < 2) return
        val currentIndex = profiles.indexOfFirst { it.id == repository.activeId() }
        val size = profiles.size
        val nextIndex = ((currentIndex + direction) % size + size) % size
        persistBoard()
        repository.setActive(profiles[nextIndex].id)
        loadedProfileId = null
        applyConfig()
    }

    private fun saveImage() {
        val board = binding?.overlayBoard ?: return
        if (!board.hasContent) {
            toast(getString(R.string.ime_board_empty))
            return
        }
        val uri = BoardExporter.saveToGallery(this, board)
        toast(getString(if (uri != null) R.string.ime_saved else R.string.ime_save_failed))
    }

    private fun persistBoard(profileId: String? = null) {
        val board = binding?.overlayBoard ?: return
        repository.saveBoard(profileId ?: loadedProfileId ?: repository.activeId(), board.snapshot())
    }

    private fun loadBoardIfNeeded() {
        val board = binding?.overlayBoard ?: return
        val activeId = repository.activeId()
        if (loadedProfileId == activeId) return
        board.restore(repository.loadBoard(activeId))
        loadedProfileId = activeId
    }

    private fun toast(message: String) {
        runCatching { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    // ------------------------------------------------------------ notifikasi

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.overlay_channel_description) }
            manager.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PenboardOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.overlay_close), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        const val ACTION_START = "dev.retrobit.penboard.overlay.START"
        const val ACTION_STOP = "dev.retrobit.penboard.overlay.STOP"
        private const val CHANNEL_ID = "penboard_overlay"
        private const val NOTIFICATION_ID = 4711
        private const val TAG = "PenboardOverlay"

        /** Ditandai true selama overlay aktif, dipakai untuk status di aplikasi. */
        @Volatile
        var running: Boolean = false
            private set
    }
}
