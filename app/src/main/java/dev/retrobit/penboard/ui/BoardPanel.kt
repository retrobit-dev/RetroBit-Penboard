package dev.retrobit.penboard.ui

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import dev.retrobit.penboard.R
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.IconStyle
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.util.GridMath
import dev.retrobit.penboard.util.PixelArt
import kotlin.math.min

/**
 * Susunan Penboard ala HiWrite: **papan tulis di kiri sampai tombol, grid tombol di kanan**.
 *
 * - Tiga kolom tombol tetap; jumlah baris 1-6 (diatur pengguna, lihat
 *   [PenboardConfig.gridRows]) sehingga sampai 18 tombol bisa dipasang.
 * - Tombol digambar sebagai **ikon** monokrom ([KeyIcons]) mengikuti tema retro
 *   cyberpunk grayscale ePaper, dan label teks hanya dipakai bila aksinya tidak punya ikon.
 *
 * Di atas papan ada **bingkai jendela**: bilah judul dengan tombol minimize (menyembunyikan
 * papan), dan di bawah papan ada **bilah tombol pixel art** ([PixelArt]) yang isinya bebas
 * diatur pengguna.
 *
 * Dipakai bersama oleh keyboard ([dev.retrobit.penboard.ime.PenboardInputService])
 * dibuat sepenuhnya dari kode supaya tidak ada berkas tata letak yang perlu disamakan.
 */
class BoardPanel(private val context: Context) {

    interface Listener {
        fun onKey(key: KeyDef, action: KeyAction)
        fun onStrokeStarted()
        fun onStrokeCommitted()
        fun onPenButton(primary: Boolean)
        fun onContentChanged()
        /** Tombol minimize di bilah jendela: sembunyikan papan. */
        fun onMinimize()
    }

    var listener: Listener? = null

    private val density = context.resources.displayMetrics.density
    private val buttons = mutableListOf<KeyButton>()
    private val gridRows = mutableListOf<LinearLayout>()
    private val gridContainer = LinearLayout(context)
    private val panelRow = LinearLayout(context)
    private val panel = LinearLayout(context)
    private val panelBackground = ScanlineDrawable()

    // Bingkai jendela: bilah judul + tombol minimize.
    private val titleBar = LinearLayout(context)
    private val titleLabel = AppCompatTextView(context)
    private val minimize = KeyButton(context)

    // Papan tulis + tombol pixel art tepat di bawahnya (satu kolom di dalam bingkai).
    private val boardColumn = LinearLayout(context)
    private val bottomContainer = LinearLayout(context)

    val root = FrameLayout(context)
    val board = BoardView(context)

    init {
        board.onStrokeStarted = { listener?.onStrokeStarted() }
        board.onStrokeCommitted = { listener?.onStrokeCommitted() }
        board.onContentChanged = { listener?.onContentChanged() }
        board.onPenButtonPrimary = { listener?.onPenButton(true) }
        board.onPenButtonSecondary = { listener?.onPenButton(false) }
        board.edgeToEdge = true

        gridContainer.orientation = LinearLayout.VERTICAL
        gridContainer.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        for (rowIndex in 0 until GridMath.MAX_ROWS) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            for (column in 0 until GridMath.COLUMNS) {
                val button = KeyButton(context)
                buttons.add(button)
                row.addView(button)
            }
            gridRows.add(row)
            gridContainer.addView(row)
        }

