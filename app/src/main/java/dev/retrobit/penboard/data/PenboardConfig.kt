package dev.retrobit.penboard.data

import dev.retrobit.penboard.ink.RecognitionLanguages
import dev.retrobit.penboard.util.GridMath
import org.json.JSONArray
import org.json.JSONObject

/**
 * Aksi yang bisa dipasang pada tombol keyboard maupun tombol samping pena.
 *
 * [label] hanya dipakai sebagai cadangan bila ikonnya tidak digambar; semua aksi
 * yang punya ikon digambar sebagai ikon di papan tombol (lihat `ui/KeyIcons.kt`).
 */
enum class KeyAction(val id: String, val label: String) {
    // Papan tulis & pengenalan
    TEXT("text", "abc"),
    RECOGNIZE("recognize", "Teks"),
    CLEAR("clear", "Bersih"),
    DELETE_STROKE("deleteStroke", "Hapus gores"),
    UNDO("undo", "Batal"),
    REDO("redo", "Ulangi"),
    CYCLE_PEN("cyclePen", "Warna"),

    // Tombol keyboard konvensional
    SPACE("space", "Spasi"),
    ENTER("enter", "Enter"),
    BACKSPACE("backspace", "Hapus"),
    DELETE_WORD("deleteWord", "Hapus kata"),
    DELETE_FORWARD("deleteForward", "Hapus depan"),
    TAB("tab", "Tab"),

    // Penempatan kursor
    CURSOR_LEFT("cursorLeft", "Kiri"),
    CURSOR_RIGHT("cursorRight", "Kanan"),
    CURSOR_UP("cursorUp", "Atas"),
    CURSOR_DOWN("cursorDown", "Bawah"),
    CURSOR_HOME("cursorHome", "Awal baris"),
    CURSOR_END("cursorEnd", "Akhir baris"),

    // Papan klip & pilihan
    SELECT_ALL("selectAll", "Pilih semua"),
    CUT("cut", "Potong"),
    COPY("copy", "Salin"),
    PASTE("paste", "Tempel"),

    // Jembatan ke perangkat
    SYSTEM_HANDWRITING("systemHandwriting", "Huawei"),
    KEYBOARD_PICKER("keyboardPicker", "Pilih keyboard"),
    HIDE("hide", "Tutup");

    companion object {
        fun fromId(id: String?): KeyAction =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SPACE
    }
}

/** Aksi tombol samping pena stylus. */
/** Gaya ikon tombol papan: pixel art 12 x 12, atau ikon vektor bergaris. */
enum class IconStyle(val id: String, val label: String) {
    PIXEL("pixel", "Pixel art"),
    VECTOR("vector", "Vektor");

    companion object {
        fun fromId(id: String?): IconStyle =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: PIXEL
    }
}

enum class PenButtonAction(val id: String, val label: String, val key: KeyAction?) {
    NONE("none", "Tidak ada", null),
    RECOGNIZE("recognize", "Jadikan teks", KeyAction.RECOGNIZE),
    UNDO("undo", "Batal goresan", KeyAction.UNDO),
    DELETE_STROKE("deleteStroke", "Hapus goresan terakhir", KeyAction.DELETE_STROKE),
    CLEAR("clear", "Bersihkan papan", KeyAction.CLEAR),
    SYSTEM_HANDWRITING("systemHandwriting", "Tulisan tangan Huawei", KeyAction.SYSTEM_HANDWRITING),
    KEYBOARD_PICKER("keyboardPicker", "Pilih keyboard", KeyAction.KEYBOARD_PICKER);

    companion object {
        fun fromId(id: String?): PenButtonAction =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
    }
}

