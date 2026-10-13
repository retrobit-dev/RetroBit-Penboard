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
    val repeatOnHold: Boolean = false
) {
    /** Judul yang benar-benar dipakai di bilah jendela. */
    val effectiveWindowTitle: String
        get() = windowTitle.trim().ifBlank { DEFAULT_WINDOW_TITLE }.take(MAX_WINDOW_TITLE)

    fun toJson(): JSONObject = JSONObject().apply {
        put("action", action.id)
        put("label", label)
        put("text", text)
        put("repeat", repeatOnHold)
    }

    companion object {
        fun fromJson(json: JSONObject): KeyDef = KeyDef(
            action = KeyAction.fromId(json.optString("action")),
            label = json.optString("label"),
            text = json.optString("text"),
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
    val boardHeightDp: Int = 176,
    val lineGapDp: Int = 44,
    val penWidthDp: Int = 3,
    val penColors: List<Int> = DEFAULT_PEN_COLORS,
    val penColorIndex: Int = 0,
    val showLines: Boolean = true,
    val allowFinger: Boolean = false,
    /** Ketebalan goresan mengikuti tekanan stylus (M-Pen dan pena sejenis). */
    val pressureEnabled: Boolean = true,

    // Grid tombol (3 kolom, jumlah baris bisa diatur 1-6)
    val gridRows: Int = DEFAULT_ROWS,
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
     * ala HiWrite — papan tulis tinggi, 6 baris tombol berikon (18 tombol), tema
     * retro cyberpunk grayscale ePaper, dan input otomatis 2 detik.
     *
     * Pengaturan pribadi yang tidak berhubungan dibiarkan apa adanya: bahasa tulisan,
     * tombol samping pena, dan tinggi papan tulis.
     */
    fun withHiWritePreset(): PenboardConfig = withRetroTheme().copy(
        // Papan tulis: lebih tinggi supaya enak ditulis, pena 3 dp.
        boardHeightDp = 200,
        lineGapDp = 48,
        penWidthDp = 3,
        showLines = true,
        allowFinger = false,
        pressureEnabled = true,
        // Tombol: 6 baris x 3 kolom = 18 tombol berikon.
        gridRows = MAX_GRID_ROWS,
        gridButtonCount = MAX_GRID_KEYS,
        gridKeys = defaultGridKeys(),
        // Jendela & bilah bawah: bingkai + minimize, serta 5 tombol pixel art di bawah papan.
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

    fun toJson(): JSONObject = JSONObject().apply {
        put("schemaVersion", schemaVersion)
        put("boardHeightDp", boardHeightDp)
        put("lineGapDp", lineGapDp)
        put("penWidthDp", penWidthDp)
        put("penColors", JSONArray().apply { penColors.forEach { put(it.toLong() and 0xFFFFFFFFL) } })
        put("penColorIndex", penColorIndex)
        put("showLines", showLines)
        put("allowFinger", allowFinger)
        put("pressureEnabled", pressureEnabled)
        put("gridRows", gridRows)
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
        put("autoRecognizeDelayMs", autoRecognizeDelayMs)
        put("autoSpace", autoSpace)
        put("clearAfterRecognize", clearAfterRecognize)
        put("recognitionLanguage", recognitionLanguage)
        put("autoSwitchToSystemHandwriting", autoSwitchToSystemHandwriting)
        put("bottomKeys", JSONArray().apply { bottomKeys.forEach { put(it.toJson()) } })
        put("bottomButtonCount", bottomButtonCount)
        put("windowFrame", windowFrame)
        put("minimizeButton", minimizeButton)
        put("windowTitle", windowTitle)
    }

    /** Semua angka dipangkas ke rentang yang masuk akal. */
    fun sanitized(): PenboardConfig {
        val colors = penColors.map { it }.distinct().take(MAX_PEN_COLORS).ifEmpty { DEFAULT_PEN_COLORS }
        val rows = gridRows.coerceIn(MIN_GRID_ROWS, MAX_GRID_ROWS)
        return copy(
            boardHeightDp = boardHeightDp.coerceIn(MIN_BOARD_HEIGHT_DP, MAX_BOARD_HEIGHT_DP),
            lineGapDp = lineGapDp.coerceIn(MIN_LINE_GAP_DP, MAX_LINE_GAP_DP),
            penWidthDp = penWidthDp.coerceIn(MIN_PEN_WIDTH_DP, MAX_PEN_WIDTH_DP),
            penColors = colors,
            penColorIndex = penColorIndex.coerceIn(0, colors.size - 1),
            gridRows = rows,
            // Tombol tidak boleh melebihi jumlah sel yang tersedia di baris yang dipilih.
            gridButtonCount = gridButtonCount.coerceIn(MIN_GRID_KEYS, rows * COLUMNS),
            gridKeys = sanitizeKeys(gridKeys),
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

    private fun sanitizeKeys(keys: List<KeyDef>): List<KeyDef> =
        sanitizeKeys(keys, defaultGridKeys(), GRID_SLOTS)

    /** Isi setiap slot: pakai isian pengguna, atau bawaan yang sesuai untuk slot itu. */
    private fun sanitizeKeys(keys: List<KeyDef>, defaults: List<KeyDef>, slots: Int): List<KeyDef> {
        val result = ArrayList<KeyDef>(slots)
        for (index in 0 until slots) {
            result.add(keys.getOrNull(index) ?: defaults.getOrElse(index) { defaults.last() })
        }
        return result
    }

    private fun JSONObject.putColor(name: String, color: Int) {
        put(name, color.toLong() and 0xFFFFFFFFL)
    }

    companion object {
        const val SCHEMA_VERSION = 1

        /** Tiga kolom tetap; jumlah baris bisa 1 sampai 6 (18 tombol). */
        const val COLUMNS = GridMath.COLUMNS
        const val MIN_GRID_ROWS = GridMath.MIN_ROWS
        const val MAX_GRID_ROWS = GridMath.MAX_ROWS
        const val DEFAULT_ROWS = GridMath.ROWS
        const val GRID_SLOTS = COLUMNS * MAX_GRID_ROWS
        const val MAX_PEN_COLORS = 6

        const val MIN_BOARD_HEIGHT_DP = 120
        const val MAX_BOARD_HEIGHT_DP = 340
        const val MIN_LINE_GAP_DP = 28
        const val MAX_LINE_GAP_DP = 76
        const val MIN_PEN_WIDTH_DP = 1
        const val MAX_PEN_WIDTH_DP = 12
        const val MIN_GRID_KEYS = 1
        const val MAX_GRID_KEYS = GRID_SLOTS

        /** Bilah tombol di bawah papan tulis: 0 (disembunyikan) sampai 8 tombol. */
        const val MIN_BOTTOM_KEYS = 0
        const val MAX_BOTTOM_KEYS = 8
        const val BOTTOM_SLOTS = MAX_BOTTOM_KEYS
        const val DEFAULT_BOTTOM_KEYS = 5

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
         * Bilah **bawah papan** bawaan: aksi papan tulis yang paling sering dipakai,
         * digambar dengan ikon pixel art ([dev.retrobit.penboard.util.PixelArt]).
         */
        fun defaultBottomKeys(): List<KeyDef> = listOf(
            KeyDef(KeyAction.RECOGNIZE),
            KeyDef(KeyAction.TEXT),
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
            KeyDef(KeyAction.HIDE)
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
                boardHeightDp = json.optInt("boardHeightDp", base.boardHeightDp),
                lineGapDp = json.optInt("lineGapDp", base.lineGapDp),
                penWidthDp = json.optInt("penWidthDp", base.penWidthDp),
                penColors = colors,
                penColorIndex = json.optInt("penColorIndex", base.penColorIndex),
                showLines = json.optBoolean("showLines", base.showLines),
                allowFinger = json.optBoolean("allowFinger", base.allowFinger),
                pressureEnabled = json.optBoolean("pressureEnabled", base.pressureEnabled),
                gridRows = json.optInt("gridRows", base.gridRows),
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
                windowFrame = json.optBoolean("windowFrame", base.windowFrame),
                minimizeButton = json.optBoolean("minimizeButton", base.minimizeButton),
                windowTitle = json.optString("windowTitle", base.windowTitle)
            ).sanitized()
        }

        private fun JSONObject.color(name: String, fallback: Int): Int =
            (optLong(name, fallback.toLong()) and 0xFFFFFFFFL).toInt()
    }
}
