package dev.retrobit.penboard.ui

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import dev.retrobit.penboard.R
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.IconStyle
import dev.retrobit.penboard.data.KeyAction
import dev.retrobit.penboard.data.KeyDef
import dev.retrobit.penboard.data.PenboardConfig
import dev.retrobit.penboard.util.GridMath
import dev.retrobit.penboard.util.KeyboardChars
import dev.retrobit.penboard.util.PixelArt
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Susunan Penboard: **papan tulis di kiri, kolom tombol persegi di kanan**.
 *
 * - Tombol samping **selalu persegi** dan jumlahnya bisa diatur: kolom 1-6, baris 1-6
 *   (sampai 36 tombol), mengikuti baris area tulis atau diatur sendiri. Ukuran
 *   otomatisnya = tinggi satu baris tulisan; kolom tombol selalu rata atas.
 * - Di atas papan ada **bingkai jendela**: bilah judul (bisa ditarik untuk memindah
 *   papan) dengan tombol minimize, dan **sudut kanan bawah** bisa ditarik untuk
 *   mengubah lebar papan sekaligus ukuran tombolnya.
 * - Di atas papan ada **bilah tombol atas** (dulu "tombol bawah"): sampai 80 tombol,
 *   rata kiri, jaraknya sama seperti tombol samping. Hanya satu baris yang tampil;
 *   baris lain diganti dengan panah tepat di atas sisi kanan area tulis.
 * - Ikon tombol mengikuti [PenboardConfig.iconStyle] (pixel art atau vektor).
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

        /** Bingkai atas ditarik ([dxPx], [dyPx]): papan dipindah. */
        fun onMoveBy(dxPx: Int, dyPx: Int)

        /** Sudut kanan bawah ditarik ([dxPx], [dyPx]): ukuran papan diubah. */
        fun onResizeBy(dxPx: Int, dyPx: Int)

        /** Tarikan selesai: simpan posisi/ukuran. */
        fun onGestureFinished()

        /** Bilah atas berpindah baris (panah atau geser). Tinggi papan tidak berubah. */
        fun onTopPageChanged() {}
    }

    var listener: Listener? = null

    private val density = context.resources.displayMetrics.density
    private val buttons = mutableListOf<KeyButton>()
    private val bottomButtons = mutableListOf<KeyButton>()
    private val panelRow = LinearLayout(context)
    private val panel = LinearLayout(context)
    private val panelBackground = ScanlineDrawable()
    private val boardColumn = LinearLayout(context)
    private val gridContainer = LinearLayout(context)
    private val bottomContainer = LinearLayout(context)

    /** Bilah judul (bingkai) / bilah kecil (tanpa bingkai) tempat menyeret papan. */
    private val titleBar = LinearLayout(context)
    private val titleLabel = AppCompatTextView(context)
    private val minimize = KeyButton(context)

    /** Sudut kanan bawah untuk mengubah ukuran papan. */
    val resizeHandle = View(context)

    /**
     * Ruang di bawah papan di dalam area keyboard. Papan diangkat ke atas dengan
     * **menumbuhkan ruang ini**: jendela keyboard bertambah tinggi, papan yang menempel
     * di bagian atasnya ikut naik — jadi papan bisa dipindah ke mana saja tanpa pernah
     * keluar dari area keyboard.
     */
    private val liftSpacer = View(context)

    val root = PenboardRootView(context)
    val board = BoardView(context)

    /** Papan angka/huruf/tanda baca. Menutupi panel saat fungsi Papan karakter dipakai. */
    private val charPad = CharPad(context)

    /** Sisi tombol yang sedang dipakai (px) — dipakai layar pengaturan untuk mengubah ukuran. */
    var cellPx: Int = 0
        private set

    /** Jumlah tombol atas yang diminta pengaturan, dan yang benar-benar tampil di baris ini. */
    var bottomRequestedCount: Int = 0
        private set
    var bottomShownCount: Int = 0
        private set

    /** Baris tombol atas yang sedang tampil (0 = pertama) dan jumlah baris yang diatur. */
    var topPage: Int = 0
        private set
    var topRowCount: Int = 1
        private set

    /** Perkiraan berapa tombol persegi yang muat di satu baris (dipakai editor slot). */
    var topButtonsPerRow: Int = 0
        private set

    /** Berapa tombol yang kebagian halaman dengan jumlah baris sekarang. */
    var topReachableCount: Int = 0
        private set

    /** Ukuran papan yang sedang dipakai (px). */
    var panelWidthPx: Int = 0
        private set
    var panelHeightPx: Int = 0
        private set

    /** Posisi papan: jarak dari tepi kiri area keyboard (px). */
    var panelLeftPx: Int = 0
        private set

    /** Seberapa tinggi papan diangkat dari bawah area keyboard (px). */
    var panelLiftPx: Int = 0
        private set

    /** Pengaturan terakhir, dipakai saat lebar area keyboard baru diketahui. */
    private var lastConfig: PenboardConfig = PenboardConfig()
    private var lastHeightPx = 0
    private var lastWidthPx = 0
    private var lastCell = 0
    private var lastSpacing = 0
    private var lastUsable = 0
    private var lastWritingRight = 0
    private var lastScale = 1f
    private var lastPages: List<List<Int>> = emptyList()
    private var pageChangePosted = false

    /** Panah ganti baris: bukan tombol yang bisa diatur, hanya pengalih halaman. */
    private val rowUp = KeyButton(context)
    private val rowDown = KeyButton(context)
    private val topPageLabel = AppCompatTextView(context)
    private val topBarHost = object : FrameLayout(context) {
        private var downX = 0f
        private var downY = 0f

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (topRowCount <= 1) return false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x
                    downY = ev.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.y - downY
                    val dx = ev.x - downX
                    val slop = 24f * density
                    if (abs(dy) > slop && abs(dy) > abs(dx) * 1.3f) return true
                }
            }
            return false
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (topRowCount <= 1) return false
            if (ev.actionMasked == MotionEvent.ACTION_UP) {
                val dy = ev.y - downY
                val slop = 24f * density
                // Jari ke atas = baris berikutnya (isi bergeser ke atas), sama seperti scroll.
                if (dy <= -slop) stepTopPage(1) else if (dy >= slop) stepTopPage(-1)
            }
            return true
        }
    }

    /** Batas angkat papan (px) menurut tinggi area keyboard yang tersedia saat ini. */
    val maxLiftPx: Int
        get() {
            val allowed = root.maxHeightPx
            if (allowed <= 0) return MAX_LIFT_UNKNOWN_PX
            return (allowed - panelHeightPx).coerceAtLeast(0)
        }

    init {
        topBarHost.clipChildren = true
        topBarHost.clipToPadding = true
        topPageLabel.gravity = Gravity.CENTER
        topPageLabel.maxLines = 1
        topPageLabel.typeface = Typeface.MONOSPACE
        board.onStrokeStarted = { listener?.onStrokeStarted() }
        board.onStrokeCommitted = { listener?.onStrokeCommitted() }
        board.onContentChanged = { listener?.onContentChanged() }
        board.onPenButtonPrimary = { listener?.onPenButton(true) }
        board.onPenButtonSecondary = { listener?.onPenButton(false) }
        board.edgeToEdge = true

        // Lebar area keyboard (atau kartu pratinjau) baru diketahui setelah tata letak
        // pertama. Kalau papan lebih lebar dari induknya, kolom tombol di tepi kanan
        // akan terpotong — jadi ukurannya dihitung ulang dengan lebar yang sebenarnya.
        root.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val width = view.width
            if (width > 0 && width != lastWidthPx) {
                applyConfig(lastConfig, lastHeightPx, width)
            }
        }

        // Tombol grid dibuat sekali; penyusunannya (kolom x baris) dilakukan tiap
        // pengaturan diterapkan supaya jumlah kolom bisa berubah.
        while (buttons.size < PenboardConfig.GRID_SLOTS) {
            buttons.add(KeyButton(context))
        }
        while (bottomButtons.size < PenboardConfig.BOTTOM_SLOTS) {
            bottomButtons.add(KeyButton(context))
        }

        gridContainer.orientation = LinearLayout.VERTICAL
        gridContainer.gravity = Gravity.TOP or Gravity.END

        boardColumn.orientation = LinearLayout.VERTICAL
        boardColumn.addView(
            board,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        )

        panelRow.orientation = LinearLayout.HORIZONTAL
        // Rata atas: papan dan kolom tombol mulai dari garis yang sama, dan tingginya
        // memang sama, jadi sejajar atas maupun bawah.
        panelRow.gravity = Gravity.TOP
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

        // Menarik bilah judul = memindahkan papan.
        titleBar.setOnTouchListener { view, event -> handleDrag(view, event, resize = false) }

        // ---------------------------------------------------------- sudut pengubah ukuran
        resizeHandle.contentDescription = context.getString(R.string.action_resize)
        resizeHandle.background = ResizeCornerDrawable()
        resizeHandle.setOnTouchListener { view, event -> handleDrag(view, event, resize = true) }

        bottomContainer.orientation = LinearLayout.VERTICAL
        bottomContainer.gravity = Gravity.START

        // Urutan panel (dari atas): bilah judul -> tombol bawah -> area tulis + kolom tombol.
        // Tombol bawah sengaja **di atas** area tulis, sesuai permintaan.
        panel.orientation = LinearLayout.VERTICAL
        panel.addView(titleBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
        panel.addView(
            bottomContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        panel.addView(
            panelRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        charPad.visibility = View.GONE
        charPad.onInsert = { text ->
            listener?.onKey(KeyDef(action = KeyAction.TEXT, text = text), KeyAction.TEXT)
        }
        charPad.onAction = { action ->
            listener?.onKey(KeyDef(action = action), action)
        }
        charPad.onClose = { closeCharPad() }

        root.addView(
            panel,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        root.addView(liftSpacer, FrameLayout.LayoutParams(1, 0))
        root.addView(resizeHandle, FrameLayout.LayoutParams(0, 0))
        root.addView(charPad, FrameLayout.LayoutParams(0, 0))
    }

    /** Buka atau tutup papan karakter. Tombol yang sama menukar keadaannya. */
    fun toggleCharPad() {
        if (charPad.visibility == View.VISIBLE) {
            closeCharPad()
        } else {
            openCharPad()
        }
    }

    /** Tutup papan karakter dan kembalikan area tulis. Aman dipanggil bila sudah tertutup. */
    fun closeCharPad() {
        if (charPad.visibility != View.VISIBLE) return
        charPad.visibility = View.GONE
        applyResizeHandle(lastConfig)
    }

    private fun openCharPad() {
        val width = panelWidthPx.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels
        charPad.visibility = View.VISIBLE
        charPad.elevation = 12f * density
        charPad.applyStyle(lastConfig, width)
        resizeHandle.visibility = View.GONE
        placeCharPad()
        charPad.bringToFront()
    }

    /** Samakan posisi papan karakter dengan panel, termasuk saat panel digeser. */
    private fun placeCharPad() {
        val width = panelWidthPx.coerceAtLeast(1)
        val height = panelHeightPx.coerceAtLeast(1)
        charPad.layoutParams = FrameLayout.LayoutParams(width, height).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = panelLeftPx
            topMargin = 0
        }
    }

    /**
     * Terapkan seluruh pengaturan ke papan dan tombol.
     *
     * [availableHeightPx] adalah tinggi yang disediakan keyboard (0 = belum diketahui);
     * [availableWidthPx] adalah lebar area keyboard/kartu pratinjau (0 = pakai lebar
     * tampilan). Papan dipangkas otomatis supaya bingkai + bilah judul + bilah tombol
     * atas tidak pernah terpotong, dan kolom tombol tidak melewati tepi kanan.
     */
    fun applyConfig(
        config: PenboardConfig,
        availableHeightPx: Int = 0,
        availableWidthPx: Int = 0
    ) {
        lastConfig = config
        lastHeightPx = availableHeightPx
        val spacing = (GridMath.SPACING_DP * density).toInt()
        // Lebar sebenarnya (area keyboard atau kartu pratinjau) lebih dipercaya daripada
        // lebar layar: di jendela terbagi / layar berponi, area keyboard bisa lebih sempit.
        val screenWidth = when {
            availableWidthPx > 0 -> availableWidthPx
            root.width > 0 -> root.width
            else -> context.resources.displayMetrics.widthPixels
        }
        lastWidthPx = screenWidth
        val padding = (3 * density).toInt()
        val minCell = (GridMath.MIN_TOUCH_DP * density).toInt()
        val barHeight = (config.titleBarHeightDp() * density).toInt()
        val showBar = config.showsTitleBar
        val columns = GridMath.columnsOr(config.gridColumns)
        val lines = GridMath.linesOr(config.boardLines)
        // Baris tombol samping: mengikuti area tulis (bawaan) atau diatur sendiri.
        val rows = GridMath.rowsOr(config.effectiveGridRows)

        // ---------------------------------------------------------------- ukuran papan
        val panelWidth = (screenWidth * config.panelWidthFraction.coerceIn(PenboardConfig.MIN_PANEL_WIDTH_FRACTION, 1f))
            .toInt()
            .coerceIn((240 * density).toInt().coerceAtMost(screenWidth), screenWidth)
        val minBoardWidth = (72 * density).toInt()
        // Lebar yang bisa dipakai isi panel (tanpa padding bingkai).
        val usableWidth = (panelWidth - 2 * padding).coerceAtLeast(1)
        // Lebar area tulis yang dijamin: kolom tombol mengambil sisanya, jadi seluruh
        // tombol (termasuk kolom ketiga) selalu terlihat utuh.
        val boardWidth = GridMath.boardWidthFor(
            usableWidthPx = usableWidth,
            fraction = config.boardWidthFraction,
            minBoardWidthPx = minBoardWidth,
            gridMinWidthPx = GridMath.gridWidthPx(columns, minCell, spacing),
            spacingPx = spacing
        )
        val lineGapRequested = (config.lineGapDp * density).toInt().coerceAtLeast(1)
        val titleBlock = if (showBar) barHeight + spacing else 0
        val bottomCount = bottomCount(config)
        val bottomWidths = bottomMultipliers(config)
        val bottomHeightMultipliers = bottomHeights(config)

        // Ukuran tombol samping. Tombol bawah memakai ukuran itu (dikali pengali per tombol),
        // jadi tinggi bilah bawah ikut berubah saat ukuran tombol diubah.
        var lineGap = lineGapRequested
        val maxCellByWidth = GridMath.maxCellByWidthPx(usableWidth, boardWidth, columns, spacing)
        var cell = if (config.gridCellDp > 0) {
            // Ukuran pilihan pengguna tetap dipangkas supaya kolom tombol tidak terpotong.
            (config.gridCellDp * density).toInt().coerceIn(1, maxCellByWidth)
        } else {
            GridMath.cellSizePx(
                boardHeightPx = GridMath.boardHeightForLines(lines, lineGapRequested),
                availableWidthPx = usableWidth,
                spacingPx = spacing,
                minCellPx = minCell,
                // Ukuran otomatis = tinggi satu baris tulisan, berapa pun baris tombolnya.
                rows = lines,
                columns = columns,
                minBoardWidthPx = boardWidth
            )
        }
        var bottomRows: List<List<Int>> = emptyList()
        var bottomRowHeights: List<Int> = emptyList()
        // 1 = sesuai pilihan pengguna. Hanya mengecil bila panel benar-benar tidak muat
        // (mis. 5 tombol atas dengan ukuran tombol manual yang besar).
        var bottomScale = 1f
        fun barCell(): Int = (cell * bottomScale).roundToInt().coerceAtLeast(1)
        fun layoutBottom() {
            val gridW = GridMath.gridWidthPx(columns, cell, spacing)
            val writingRight = (usableWidth - spacing - gridW).coerceAtLeast(1)
            val measured = measureTopBar(config, barCell(), spacing, usableWidth, writingRight)
            // Selalu satu baris yang terlihat, setinggi tombol tertinggi di semua halaman
            // supaya pindah baris tidak mengubah tinggi papan atau memotong tombol.
            bottomRows = if (measured.rowHeight <= 0) emptyList() else listOf(measured.shown)
            bottomRowHeights = if (measured.rowHeight <= 0) emptyList() else listOf(measured.rowHeight)
            bottomRequestedCount = bottomCount
            bottomShownCount = measured.shown.size
        }

        // Dua putaran: ukuran tombol menentukan tinggi bilah bawah, dan tinggi bilah bawah
        // menentukan sisa ruang untuk papan. Hasilnya tidak pernah saling menimpa.
        repeat(2) {
            layoutBottom()
            val bottomBar = GridMath.bottomBarHeightPx(bottomRowHeights, spacing)
            // Bagian yang tidak bisa mengecil: padding bingkai, bilah judul, bilah bawah.
            val bottomSpacing = if (bottomBar > 0) spacing else 0
            val fixed = 2 * padding + titleBlock + bottomBar + bottomSpacing
            // Jarak baris dipangkas bila layar tidak cukup untuk semua baris area tulis.
            lineGap = GridMath.fitLineGapPx(lineGapRequested, availableHeightPx, lines, fixed)
            // Tombol samping boleh setinggi isi panel yang tersisa (rata atas saja).
            val maxCellByHeight = if (availableHeightPx > 0) {
                val content = (availableHeightPx - fixed).coerceAtLeast(1)
                ((content - spacing * (rows - 1)) / rows).coerceAtLeast(1)
            } else {
                Int.MAX_VALUE
            }
            val maxCell = minOf(maxCellByHeight, maxCellByWidth)
            cell = if (config.gridCellDp > 0) {
                (config.gridCellDp * density).toInt().coerceIn(1, maxCell)
            } else {
                GridMath.cellSizePx(
                    boardHeightPx = GridMath.boardHeightForLines(lines, lineGap),
                    availableWidthPx = usableWidth,
                    spacingPx = spacing,
                    minCellPx = minCell,
                    rows = lines,
                    columns = columns,
                    minBoardWidthPx = boardWidth
                ).coerceAtMost(maxCell)
            }.coerceAtLeast(1)
        }

        // ---------------------------------------------------------------- koreksi akhir
        // Susunan bilah bawah dihitung ulang dengan ukuran tombol **final**: putaran
        // terakhir bisa mengubah cell setelah barisnya sempat dihitung, dan jumlah
        // baris (juga tinggi bilah) ikut berubah. Tanpa ini, tinggi panel bisa
        // kurang dari kenyataan dan bilah bawah terpotong.
        layoutBottom()

        // Tinggi bilah bawah ikut berubah saat ukuran tombol berubah (putaran di atas),
        // jadi bisa tersisa sedikit kelebihan. Pangkas jarak baris (dan tombol otomatis)
        // sampai seluruh panel benar-benar muat di ruang keyboard.
        var boardHeight = GridMath.boardHeightForLines(lines, lineGap)
        var gridHeight = GridMath.boardHeightPx(rows, cell, spacing)
        var bottomBarHeight = GridMath.bottomBarHeightPx(bottomRowHeights, spacing)
        var bottomSpacing = if (bottomBarHeight > 0) spacing else 0
        fun totalHeight(): Int = 2 * padding + titleBlock + bottomBarHeight + bottomSpacing +
            maxOf(boardHeight, gridHeight)
        fun measureBottom() {
            layoutBottom()
            bottomBarHeight = GridMath.bottomBarHeightPx(bottomRowHeights, spacing)
            bottomSpacing = if (bottomBarHeight > 0) spacing else 0
        }
        var guard = 0
        while (availableHeightPx > 0 && totalHeight() > availableHeightPx && guard < 12) {
            guard++
            // Area tulis lebih penting daripada bilah tombol: bila panel terlalu tinggi,
            // kecilkan dulu bilah bawah (sampai 40%) sebelum memangkas area menulis.
            if (bottomBarHeight > 0 && bottomScale > 0.4f) {
                bottomScale = (bottomScale - 0.1f).coerceAtLeast(0.4f)
                measureBottom()
                continue
            }
            val excess = totalHeight() - availableHeightPx
            val shrink = ((excess + lines - 1) / lines).coerceAtLeast(1)
            lineGap = (lineGap - shrink).coerceAtLeast(1)
            boardHeight = GridMath.boardHeightForLines(lines, lineGap)
            if (config.gridCellDp <= 0) {
                cell = GridMath.cellSizePx(
                    boardHeightPx = boardHeight,
                    availableWidthPx = usableWidth,
                    spacingPx = spacing,
                    minCellPx = minCell,
                    rows = lines,
                    columns = columns,
                    minBoardWidthPx = boardWidth
                ).coerceAtMost(cell).coerceAtLeast(1)
                gridHeight = GridMath.boardHeightPx(rows, cell, spacing)
            }
            // Batas keras: berapa pun pilihan pengguna, kolom tombol tidak boleh
            // melebihi ruang yang tersisa setelah bingkai, judul, dan bilah bawah.
            if (availableHeightPx > 0) {
                val room = (availableHeightPx - (2 * padding + titleBlock + bottomBarHeight +
                    bottomSpacing)).coerceAtLeast(1)
                val hardMax = ((room - spacing * (rows - 1)) / rows).coerceAtLeast(1)
                if (cell > hardMax) {
                    cell = hardMax
                    gridHeight = GridMath.boardHeightPx(rows, cell, spacing)
                }
            }
            measureBottom()
        }

        val gridWidth = GridMath.gridWidthPx(columns, cell, spacing)
        val panelHeight = totalHeight()

        cellPx = cell
        panelWidthPx = panelWidth
        panelHeightPx = panelHeight

        // ---------------------------------------------------------------- bingkai & latar
        panel.setPadding(padding, padding, padding, padding)
        panel.layoutParams = FrameLayout.LayoutParams(panelWidth, panelHeight)
        panelBackground.radiusPx = config.cornerRadiusDp * density
        panelBackground.fillColor =
            BoardView.withAlpha(config.panelColor, config.panelOpacity * 255 / 100)
        panelBackground.borderColor = BoardView.withAlpha(
            config.accentColor,
            if (config.windowFrame) 190 else 120
        )
        panelBackground.borderWidthPx = if (config.windowFrame) 1.5f * density else 1f
        panelBackground.titleBarHeightPx =
            if (config.windowFrame) (barHeight + padding).toFloat() else 0f
        panelBackground.titleBarColor = BoardView.withAlpha(config.accentColor, 30)
        panelBackground.dividerColor = BoardView.withAlpha(config.accentColor, 150)
        panelBackground.scanlines = config.scanlines
        panelBackground.lineSpacingPx = 3f * density
        panel.background = panelBackground

        // ---------------------------------------------------------------- bilah judul / bilah kecil
        titleBar.visibility = if (showBar) View.VISIBLE else View.GONE
        // Jarak ke elemen berikutnya diatur oleh elemen itu sendiri (bilah bawah atau
        // deretan papan), supaya tidak pernah ada jarak ganda.
        titleBar.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            barHeight
        )
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

        // ---------------------------------------------------------------- papan tulis
        board.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, boardHeight)
        board.lineGapPx = lineGap.toFloat()
        board.penColor = config.currentPenColor
        board.penWidthPx = config.penWidthDp * density
        board.allowFinger = config.allowFinger
        board.pressureEnabled = config.pressureEnabled
        board.showLines = config.showLines
        board.lineNumbers = config.lineOffsetEnabled
        board.dotLines = config.scanlines
        board.haptics = config.haptics
        board.edgeToEdge = true
        board.cornerRadiusPx = config.cornerRadiusDp * density
        board.boardColor = config.boardColor
        board.boardAlpha = config.boardOpacity * 255 / 100
        board.lineColor = config.lineColor
        board.accentColor = config.accentColor

        val hasBottomBar = bottomCount(config) > 0
        panelRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = if (hasBottomBar || showBar) spacing else 0 }

        // ---------------------------------------------------------------- kolom tombol (persegi)
        buildGrid(config, columns, rows, cell, spacing)

        gridContainer.layoutParams = LinearLayout.LayoutParams(
            gridWidth,
            gridHeight
        ).apply { marginStart = spacing }

        val writingRight = (usableWidth - spacing - gridWidth).coerceAtLeast(1)
        applyBottomBar(config, cell, spacing, usableWidth, bottomScale, writingRight)
        applyResizeHandle(config)
        // Ukuran berubah => posisi harus dipasang ulang supaya sudut pengubah ukuran
        // tetap menempel di pojok kanan bawah papan.
        placePanel(panelLeftPx, panelLiftPx)
        if (charPad.visibility == View.VISIBLE) {
            resizeHandle.visibility = View.GONE
            charPad.applyStyle(lastConfig, panelWidthPx.coerceAtLeast(1))
            charPad.bringToFront()
        }
    }

    /**
     * Hasil pengukuran nyata untuk bukti di kartu pratinjau:
     * (jumlah baris tombol yang terpasang, jumlah baris area tulis).
     *
     * Baris tombol harus sama dengan baris tulisan; kesejajaran **atas** dijamin tata
     * letak (keduanya mulai di garis yang sama), jadi yang perlu dibuktikan angkanya
     * adalah jumlah barisnya.
     */
    fun measuredRows(): Pair<Int, Int> = gridContainer.childCount to board.lineCount

    /** Susun ulang tombol grid sesuai jumlah kolom & baris yang dipilih. */
    private fun buildGrid(
        config: PenboardConfig,
        columns: Int,
        rows: Int,
        cell: Int,
        spacing: Int
    ) {
        // Tombol grid dipakai ulang setiap pengaturan diterapkan. Sebuah View hanya boleh
        // punya SATU induk: kalau tombol masih menempel di baris lama, addView() ke baris
        // baru melempar IllegalStateException ("child already has a parent"). Itulah yang
        // membuat aplikasi force close begitu pengaturan berubah / keyboard muncul.
        for (button in buttons) (button.parent as? ViewGroup)?.removeView(button)
        gridContainer.removeAllViews()
        val defaults = PenboardConfig.defaultGridKeys()
        val slots = GridMath.slots(columns, rows)
        for (rowIndex in 0 until rows) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            for (column in 0 until columns) {
                val index = rowIndex * columns + column
                val button = buttons[index]
                val key = config.gridKeys.getOrNull(index) ?: defaults.getOrElse(index) { defaults.last() }
                val enabled = index < config.gridButtonCount
                button.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
                button.repeatOnHold = key.repeatOnHold
                bindTap(button, key)
                button.contentDescription = describeKey(config, key)
                button.layoutParams = LinearLayout.LayoutParams(cell, cell).apply {
                    marginStart = if (column == 0) 0 else spacing
                }
                button.style(
                    radiusPx = config.cornerRadiusDp * density * 0.9f,
                    fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
                    borderColor = BoardView.withAlpha(config.accentColor, 130),
                    labelColor = config.buttonTextColor,
                    textSizePx = cell * 0.26f,
                    haptics = config.haptics
                )
                applyIcon(button, config, key, fraction = 0.62f)
                row.addView(button)
            }
            gridContainer.addView(
                row,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, cell).apply {
                    topMargin = if (rowIndex == 0) 0 else spacing
                }
            )
        }
        // Tombol di luar susunan sekarang disembunyikan supaya tidak ikut terukur.
        for (index in slots until buttons.size) buttons[index].visibility = View.GONE
    }

    /**
     * Tombol **tepat di atas area menulis** (masih di dalam bingkai jendela).
     *
     * Hanya satu baris yang dipasang. Bila pengguna menambah baris, sisanya tidak
     * ditumpuk: panah atas/bawah duduk tepat di tepi kanan area tulis dan mengganti
     * baris. Geser vertikal pada bilah melakukan hal yang sama.
     */
    private fun applyBottomBar(
        config: PenboardConfig,
        cell: Int,
        spacing: Int,
        availableWidth: Int,
        scale: Float = 1f,
        writingRight: Int = availableWidth
    ) {
        lastCell = cell
        lastSpacing = spacing
        lastUsable = availableWidth
        lastScale = scale
        lastWritingRight = writingRight
        // Sama seperti tombol grid: lepaskan dulu dari baris lama sebelum disusun ulang.
        for (button in bottomButtons) (button.parent as? ViewGroup)?.removeView(button)
        (rowUp.parent as? ViewGroup)?.removeView(rowUp)
        (rowDown.parent as? ViewGroup)?.removeView(rowDown)
        (topPageLabel.parent as? ViewGroup)?.removeView(topPageLabel)
        (topBarHost.parent as? ViewGroup)?.removeView(topBarHost)
        topBarHost.removeAllViews()
        bottomContainer.removeAllViews()
        val count = bottomCount(config)
        if (count <= 0) {
            bottomContainer.visibility = View.GONE
            return
        }
        bottomContainer.visibility = View.VISIBLE
        bottomContainer.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = if (config.showsTitleBar) spacing else 0 }

        val side = (cell * scale).roundToInt().coerceAtLeast(1)
        val measured = measureTopBar(config, side, spacing, availableWidth, writingRight)
        val defaults = PenboardConfig.defaultBottomKeys()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // Rata kiri, jarak sama seperti tombol samping. Lebar baris berhenti sebelum
        // panah supaya tombol tidak menutupi tepi kanan area tulis.
        val rowWidth = if (measured.arrows != null) measured.buttonWidth else ViewGroup.LayoutParams.MATCH_PARENT
        for ((position, index) in measured.shown.withIndex()) {
            val key = config.bottomKeys.getOrNull(index)
                ?: defaults.getOrElse(index) { defaults.last() }
            row.addView(
                buildBottomButton(config, key, side, if (position == 0) 0 else spacing, index)
            )
        }
        topBarHost.addView(
            row,
            FrameLayout.LayoutParams(rowWidth, measured.rowHeight).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
        )
        val arrows = measured.arrows
        if (arrows != null) {
            val cluster = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            topPageLabel.text = "${measured.page + 1}/${measured.rowCount}"
            topPageLabel.setTextColor(config.buttonTextColor)
            topPageLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, (arrows.label * 0.38f).coerceAtLeast(8f))
            topPageLabel.contentDescription = context.getString(
                R.string.desc_top_page,
                measured.page + 1,
                measured.rowCount
            )
            cluster.addView(
                topPageLabel,
                LinearLayout.LayoutParams(arrows.label, ViewGroup.LayoutParams.MATCH_PARENT)
            )
            styleArrow(
                rowUp, config, arrows.side, spacing, KeyAction.CURSOR_UP,
                enabled = measured.page > 0,
                delta = -1,
                description = context.getString(R.string.desc_row_up)
            )
            styleArrow(
                rowDown, config, arrows.side, spacing, KeyAction.CURSOR_DOWN,
                enabled = measured.page < measured.rowCount - 1,
                delta = 1,
                description = context.getString(R.string.desc_row_down)
            )
            cluster.addView(rowUp)
            cluster.addView(rowDown)
            val arrowLeft = (measured.writingRight - arrows.block).coerceAtLeast(0)
            topBarHost.addView(
                cluster,
                FrameLayout.LayoutParams(
                    arrows.block,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    leftMargin = arrowLeft
                }
            )
        }
        bottomContainer.addView(
            topBarHost,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                measured.rowHeight
            )
        )
    }

    /** Pindah baris tombol atas. Tidak mengubah tinggi papan. */
    private fun stepTopPage(delta: Int) {
        showTopPage(topPage + delta)
    }

    /**
     * Tampilkan baris [page]. Dipakai panah, geser, dan setelah memasang tombol baru.
     * Penyusunan ulang dijadwalkan supaya tidak mengubah hierarki di tengah sentuhan.
     */
    fun showTopPage(page: Int) {
        if (lastCell <= 0) {
            topPage = page.coerceAtLeast(0)
            return
        }
        val next = page.coerceIn(0, (topRowCount - 1).coerceAtLeast(0))
        if (next == topPage) return
        topPage = next
        if (pageChangePosted) return
        pageChangePosted = true
        topBarHost.post {
            pageChangePosted = false
            applyBottomBar(lastConfig, lastCell, lastSpacing, lastUsable, lastScale, lastWritingRight)
            listener?.onTopPageChanged()
        }
    }

    /** Halaman perkiraan untuk slot [index], termasuk yang belum muat di baris sekarang. */
    fun topPageFor(index: Int): Int {
        if (index < 0) return 0
        for ((page, indexes) in lastPages.withIndex()) {
            if (index in indexes) return page
        }
        val per = topButtonsPerRow.coerceAtLeast(1)
        return index / per
    }

    private data class ArrowMetrics(val side: Int, val label: Int, val block: Int)

    private data class TopBarMeasure(
        val pages: List<List<Int>>,
        val page: Int,
        val rowCount: Int,
        val shown: List<Int>,
        val rowHeight: Int,
        val buttonWidth: Int,
        val writingRight: Int,
        val arrows: ArrowMetrics?,
        val perRow: Int
    )

    /**
     * Satu sumber hitungan untuk tinggi panel dan pemasangan tombol, supaya keduanya
     * tidak berbeda. Panah hanya ada bila baris lebih dari satu.
     */
    private fun measureTopBar(
        config: PenboardConfig,
        side: Int,
        spacing: Int,
        usableWidth: Int,
        writingRight: Int
    ): TopBarMeasure {
        val count = bottomCount(config)
        val rowCount = config.bottomRowCount.coerceIn(
            PenboardConfig.MIN_BOTTOM_ROWS,
            PenboardConfig.MAX_BOTTOM_ROWS
        )
        topRowCount = rowCount
        if (topPage >= rowCount) topPage = (rowCount - 1).coerceAtLeast(0)
        if (topPage < 0) topPage = 0
        val showArrows = rowCount > 1 && count > 0
        val arrows = if (showArrows) arrowMetrics(side, spacing, writingRight) else null
        val buttonWidth = if (arrows != null) {
            (writingRight - arrows.block - spacing).coerceAtLeast(1)
        } else {
            usableWidth.coerceAtLeast(1)
        }
        val pages = GridMath.packTopPages(
            count,
            rowCount,
            side,
            bottomMultipliers(config),
            buttonWidth,
            spacing
        )
        lastPages = pages
        topReachableCount = pages.sumOf { it.size }
        val shown = pages.getOrElse(topPage) { emptyList() }
        val heightIndexes = pages.flatten()
        val heights = bottomHeights(config)
        val rowHeight = when {
            count <= 0 -> 0
            heightIndexes.isEmpty() -> side
            else -> heightIndexes.maxOf { index ->
                GridMath.buttonHeightPx(side, heights.getOrElse(index) { 1f })
            }
        }
        val perRow = GridMath.bottomRowIndexes(
            PenboardConfig.MAX_BOTTOM_KEYS,
            side,
            List(PenboardConfig.MAX_BOTTOM_KEYS) { 1f },
            buttonWidth,
            spacing
        ).firstOrNull()?.size ?: 0
        topButtonsPerRow = perRow
        return TopBarMeasure(
            pages = pages,
            page = topPage,
            rowCount = rowCount,
            shown = shown,
            rowHeight = rowHeight,
            buttonWidth = buttonWidth,
            writingRight = writingRight,
            arrows = arrows,
            perRow = perRow
        )
    }

    /** Panah dikecilkan bila area tulis lebih sempit daripada dua tombol + label. */
    private fun arrowMetrics(side: Int, spacing: Int, writingRight: Int): ArrowMetrics {
        val label = (26f * density).roundToInt().coerceAtLeast(1)
        val full = GridMath.topArrowBlockPx(side, spacing, label)
        if (full <= writingRight) return ArrowMetrics(side, label, full)
        val compact = (18f * density).roundToInt().coerceAtLeast(1)
        val room = (writingRight - compact - spacing).coerceAtLeast(2)
        val shrunk = (room / 2).coerceIn(1, side)
        val block = GridMath.topArrowBlockPx(shrunk, spacing, compact)
            .coerceAtMost(writingRight.coerceAtLeast(1))
        return ArrowMetrics(shrunk, compact, block)
    }

    private fun styleArrow(
        button: KeyButton,
        config: PenboardConfig,
        side: Int,
        spacing: Int,
        action: KeyAction,
        enabled: Boolean,
        delta: Int,
        description: String
    ) {
        button.repeatOnHold = false
        button.visibility = View.VISIBLE
        button.alpha = if (enabled) 1f else 0.35f
        button.contentDescription = description
        // Jarak hanya di antara dua panah, supaya lebar blok sama dengan hitungan GridMath.
        button.layoutParams = LinearLayout.LayoutParams(side, side).apply {
            marginStart = if (delta < 0) 0 else spacing
        }
        button.style(
            radiusPx = config.cornerRadiusDp * density * 0.9f,
            fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
            borderColor = BoardView.withAlpha(config.accentColor, 130),
            labelColor = config.buttonTextColor,
            textSizePx = side * 0.26f,
            haptics = config.haptics
        )
        applyIcon(button, config, KeyDef(action), fraction = 0.62f)
        button.onTap = if (enabled) {
            { stepTopPage(delta) }
        } else {
            null
        }
    }

    /** Satu tombol bawah: tinggi = tombol samping, lebar = tinggi x [KeyDef.widthMultiplier]. */
    private fun buildBottomButton(
        config: PenboardConfig,
        key: KeyDef,
        side: Int,
        gapStart: Int,
        index: Int
    ): KeyButton = bottomButtons[index].apply {
        visibility = View.VISIBLE
        repeatOnHold = key.repeatOnHold
        bindTap(this, key)
        contentDescription = describeKey(config, key)
        val width = GridMath.buttonWidthPx(side, bottomMultiplier(config, index))
        val height = GridMath.buttonHeightPx(side, bottomHeight(config, index))
        layoutParams = LinearLayout.LayoutParams(width, height).apply {
            marginStart = gapStart
        }
        style(
            radiusPx = config.cornerRadiusDp * density * 0.9f,
            fillColor = BoardView.withAlpha(config.buttonColor, config.buttonOpacity * 255 / 100),
            borderColor = BoardView.withAlpha(config.accentColor, 130),
            labelColor = config.buttonTextColor,
            textSizePx = minOf(width, height) * 0.26f,
            haptics = config.haptics
        )
        applyIcon(this, config, key, fraction = 0.62f)
    }

    /**
     * Letakkan papan: [leftPx] dari tepi kiri dan [liftPx] ke atas dari bawah area keyboard.
     *
     * Papan selalu menempel di bagian atas area keyboard; tingginya area itu yang
     * bertambah ([liftSpacer]) sehingga papan naik di layar. Dipakai keyboard (mode
     * melayang: papan boleh di mana saja) dan kartu pratinjau.
     * Sudut pengubah ukuran ikut dipindah supaya selalu menempel di pojok papan.
     */
    fun placePanel(leftPx: Int, liftPx: Int) {
        // Sebelum area keyboard diukur (lebar 0) posisi dipakai apa adanya, supaya papan
        // yang sudah dipindah tidak melompat ke pojok saat digambar ulang.
        val left =
            if (root.width > 0) leftPx.coerceIn(0, (root.width - panelWidthPx).coerceAtLeast(0)) else leftPx
        val lift = liftPx.coerceIn(0, maxLiftPx)
        panelLeftPx = left
        panelLiftPx = lift
        panel.layoutParams = FrameLayout.LayoutParams(panelWidthPx, panelHeightPx).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = left
            topMargin = 0
        }
        if (lift > 0) {
            liftSpacer.visibility = View.VISIBLE
            liftSpacer.layoutParams = FrameLayout.LayoutParams(1, lift).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = panelHeightPx
            }
        } else {
            liftSpacer.visibility = View.GONE
        }
        val side = (16 * density).toInt()
        val inset = (2 * density).toInt()
        (resizeHandle.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.gravity = Gravity.TOP or Gravity.START
            params.width = side
            params.height = side
            params.leftMargin = left + panelWidthPx - side - inset
            params.topMargin = panelHeightPx - side - inset
            resizeHandle.layoutParams = params
        }
        placeCharPad()
    }

    /** Jumlah tombol bawah yang ditampilkan (0 = bilah bawah disembunyikan). */
    private fun bottomCount(config: PenboardConfig): Int =
        config.bottomButtonCount.coerceIn(0, PenboardConfig.BOTTOM_SLOTS)

    /** Pengali lebar tombol bawah ke-[index] (1x = persegi). */
    private fun bottomMultiplier(config: PenboardConfig, index: Int): Float {
        val defaults = PenboardConfig.defaultBottomKeys()
        val key = config.bottomKeys.getOrNull(index)
            ?: defaults.getOrElse(index) { defaults.last() }
        return key.widthMultiplier
    }

    /** Pengali lebar semua tombol bawah yang tampil, dipakai untuk menghitung barisnya. */
    private fun bottomMultipliers(config: PenboardConfig): List<Float> =
        (0 until bottomCount(config)).map { index -> bottomMultiplier(config, index) }

    /** Pengali tinggi tombol bawah ke-[index] (1x = setinggi tombol samping). */
    private fun bottomHeight(config: PenboardConfig, index: Int): Float {
        val defaults = PenboardConfig.defaultBottomKeys()
        val key = config.bottomKeys.getOrNull(index)
            ?: defaults.getOrElse(index) { defaults.last() }
        return key.heightMultiplier
    }

    /** Pengali tinggi semua tombol bawah yang tampil. */
    private fun bottomHeights(config: PenboardConfig): List<Float> =
        (0 until bottomCount(config)).map { index -> bottomHeight(config, index) }

    /** Sudut kanan bawah: petunjuk bahwa ukuran papan bisa diubah dengan menariknya. */
    private fun applyResizeHandle(config: PenboardConfig) {
        val side = (16 * density).toInt()
        resizeHandle.visibility = if (config.windowFrame || config.minimizeButton) View.VISIBLE else View.GONE
        (resizeHandle.background as? ResizeCornerDrawable)?.apply {
            color = BoardView.withAlpha(config.accentColor, 190)
            strokePx = 1.6f * density
        }
        // Posisinya diatur [placePanel] supaya selalu di pojok kanan bawah papan.
        val current = resizeHandle.layoutParams as? FrameLayout.LayoutParams
        resizeHandle.layoutParams = (current ?: FrameLayout.LayoutParams(side, side)).apply {
            width = side
            height = side
        }
    }

    /** Fungsi Papan karakter membuka papan; aksi lain dilaporkan ke [Listener]. */
    private fun bindTap(button: KeyButton, key: KeyDef) {
        button.onTap = {
            if (key.action == KeyAction.CHAR_PAD) toggleCharPad() else listener?.onKey(key, key.action)
        }
    }

    /** Keterangan aksesibilitas: tombol karakter menyebut teks yang dimasukkan. */
    private fun describeKey(config: PenboardConfig, key: KeyDef): String =
        if (key.action == KeyAction.TEXT && key.text.isNotEmpty()) {
            key.action.label + " " + key.text
        } else {
            config.labelFor(key)
        }

    /**
     * Pasang ikon pada [button] mengikuti gaya pilihan pengguna ([PenboardConfig.iconStyle]):
     * **pixel art** 12 x 12 ([PixelArt]) atau **ikon vektor** ([KeyIcons]).
     * Tombol karakter yang sudah diisi menampilkan karakternya, bukan ikon generik.
     */
    private fun applyIcon(button: KeyButton, config: PenboardConfig, key: KeyDef, fraction: Float) {
        val face = if (key.action == KeyAction.TEXT) KeyboardChars.faceOf(key.label, key.text) else null
        if (face != null) {
            button.showPixels(null, config.buttonTextColor)
            button.showGlyph(null, config.buttonTextColor)
            button.text = face
            button.maxLines = if (face.length <= 3) 1 else 2
            val params = button.layoutParams
            val side = minOf(
                params?.width?.takeIf { it > 0 } ?: button.width,
                params?.height?.takeIf { it > 0 } ?: button.height
            )
            if (side > 0) {
                button.setTextSize(TypedValue.COMPLEX_UNIT_PX, KeyboardChars.faceTextPx(side, face))
            }
            return
        }
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
     * Tarikan pada bingkai atas (memindah) atau sudut kanan bawah (mengubah ukuran).
     * Selisihnya dilaporkan ke [Listener]; jarak terkecil 2 px supaya tidak bergetar.
     */
    private fun handleDrag(view: View, event: MotionEvent, resize: Boolean): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                view.isPressed = true
                lastX = event.rawX
                lastY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX).roundToInt()
                val dy = (event.rawY - lastY).roundToInt()
                if (abs(dx) < DRAG_STEP_PX && abs(dy) < DRAG_STEP_PX) return true
                lastX = event.rawX
                lastY = event.rawY
                if (resize) listener?.onResizeBy(dx, dy) else listener?.onMoveBy(dx, dy)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
                listener?.onGestureFinished()
            }
            else -> return false
        }
        return true
    }

    private var lastX = 0f
    private var lastY = 0f

    companion object {
        /** Jarak minimum sebelum tarikan dianggap gerakan (px). */
        private const val DRAG_STEP_PX = 2

        /**
         * Batas angkat sebelum tinggi area keyboard terukur: cukup longgar supaya tarikan
         * pertama tidak langsung tertahan, tetapi tetap dibatasi ukuran layar.
         */
        private const val MAX_LIFT_UNKNOWN_PX = 4000
    }
}