/** Satu tombol pada grid tombol (3 kolom, sampai 6 baris). */
data class KeyDef(
    val action: KeyAction = KeyAction.SPACE,
    /** Label khusus; kosong berarti memakai label bawaan aksi. */
    val label: String = "",
    /** Teks yang diinput untuk [KeyAction.TEXT]. */
    val text: String = "",
    /** Ulangi aksi selama tombol ditahan (mis. hapus). */
    val repeatOnHold: Boolean = false,
    /**
     * Lebar tombol **bawah** dibanding tingginya (1 = persegi). Tingginya selalu
     * mengikuti tombol samping; hanya lebarnya yang bisa diatur pengguna.
     */
    val widthMultiplier: Float = 1f,
    /**
     * Tinggi tombol **bawah** dibanding tombol samping (1 = setinggi tombol samping).
     * Lebar dan tingginya bisa diatur sendiri-sendiri.
     */
    val heightMultiplier: Float = 1f
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("action", action.id)
        put("label", label)
        put("text", text)
        put("repeat", repeatOnHold)
        put("widthMultiplier", widthMultiplier)
        put("heightMultiplier", heightMultiplier)
    }

    companion object {
        /** Lebar tombol bawah: 1x (persegi) sampai 4x tinggi tombol samping. */
        const val MIN_WIDTH_MULTIPLIER = GridMath.MIN_WIDTH_MULTIPLIER
        const val MAX_WIDTH_MULTIPLIER = GridMath.MAX_WIDTH_MULTIPLIER
        const val MIN_HEIGHT_MULTIPLIER = GridMath.MIN_HEIGHT_MULTIPLIER
        const val MAX_HEIGHT_MULTIPLIER = GridMath.MAX_HEIGHT_MULTIPLIER

        fun fromJson(json: JSONObject): KeyDef = KeyDef(
            action = KeyAction.fromId(json.optString("action")),
            label = json.optString("label"),
            text = json.optString("text"),
            widthMultiplier = json.optDouble("widthMultiplier", 1.0).toFloat()
                .coerceIn(MIN_WIDTH_MULTIPLIER, MAX_WIDTH_MULTIPLIER),
            heightMultiplier = json.optDouble("heightMultiplier", 1.0).toFloat()
                .coerceIn(MIN_HEIGHT_MULTIPLIER, MAX_HEIGHT_MULTIPLIER),
            repeatOnHold = json.optBoolean("repeat", false)
        )
    }
}

/**
 * Seluruh pengaturan Penboard (satu profil, disimpan sebagai JSON).
 * Semua nilai dibersihkan lewat [sanitized] agar tidak ada angka liar dari berkas lama.
 */