        // Papan tulis dan tombol bawahnya berada dalam satu kolom, jadi tombol bawah
        // selalu tepat di bawah area menulis dan tidak pernah keluar dari bingkai.
        boardColumn.orientation = LinearLayout.VERTICAL
        boardColumn.addView(
            board,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        )
        bottomContainer.orientation = LinearLayout.VERTICAL
        boardColumn.addView(
            bottomContainer,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        panelRow.orientation = LinearLayout.HORIZONTAL
        panelRow.gravity = Gravity.CENTER_VERTICAL
        panelRow.addView(boardColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        panelRow.addView(
            gridContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // ---------------------------------------------------------- bilah judul
        titleBar.orientation = LinearLayout.HORIZONTAL
        titleBar.gravity = Gravity.CENTER_VERTICAL
        titleLabel.apply {
            textSize = 11f
            letterSpacing = 0.14f
            typeface = Typeface.MONOSPACE
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 0, 0, 0)
        }
        titleBar.addView(titleLabel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        minimize.isFocusable = false
        minimize.onTap = { listener?.onMinimize() }
        minimize.contentDescription = context.getString(R.string.action_minimize)
        titleBar.addView(minimize)

        panel.orientation = LinearLayout.VERTICAL
        panel.addView(
            titleBar,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        )
        panel.addView(
            panelRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        root.addView(
            panel,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }

    /**
     * Terapkan seluruh pengaturan ke papan dan tombol.
     *
     * [availableHeightPx] adalah tinggi yang disediakan keyboard (bila diketahui).
     * Papan dipangkas otomatis supaya bingkai + bilah judul + tombol bawah tidak
     * pernah terpotong di layar kecil.
     */
    fun applyConfig(config: PenboardConfig, availableHeightPx: Int = 0) {
        val spacing = (GridMath.SPACING_DP * density).toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val panelPadding = (3 * density).toInt()
        val minCell = (44 * density).toInt()
        // Bingkai jendela -> bilah judul; bingkai mati tapi minimize nyala -> bilah kecil.
        val titleBarHeight = (config.titleBarHeightDp() * density).toInt()
        val showTitleBar = config.showsTitleBar
        val minBoardHeightPx = (PenboardConfig.MIN_BOARD_HEIGHT_DP * density).toInt()

        val requestedBoardHeightPx = (config.boardHeightDp * density).toInt()
        val boardHeightPx = if (availableHeightPx > 0) {
            // Bagian tetap: padding bingkai + bilah judul + baris tombol bawah.
            val requestedCell = GridMath.cellSizePx(
                requestedBoardHeightPx,
                screenWidth,
                spacing,
                minCell,
                config.gridRows
            )
            val requestedGridWidth = requestedCell * GridMath.COLUMNS + spacing * (GridMath.COLUMNS - 1)
            val requestedBoardWidth =
                (screenWidth - 2 * panelPadding - requestedGridWidth - spacing).coerceAtLeast(1)
            val requestedBottomRows = GridMath.wrapRows(
                config.bottomButtonCount,
                GridMath.columnsFor(requestedBoardWidth, requestedCell, spacing)
            )
            val bottomBlock = if (requestedBottomRows > 0) {
                spacing + requestedBottomRows * (requestedCell + spacing)
            } else {
                0
            }
            val fixed = 2 * panelPadding +
                (if (showTitleBar) titleBarHeight + spacing else 0) +
                bottomBlock
            GridMath.fitBoardHeightPx(
                requestedBoardHeightPx,
                availableHeightPx,
                fixed,
                minBoardHeightPx
            )
        } else {
            requestedBoardHeightPx
        }

        val cell = GridMath.cellSizePx(
            boardHeightPx,
            screenWidth,
            spacing,
            minCell,
            config.gridRows
        )

        panel.setPadding(panelPadding, panelPadding, panelPadding, panelPadding)
        panelBackground.radiusPx = config.cornerRadiusDp * density
        panelBackground.fillColor =
            BoardView.withAlpha(config.panelColor, config.panelOpacity * 255 / 100)
        // Bingkai jendela: garis tepi lebih tegas + bilah judul berpemisah.
        panelBackground.borderColor = BoardView.withAlpha(
            config.accentColor,
            if (config.windowFrame) 190 else 120
        )
        panelBackground.borderWidthPx = if (config.windowFrame) 1.5f * density else 1f
        // Bilah judul hanya digambar bila bingkai jendela menyala; bilah kecil minimize
        // sengaja dibiarkan polos supaya panel tetap terlihat "tanpa bingkai".
        panelBackground.titleBarHeightPx =
            if (config.windowFrame) (titleBarHeight + panelPadding).toFloat() else 0f
        panelBackground.titleBarColor = BoardView.withAlpha(config.accentColor, 30)
        panelBackground.dividerColor = BoardView.withAlpha(config.accentColor, 150)
        panelBackground.scanlines = config.scanlines
        panelBackground.lineSpacingPx = 3f * density
        panel.background = panelBackground

        // ---------------------------------------------------------- bilah judul / bilah kecil
        titleBar.visibility = if (showTitleBar) View.VISIBLE else View.GONE
        titleBar.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            titleBarHeight
        ).apply { bottomMargin = if (showTitleBar) spacing else 0 }
        titleLabel.visibility = if (config.windowFrame) View.VISIBLE else View.GONE
        titleLabel.text = config.effectiveWindowTitle
        titleLabel.setTextColor(
            BoardView.withAlpha(config.accentColor, (config.buttonOpacity * 255 / 100).coerceIn(60, 255))
        )

        val minimizeSide = (18 * density).toInt()
        minimize.visibility = if (config.minimizeButton) View.VISIBLE else View.GONE
        minimize.repeatOnHold = false
        minimize.layoutParams = LinearLayout.LayoutParams(minimizeSide, minimizeSide)
        minimize.style(
            radiusPx = config.cornerRadiusDp * density * 0.6f,
            fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
            borderColor = BoardView.withAlpha(config.accentColor, 170),
            labelColor = config.buttonTextColor,
            textSizePx = minimizeSide * 0.2f,
            haptics = config.haptics
        )
        if (config.iconStyle == IconStyle.PIXEL) {
            minimize.text = ""
            minimize.showPixels(PixelArt.MINIMIZE, config.buttonTextColor, 0.66f)
        } else {
            minimize.text = ""
            minimize.showGlyph(KeyIcons.minimize, config.buttonTextColor, 0.62f)
        }

        board.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            boardHeightPx
        )
        board.lineGapPx = config.lineGapDp * density
        board.penColor = config.currentPenColor
        board.penWidthPx = config.penWidthDp * density
        board.allowFinger = config.allowFinger
        board.pressureEnabled = config.pressureEnabled
        board.showLines = config.showLines
        board.dotLines = config.scanlines
        board.haptics = config.haptics
        board.edgeToEdge = true
        board.cornerRadiusPx = config.cornerRadiusDp * density
        board.boardColor = config.boardColor
        board.boardAlpha = config.boardOpacity * 255 / 100
        board.lineColor = config.lineColor
        board.accentColor = config.accentColor

        val defaults = PenboardConfig.defaultGridKeys()
        gridRows.forEachIndexed { rowIndex, row ->
            row.visibility = if (config.gridRows > rowIndex) View.VISIBLE else View.GONE
        }
        for (index in 0 until PenboardConfig.GRID_SLOTS) {
            val button = buttons[index]
            val key = config.gridKeys.getOrNull(index) ?: defaults[index]
            button.visibility = if (index < config.gridButtonCount) View.VISIBLE else View.INVISIBLE
            button.repeatOnHold = key.repeatOnHold
            button.onTap = { listener?.onKey(key, key.action) }
            button.contentDescription = config.labelFor(key)
            button.layoutParams = LinearLayout.LayoutParams(cell, cell).apply {
                marginStart = if (index % GridMath.COLUMNS == 0) 0 else spacing
                topMargin = if (index >= GridMath.COLUMNS) spacing else 0
            }
            button.style(
                radiusPx = config.cornerRadiusDp * density * 0.9f,
                fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
                borderColor = BoardView.withAlpha(config.accentColor, 130),
                labelColor = config.buttonTextColor,
                textSizePx = cell * 0.26f,
                haptics = config.haptics
            )
            applyIcon(button, config, key, fraction = 0.5f)
        }
        gridContainer.layoutParams = LinearLayout.LayoutParams(
            cell * GridMath.COLUMNS + spacing * (GridMath.COLUMNS - 1),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = spacing }

        val gridWidth = cell * GridMath.COLUMNS + spacing * (GridMath.COLUMNS - 1)
        applyBottomBar(config, cell, spacing, screenWidth, gridWidth, panelPadding)
    }

    /**
     * Pasang ikon pada [button] mengikuti gaya pilihan pengguna ([PenboardConfig.iconStyle]):
     * **pixel art** 12 x 12 ([PixelArt]) atau **ikon vektor** ([KeyIcons]). Label teks
     * hanya dipakai bila aksinya tidak punya ikon sama sekali.
     */
    private fun applyIcon(button: KeyButton, config: PenboardConfig, key: KeyDef, fraction: Float) {
        val action = key.action
        if (config.iconStyle == IconStyle.PIXEL && PixelArt.hasPattern(action)) {
            button.text = ""
            button.showPixels(PixelArt.pattern(action), config.buttonTextColor, fraction)
            return
        }
        val glyph = KeyIcons.glyph(action)
        if (glyph != null) {
            button.text = ""
            button.showGlyph(glyph, config.buttonTextColor, fraction)
            return
        }
        button.showPixels(null, config.buttonTextColor)
        button.showGlyph(null, config.buttonTextColor)
        button.text = config.labelFor(key)
    }

    /**
     * Tombol **tepat di bawah area menulis** (masih di dalam bingkai jendela):
     * ukurannya sama persis dengan tombol samping ([gridCell]), ikonnya pixel art
     * ([PixelArt]), dan isinya bebas diatur pengguna.
     *
     * Kalau satu baris tidak cukup, tombolnya mengalir ke baris berikutnya di bawah
     * papan tulis (hitungan kolomnya di [GridMath.columnsFor]).
     */
    private fun applyBottomBar(
        config: PenboardConfig,
        gridCell: Int,
        spacing: Int,
        screenWidth: Int,
        gridWidth: Int,
        panelPadding: Int
    ) {
        bottomContainer.removeAllViews()
        val count = config.bottomButtonCount.coerceIn(0, PenboardConfig.BOTTOM_SLOTS)
        if (count <= 0) {
            bottomContainer.visibility = View.GONE
            return
        }
        bottomContainer.visibility = View.VISIBLE

        // Lebar yang tersedia untuk tombol bawah = lebar papan tulis.
        val boardWidth = (screenWidth - 2 * panelPadding - gridWidth - spacing).coerceAtLeast(1)
        val columns = GridMath.columnsFor(boardWidth, gridCell, spacing)
        val rows = GridMath.wrapRows(count, columns)

        val defaults = PenboardConfig.defaultBottomKeys()
        for (rowIndex in 0 until rows) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }
            for (column in 0 until columns) {
                val index = rowIndex * columns + column
                if (index >= count) break
                val key = config.bottomKeys.getOrNull(index)
                    ?: defaults.getOrElse(index) { defaults.last() }
                row.addView(buildBottomButton(config, key, gridCell, spacing, column == 0))
            }
            bottomContainer.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = spacing }
            )
        }
    }

    /** Satu tombol bawah: ukuran &amp; gayanya sama dengan tombol samping, ikonnya pixel art. */
    private fun buildBottomButton(
        config: PenboardConfig,
        key: KeyDef,
        side: Int,
        spacing: Int,
        first: Boolean
    ): KeyButton = KeyButton(context).apply {
        repeatOnHold = key.repeatOnHold
        onTap = { listener?.onKey(key, key.action) }
        contentDescription = config.labelFor(key)
        layoutParams = LinearLayout.LayoutParams(side, side).apply {
            marginStart = if (first) 0 else spacing
        }
        style(
            radiusPx = config.cornerRadiusDp * density * 0.9f,
            fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
            borderColor = BoardView.withAlpha(config.accentColor, 130),
            labelColor = config.buttonTextColor,
            textSizePx = side * 0.26f,
            haptics = config.haptics
        )
        applyIcon(this, config, key, fraction = 0.62f)
    }

}