data class PenboardConfig(
    val schemaVersion: Int = SCHEMA_VERSION,

    // Papan tulis
    /** Jumlah baris area tulis (bawaan 3). Tingginya = baris x jarak garis panduan. */
    val boardLines: Int = GridMath.DEFAULT_LINES,
    val lineGapDp: Int = 44,
    val penWidthDp: Int = 3,
    val penColors: List<Int> = DEFAULT_PEN_COLORS,
    val penColorIndex: Int = 0,
    val showLines: Boolean = true,
    val allowFinger: Boolean = false,
    /** Ketebalan goresan mengikuti tekanan stylus (M-Pen dan pena sejenis). */
    val pressureEnabled: Boolean = true,

    // Grid tombol (3 kolom, jumlah baris bisa diatur 1-6)
    /** Jumlah kolom tombol samping (1-6). */
    val gridColumns: Int = GridMath.DEFAULT_COLUMNS,
    /** Baris tombol samping saat [gridRowsFollow] dimatikan (1-6). */
    val gridRows: Int = DEFAULT_ROWS,
    /**
     * `true` (bawaan): baris tombol samping selalu sama dengan baris area tulis.
     * `false`: baris tombol diatur sendiri lewat [gridRows] — boleh lebih banyak
     * daripada baris tulisan, mis. 3 kolom x 5 baris tombol untuk area tulis 3 baris.
     */
    val gridRowsFollow: Boolean = true,
    val gridButtonCount: Int = DEFAULT_ROWS * COLUMNS,
    val gridKeys: List<KeyDef> = defaultGridKeys(),
    val penButtonPrimary: PenButtonAction = PenButtonAction.RECOGNIZE,
    val penButtonSecondary: PenButtonAction = PenButtonAction.UNDO,

    // Tampilan & transparansi (0-100) — bawaan: tema retro cyberpunk grayscale ePaper
    val panelColor: Int = PANEL_COLOR,
    val panelOpacity: Int = 90,
    val boardColor: Int = PAPER_COLOR,
    val boardOpacity: Int = 97,
    val lineColor: Int = 0xFFB9B9B9.toInt(),
    val buttonColor: Int = BUTTON_COLOR,
    val buttonOpacity: Int = 92,
    val buttonTextColor: Int = 0xFFE6E6E6.toInt(),
    val accentColor: Int = ACCENT_COLOR,
    val cornerRadiusDp: Int = 6,
    val haptics: Boolean = true,
    /** Tekstur garis pindaian ala layar ePaper (bagian dari tema retro). */
    val scanlines: Boolean = true,

    // Pengenalan tulisan
    val autoRecognize: Boolean = true,

    /**
     * Baris tempat menulis menentukan posisi teks: baris pertama = di posisi kursor,
     * baris kedua = satu enter (satu baris di bawah kursor), baris ketiga = dua enter.
     */
    val lineOffsetEnabled: Boolean = true,
    val autoRecognizeDelayMs: Int = 2000,
    val autoSpace: Boolean = true,
    val clearAfterRecognize: Boolean = true,
    val recognitionLanguage: String = RecognitionLanguages.DEFAULT,
    /**
     * Bila mesin pengenalan bawaan tidak bisa dipakai (mis. perangkat Huawei tanpa
     * Google Play services), Penboard berpindah sendiri ke keyboard tulisan tangan
     * perangkat supaya pengguna tetap bisa menulis.
     */
    val autoSwitchToSystemHandwriting: Boolean = true,

    // ---------------------------------------------------------------- bilah bawah (pixel art)
    /** Tombol di **bawah papan tulis**; ikonnya pixel art (lihat `util/PixelArt.kt`). */
    val bottomKeys: List<KeyDef> = defaultBottomKeys(),
    /** Berapa tombol bawah yang tampil; 0 berarti bilah bawah disembunyikan. */
    val bottomButtonCount: Int = DEFAULT_BOTTOM_KEYS,

    /** Gaya ikon semua tombol papan: pixel art ([IconStyle.PIXEL]) atau vektor. */
    val iconStyle: IconStyle = IconStyle.PIXEL,

    /**
     * Sisi tombol samping (dp). `0` berarti **otomatis** (dihitung dari tinggi papan
     * dan lebar yang tersedia); selain itu [GridMath.MIN_CELL_DP]..[GridMath.MAX_CELL_DP].
     * Area tulis selalu mengikuti ukuran tombol ini.
     */
    val gridCellDp: Int = 0,

    /** Lebar papan dibanding lebar layar (0,45-1). Bisa juga diubah dengan menarik sudut bingkai. */
    val panelWidthFraction: Float = 1f,

    /**
     * Lebar area tulis dibanding lebar isi panel (0,40-0,80).
     *
     * Kolom tombol mengambil sisa ruangnya. Ukuran tombol otomatis mengecil bila perlu,
     * dan ukuran tombol manual dipangkas ke batas ini supaya **tidak ada tombol yang
     * terpotong di tepi panel**.
     */
    val boardWidthFraction: Float = GridMath.DEFAULT_BOARD_WIDTH_FRACTION,

    /** Posisi papan (0-1 dari area keyboard) saat dipindah dengan menarik bingkai atas. */
    val panelOffsetX: Float = 0f,
    val panelOffsetY: Float = 0f,

    /** Papan bisa digeser ke mana saja (mode melayang) dengan menarik bingkai atas. */
    val floatingPanel: Boolean = true,

    // ---------------------------------------------------------------- bingkai jendela
    /** Bingkai + bilah judul seperti jendela: ada tombol minimize untuk menyembunyikan papan. */
    val windowFrame: Boolean = true,
    /** Tampilkan tombol minimize di bilah judul. */
    val minimizeButton: Boolean = true,
    /** Judul di bilah jendela; kosong berarti memakai [DEFAULT_WINDOW_TITLE]. */
    val windowTitle: String = ""
) {

    /** Warna pena yang sedang dipakai. */
    val currentPenColor: Int
        get() = penColors.getOrElse(penColorIndex) { DEFAULT_PEN_COLORS.first() }

    /**
     * Pakai lagi tema **retro cyberpunk grayscale ePaper** (warna, transparansi, dan
     * bentuk saja) tanpa mengubah susunan tombol maupun pengaturan pengenalan.
     */
    fun withRetroTheme(): PenboardConfig = copy(
        panelColor = PANEL_COLOR,
        panelOpacity = THEME_PANEL_OPACITY,
        boardColor = PAPER_COLOR,
        boardOpacity = THEME_BOARD_OPACITY,
        lineColor = THEME_LINE_COLOR,
        buttonColor = BUTTON_COLOR,
        buttonOpacity = THEME_BUTTON_OPACITY,
        buttonTextColor = THEME_BUTTON_TEXT,
        accentColor = ACCENT_COLOR,
        penColors = DEFAULT_PEN_COLORS,
        penColorIndex = 0,
        cornerRadiusDp = THEME_RADIUS_DP,
        scanlines = true
    )

    /**
     * Preset **HiWrite** (gaya Huawei): satu ketukan untuk menyalakan seluruh saran
     * ala HiWrite — papan tulis 4 baris (tombol samping jadi 4x3 = 12 tombol berikon),
     * tema retro cyberpunk grayscale ePaper, dan input otomatis 2 detik.
     *
     * Pengaturan pribadi yang tidak berhubungan dibiarkan apa adanya: bahasa tulisan
     * dan tombol samping pena.
     */
    fun withHiWritePreset(): PenboardConfig = withRetroTheme().copy(
        // Papan tulis: 4 baris supaya enak ditulis, pena 3 dp.
        boardLines = 4,
        lineGapDp = 48,
        penWidthDp = 3,
        showLines = true,
        allowFinger = false,
        pressureEnabled = true,
        // Tombol: 3 kolom, barisnya mengikuti area tulis (4 baris = 12 tombol), ukuran otomatis.
        gridColumns = COLUMNS,
        gridRows = 4,
        gridRowsFollow = true,
        gridButtonCount = COLUMNS * 4,
        gridCellDp = 0,
        gridKeys = defaultGridKeys(),
        // Jendela & bilah bawah: bingkai + minimize, serta 5 tombol pixel art di bawah papan.
        iconStyle = IconStyle.PIXEL,
        windowFrame = true,
        minimizeButton = true,
        bottomKeys = defaultBottomKeys(),
        bottomButtonCount = DEFAULT_BOTTOM_KEYS,
        // Pengenalan: kata jadi teks otomatis setelah 2 detik, lalu papan bersih.
        autoRecognize = true,
        autoRecognizeDelayMs = 2000,
        autoSpace = true,
        clearAfterRecognize = true,
        haptics = true
    ).sanitized()

    /** Label tombol: label khusus, teks aksi TEXT, atau label bawaan aksi. */
    fun labelFor(key: KeyDef): String = when {
        key.label.isNotBlank() -> key.label
        key.action == KeyAction.TEXT && key.text.isNotBlank() -> key.text
        else -> key.action.label
    }

    /** Judul yang benar-benar dipakai di bilah jendela. */
    val effectiveWindowTitle: String
        get() = windowTitle.trim().ifBlank { DEFAULT_WINDOW_TITLE }.take(MAX_WINDOW_TITLE)

    /**
     * Apakah baris atas papan perlu tampil: bilah judul penuh ([windowFrame]), atau
     * **bilah kecil** berisi tombol minimize saja saat bingkai dimatikan.
     */
    val showsTitleBar: Boolean
        get() = windowFrame || minimizeButton

    /**
     * Jumlah baris tombol samping yang benar-benar dipakai.
     *
     * Bila [gridRowsFollow] aktif (bawaan), angkanya mengikuti baris area tulis;
     * selain itu memakai [gridRows] yang diatur sendiri (1-6).
     */
    val effectiveGridRows: Int
        get() = if (gridRowsFollow) {
            boardLines.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS)
        } else {
            gridRows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS)
        }

    /** Tinggi baris atas papan (dp); 0 berarti tidak ada. */
    fun titleBarHeightDp(): Int = when {
        windowFrame -> TITLE_BAR_DP
        minimizeButton -> MINI_BAR_DP
        else -> 0
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("schemaVersion", schemaVersion)
        put("boardLines", boardLines)
        put("lineGapDp", lineGapDp)
        put("penWidthDp", penWidthDp)
        put("penColors", JSONArray().apply { penColors.forEach { put(it.toLong() and 0xFFFFFFFFL) } })
        put("penColorIndex", penColorIndex)
        put("showLines", showLines)
        put("allowFinger", allowFinger)
        put("pressureEnabled", pressureEnabled)
        put("gridColumns", gridColumns)
        put("gridRows", gridRows)
        put("gridRowsFollow", gridRowsFollow)
        put("gridButtonCount", gridButtonCount)
        put("gridKeys", JSONArray().apply { gridKeys.forEach { put(it.toJson()) } })
        put("penButtonPrimary", penButtonPrimary.id)
        put("penButtonSecondary", penButtonSecondary.id)
        putColor("panelColor", panelColor)
        put("panelOpacity", panelOpacity)
        putColor("boardColor", boardColor)
        put("boardOpacity", boardOpacity)
        putColor("lineColor", lineColor)
        putColor("buttonColor", buttonColor)
        put("buttonOpacity", buttonOpacity)
        putColor("buttonTextColor", buttonTextColor)
        putColor("accentColor", accentColor)
        put("cornerRadiusDp", cornerRadiusDp)
        put("haptics", haptics)
        put("scanlines", scanlines)
        put("autoRecognize", autoRecognize)
        put("lineOffsetEnabled", lineOffsetEnabled)
        put("autoRecognizeDelayMs", autoRecognizeDelayMs)
        put("autoSpace", autoSpace)
        put("clearAfterRecognize", clearAfterRecognize)
        put("recognitionLanguage", recognitionLanguage)
        put("autoSwitchToSystemHandwriting", autoSwitchToSystemHandwriting)
        put("bottomKeys", JSONArray().apply { bottomKeys.forEach { put(it.toJson()) } })
        put("bottomButtonCount", bottomButtonCount)
        put("iconStyle", iconStyle.id)
        put("gridCellDp", gridCellDp)
        put("panelWidthFraction", panelWidthFraction)
        put("boardWidthFraction", boardWidthFraction)
        put("panelOffsetX", panelOffsetX)
        put("panelOffsetY", panelOffsetY)
        put("floatingPanel", floatingPanel)
        put("windowFrame", windowFrame)
        put("minimizeButton", minimizeButton)
        put("windowTitle", windowTitle)
    }

    /** Semua angka dipangkas ke rentang yang masuk akal. */
    fun sanitized(): PenboardConfig {
        val colors = penColors.map { it }.distinct().take(MAX_PEN_COLORS).ifEmpty { DEFAULT_PEN_COLORS }
        val columns = gridColumns.coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
        val lines = boardLines.coerceIn(MIN_BOARD_LINES, MAX_BOARD_LINES)
        // Baris tombol: mengikuti area tulis (bawaan) atau angka sendiri 1-6.
        val rows = if (gridRowsFollow) lines else gridRows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS)
        return copy(
            boardLines = boardLines.coerceIn(MIN_BOARD_LINES, MAX_BOARD_LINES),
            lineGapDp = lineGapDp.coerceIn(MIN_LINE_GAP_DP, MAX_LINE_GAP_DP),
            penWidthDp = penWidthDp.coerceIn(MIN_PEN_WIDTH_DP, MAX_PEN_WIDTH_DP),
            penColors = colors,
            penColorIndex = penColorIndex.coerceIn(0, colors.size - 1),
            gridColumns = columns,
            // Angka pilihan pengguna disimpan walau sedang mode mengikuti area tulis,
            // supaya saat sakelarnya dimatikan pilihannya tidak hilang.
            gridRows = gridRows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS),
            // Tombol tidak boleh melebihi jumlah sel yang tersedia di grid yang dipilih.
            gridButtonCount = gridButtonCount.coerceIn(MIN_GRID_KEYS, rows * columns),
            gridKeys = sanitizeKeys(gridKeys, defaultGridKeys(), GRID_SLOTS),
            gridCellDp = if (gridCellDp <= 0) 0 else gridCellDp.coerceIn(MIN_CELL_DP, MAX_CELL_DP),
            panelWidthFraction = panelWidthFraction.coerceIn(MIN_PANEL_WIDTH_FRACTION, 1f),
            boardWidthFraction = GridMath.boardFractionOr(boardWidthFraction),
            panelOffsetX = panelOffsetX.coerceIn(0f, 1f),
            panelOffsetY = panelOffsetY.coerceIn(0f, 1f),
            panelOpacity = panelOpacity.coerceIn(0, 100),
            boardOpacity = boardOpacity.coerceIn(0, 100),
            buttonOpacity = buttonOpacity.coerceIn(0, 100),
            cornerRadiusDp = cornerRadiusDp.coerceIn(MIN_RADIUS_DP, MAX_RADIUS_DP),
            autoRecognizeDelayMs = autoRecognizeDelayMs.coerceIn(MIN_AUTO_DELAY_MS, MAX_AUTO_DELAY_MS),
            recognitionLanguage = RecognitionLanguages.normalize(recognitionLanguage),
            bottomKeys = sanitizeKeys(bottomKeys, defaultBottomKeys(), BOTTOM_SLOTS),
            bottomButtonCount = bottomButtonCount.coerceIn(MIN_BOTTOM_KEYS, MAX_BOTTOM_KEYS),
            windowTitle = windowTitle.trim().take(MAX_WINDOW_TITLE)
        )
    }

    /** Isi setiap slot: pakai isian pengguna, atau bawaan yang sesuai untuk slot itu. */
    private fun sanitizeKeys(keys: List<KeyDef>, defaults: List<KeyDef>, slots: Int): List<KeyDef> {
        val result = ArrayList<KeyDef>(slots)
        for (index in 0 until slots) {
            // Slot yang belum pernah diisi memakai bawaan bergiliran, bukan tombol
            // terakhir terus-menerus — menambah tombol ke-9 tetap memberi aksi berguna.
            val fallback = if (defaults.isEmpty()) KeyDef() else defaults[index % defaults.size]
            val key = keys.getOrNull(index) ?: fallback
            result.add(
                key.copy(
                    widthMultiplier = key.widthMultiplier
                        .coerceIn(KeyDef.MIN_WIDTH_MULTIPLIER, KeyDef.MAX_WIDTH_MULTIPLIER),
                    heightMultiplier = key.heightMultiplier
                        .coerceIn(KeyDef.MIN_HEIGHT_MULTIPLIER, KeyDef.MAX_HEIGHT_MULTIPLIER)
                )
            )
        }
        return result
    }

    private fun JSONObject.putColor(name: String, color: Int) {
        put(name, color.toLong() and 0xFFFFFFFFL)
    }

    companion object {
        const val SCHEMA_VERSION = 1

        /** Kolom 1-6 dan baris 1-6: sampai 36 tombol, semuanya persegi. */
        const val COLUMNS = GridMath.DEFAULT_COLUMNS
        const val MIN_GRID_COLUMNS = GridMath.MIN_COLUMNS
        const val MAX_GRID_COLUMNS = GridMath.MAX_COLUMNS
        const val MIN_GRID_ROWS = GridMath.MIN_ROWS
        const val MAX_GRID_ROWS = GridMath.MAX_ROWS
        const val DEFAULT_ROWS = GridMath.ROWS
        const val GRID_SLOTS = MAX_GRID_COLUMNS * MAX_GRID_ROWS

        /** Ukuran tombol: 0 = otomatis, selain itu dp. */
        const val MIN_CELL_DP = GridMath.MIN_CELL_DP
        const val MAX_CELL_DP = GridMath.MAX_CELL_DP

        /** Lebar papan dibanding lebar layar. */
        const val MIN_PANEL_WIDTH_FRACTION = 0.45f

        /** Lebar area tulis dibanding lebar isi panel; kolom tombol mengambil sisanya. */
        const val MIN_BOARD_WIDTH_FRACTION = GridMath.MIN_BOARD_WIDTH_FRACTION
        const val MAX_BOARD_WIDTH_FRACTION = GridMath.MAX_BOARD_WIDTH_FRACTION
        const val DEFAULT_BOARD_WIDTH_FRACTION = GridMath.DEFAULT_BOARD_WIDTH_FRACTION
        const val MAX_PEN_COLORS = 6

        const val MIN_BOARD_LINES = GridMath.MIN_LINES
        const val MAX_BOARD_LINES = GridMath.MAX_LINES
        const val DEFAULT_BOARD_LINES = GridMath.DEFAULT_LINES
        const val MIN_LINE_GAP_DP = 28
        const val MAX_LINE_GAP_DP = 76
        const val MIN_PEN_WIDTH_DP = 1
        const val MAX_PEN_WIDTH_DP = 12
        const val MIN_GRID_KEYS = 1
        const val MAX_GRID_KEYS = GRID_SLOTS

        /**
         * Bilah tombol di atas papan tulis: 0 (disembunyikan) sampai **30 tombol**.
         *
         * Ukuran dasarnya mengikuti tombol samping, jadi jumlah yang **benar-benar muat**
         * bergantung pada ukuran itu: tombol samping 32 dp memberi sekitar 10 tombol per
         * baris, sementara tombol 96 dp hanya sekitar 3. Tombol yang tidak muat turun ke
         * baris berikutnya (tetap rata kiri, jaraknya sama dengan tombol samping).
         */
        const val MIN_BOTTOM_KEYS = 0
        const val MAX_BOTTOM_KEYS = 30
        const val BOTTOM_SLOTS = MAX_BOTTOM_KEYS
        const val DEFAULT_BOTTOM_KEYS = 5

        /** Tinggi bilah judul jendela (dp) dan bilah kecil minimize tanpa bingkai. */
        const val TITLE_BAR_DP = 22
        const val MINI_BAR_DP = 18

        /** Bingkai jendela: judul bawaan & panjang maksimumnya. */
        const val DEFAULT_WINDOW_TITLE = "PENBOARD"
        const val MAX_WINDOW_TITLE = 20
        const val MIN_RADIUS_DP = 0
        const val MAX_RADIUS_DP = 32
        const val MIN_AUTO_DELAY_MS = 500
        const val MAX_AUTO_DELAY_MS = 5000

        // Tema retro cyberpunk grayscale ePaper.
        const val PANEL_COLOR = 0xFF0B0B0B.toInt()
        const val PAPER_COLOR = 0xFFEDEDED.toInt()
        const val BUTTON_COLOR = 0xFF161616.toInt()
        const val ACCENT_COLOR = 0xFFC8C8C8.toInt()

        /** Tinta ePaper: hitam, grafit, lalu abu. */
        val DEFAULT_PEN_COLORS: List<Int> = listOf(
            0xFF101010.toInt(),
            0xFF3F3F3F.toInt(),
            0xFF7A7A7A.toInt()
        )

        /**
         * Grid bawaan ala HiWrite: baris pertama-tama panah kursor, lalu tombol
         * keyboard konvensional, lalu tombol papan tulis.
         */
        /**
         * Bilah **atas papan** bawaan: aksi papan tulis yang paling sering dipakai,
         * digambar dengan ikon pixel art ([dev.retrobit.penboard.util.PixelArt]).
         * Slot di atas 8 memakai daftar ini secara bergiliran.
         */
        fun defaultBottomKeys(): List<KeyDef> = listOf(
            KeyDef(KeyAction.RECOGNIZE),
            // Bukan TEXT: tombol teks tanpa isi tidak melakukan apa-apa, jadi bawaan
            // harus aksi yang jelas berguna (enter = baris baru).
            KeyDef(KeyAction.ENTER),
            KeyDef(KeyAction.DELETE_STROKE),
            KeyDef(KeyAction.CLEAR),
            KeyDef(KeyAction.UNDO),
            KeyDef(KeyAction.REDO),
            KeyDef(KeyAction.CYCLE_PEN),
            KeyDef(KeyAction.HIDE)
        )

        fun defaultGridKeys(): List<KeyDef> = listOf(
            // Baris 1 - penempatan kursor
            KeyDef(KeyAction.CURSOR_LEFT, repeatOnHold = true),
            KeyDef(KeyAction.CURSOR_UP, repeatOnHold = true),
            KeyDef(KeyAction.CURSOR_RIGHT, repeatOnHold = true),
            // Baris 2 - tombol keyboard konvensional
            KeyDef(KeyAction.CURSOR_DOWN, repeatOnHold = true),
            KeyDef(KeyAction.SPACE),
            KeyDef(KeyAction.ENTER),
            // Baris 3 - hapus & papan tulis
            KeyDef(KeyAction.BACKSPACE, repeatOnHold = true),
            KeyDef(KeyAction.RECOGNIZE),
            KeyDef(KeyAction.CLEAR),
            // Baris 4 - tambahan
            KeyDef(KeyAction.DELETE_WORD, repeatOnHold = true),
            KeyDef(KeyAction.UNDO),
            KeyDef(KeyAction.REDO),
            // Baris 5 - sisa tombol keyboard konvensional
            KeyDef(KeyAction.DELETE_FORWARD, repeatOnHold = true),
            KeyDef(KeyAction.TAB),
            KeyDef(KeyAction.CYCLE_PEN),
            // Baris 6 - tulisan tangan perangkat & pemilih keyboard
            KeyDef(KeyAction.SYSTEM_HANDWRITING),
            KeyDef(KeyAction.KEYBOARD_PICKER),
            KeyDef(KeyAction.HIDE),
            // Slot cadangan (dipakai bila kolom/baris ditambah): papan klip & sisa aksi.
            KeyDef(KeyAction.SELECT_ALL),
            KeyDef(KeyAction.CUT),
            KeyDef(KeyAction.COPY),
            KeyDef(KeyAction.PASTE),
            KeyDef(KeyAction.CURSOR_HOME, repeatOnHold = true),
            KeyDef(KeyAction.CURSOR_END, repeatOnHold = true),
            KeyDef(KeyAction.DELETE_WORD, repeatOnHold = true),
            KeyDef(KeyAction.DELETE_FORWARD, repeatOnHold = true),
            KeyDef(KeyAction.TAB),
            KeyDef(KeyAction.CLEAR),
            KeyDef(KeyAction.RECOGNIZE),
            KeyDef(KeyAction.UNDO),
            KeyDef(KeyAction.REDO),
            KeyDef(KeyAction.CYCLE_PEN),
            KeyDef(KeyAction.SYSTEM_HANDWRITING),
            KeyDef(KeyAction.KEYBOARD_PICKER),
            KeyDef(KeyAction.SELECT_ALL),
            KeyDef(KeyAction.PASTE)
        )

        /** Nilai tema retro dipakai juga oleh [withRetroTheme]. */
        const val THEME_PANEL_OPACITY = 90
        const val THEME_BOARD_OPACITY = 97
        const val THEME_BUTTON_OPACITY = 92
        const val THEME_LINE_COLOR = 0xFFB9B9B9.toInt()
        const val THEME_BUTTON_TEXT = 0xFFE6E6E6.toInt()
        const val THEME_RADIUS_DP = 6

        fun fromJson(json: JSONObject): PenboardConfig {
            val base = PenboardConfig()
            val colors = json.optJSONArray("penColors")?.let { array ->
                (0 until array.length()).map { index -> (array.optLong(index) and 0xFFFFFFFFL).toInt() }
            } ?: base.penColors
            val keys = json.optJSONArray("gridKeys")?.let { array ->
                (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(KeyDef::fromJson) }
            } ?: base.gridKeys
            val bottomKeys = json.optJSONArray("bottomKeys")?.let { array ->
                (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(KeyDef::fromJson) }
            } ?: base.bottomKeys
            return PenboardConfig(
                schemaVersion = json.optInt("schemaVersion", SCHEMA_VERSION),
                boardLines = json.optInt("boardLines", base.boardLines),
                lineGapDp = json.optInt("lineGapDp", base.lineGapDp),
                penWidthDp = json.optInt("penWidthDp", base.penWidthDp),
                penColors = colors,
                penColorIndex = json.optInt("penColorIndex", base.penColorIndex),
                showLines = json.optBoolean("showLines", base.showLines),
                allowFinger = json.optBoolean("allowFinger", base.allowFinger),
                pressureEnabled = json.optBoolean("pressureEnabled", base.pressureEnabled),
                gridColumns = json.optInt("gridColumns", base.gridColumns),
                gridRows = json.optInt("gridRows", base.gridRows),
                gridRowsFollow = json.optBoolean("gridRowsFollow", base.gridRowsFollow),
                gridButtonCount = json.optInt("gridButtonCount", base.gridButtonCount),
                gridKeys = keys,
                penButtonPrimary = PenButtonAction.fromId(json.optString("penButtonPrimary", base.penButtonPrimary.id)),
                penButtonSecondary = PenButtonAction.fromId(json.optString("penButtonSecondary", base.penButtonSecondary.id)),
                panelColor = json.color("panelColor", base.panelColor),
                panelOpacity = json.optInt("panelOpacity", base.panelOpacity),
                boardColor = json.color("boardColor", base.boardColor),
                boardOpacity = json.optInt("boardOpacity", base.boardOpacity),
                lineColor = json.color("lineColor", base.lineColor),
                buttonColor = json.color("buttonColor", base.buttonColor),
                buttonOpacity = json.optInt("buttonOpacity", base.buttonOpacity),
                buttonTextColor = json.color("buttonTextColor", base.buttonTextColor),
                accentColor = json.color("accentColor", base.accentColor),
                cornerRadiusDp = json.optInt("cornerRadiusDp", base.cornerRadiusDp),
                haptics = json.optBoolean("haptics", base.haptics),
                scanlines = json.optBoolean("scanlines", base.scanlines),
                autoRecognize = json.optBoolean("autoRecognize", base.autoRecognize),
                lineOffsetEnabled = json.optBoolean("lineOffsetEnabled", base.lineOffsetEnabled),
                autoRecognizeDelayMs = json.optInt("autoRecognizeDelayMs", base.autoRecognizeDelayMs),
                autoSpace = json.optBoolean("autoSpace", base.autoSpace),
                clearAfterRecognize = json.optBoolean("clearAfterRecognize", base.clearAfterRecognize),
                recognitionLanguage = json.optString("recognitionLanguage", base.recognitionLanguage),
                autoSwitchToSystemHandwriting = json.optBoolean(
                    "autoSwitchToSystemHandwriting",
                    base.autoSwitchToSystemHandwriting
                ),
                bottomKeys = bottomKeys,
                bottomButtonCount = json.optInt("bottomButtonCount", base.bottomButtonCount),
                iconStyle = IconStyle.fromId(json.optString("iconStyle", base.iconStyle.id)),
                gridCellDp = json.optInt("gridCellDp", base.gridCellDp),
                panelWidthFraction = json.optDouble("panelWidthFraction", base.panelWidthFraction.toDouble()).toFloat(),
                boardWidthFraction = json.optDouble("boardWidthFraction", base.boardWidthFraction.toDouble()).toFloat(),
                panelOffsetX = json.optDouble("panelOffsetX", base.panelOffsetX.toDouble()).toFloat(),
                panelOffsetY = json.optDouble("panelOffsetY", base.panelOffsetY.toDouble()).toFloat(),
                floatingPanel = json.optBoolean("floatingPanel", base.floatingPanel),
                windowFrame = json.optBoolean("windowFrame", base.windowFrame),
                minimizeButton = json.optBoolean("minimizeButton", base.minimizeButton),
                windowTitle = json.optString("windowTitle", base.windowTitle)
            ).sanitized()
        }

        private fun JSONObject.color(name: String, fallback: Int): Int =
            (optLong(name, fallback.toLong()) and 0xFFFFFFFFL).toInt()
    }
}
