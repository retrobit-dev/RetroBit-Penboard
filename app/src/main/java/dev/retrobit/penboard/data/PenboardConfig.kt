package dev.retrobit.penboard.data

import dev.retrobit.penboard.ink.RecognitionLanguages
import org.json.JSONArray
import org.json.JSONObject

/** Aksi yang bisa dipasang pada tombol keyboard maupun tombol samping pena. */
enum class KeyAction(val id: String, val label: String) {
    TEXT("text", "abc"),
    SPACE("space", "Spasi"),
    ENTER("enter", "Baris"),
    BACKSPACE("backspace", "\u232B"),
    DELETE_WORD("deleteWord", "\u232B\u232B"),
    UNDO("undo", "\u21B6"),
    REDO("redo", "\u21B7"),
    CLEAR("clear", "Bersih"),
    DELETE_STROKE("deleteStroke", "Hapus gores"),
    CYCLE_PEN("cyclePen", "Warna"),
    RECOGNIZE("recognize", "Teks"),
    SYSTEM_HANDWRITING("systemHandwriting", "Huawei"),
    OVERLAY_TOGGLE("overlay", "Layang"),
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
    OVERLAY_TOGGLE("overlay", "Papan melayang", KeyAction.OVERLAY_TOGGLE);

    companion object {
        fun fromId(id: String?): PenButtonAction =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
    }
}

/** Satu tombol pada grid 3x3. */
data class KeyDef(
    val action: KeyAction = KeyAction.SPACE,
    /** Label khusus; kosong berarti memakai label bawaan aksi. */
    val label: String = "",
    /** Teks yang diinput untuk [KeyAction.TEXT]. */
    val text: String = "",
    /** Ulangi aksi selama tombol ditahan (mis. hapus). */
    val repeatOnHold: Boolean = false
) {
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

    // Grid tombol 3x3
    val gridButtonCount: Int = 9,
    val gridKeys: List<KeyDef> = defaultGridKeys(),
    val penButtonPrimary: PenButtonAction = PenButtonAction.RECOGNIZE,
    val penButtonSecondary: PenButtonAction = PenButtonAction.UNDO,

    // Tampilan & transparansi (0-100)
    val panelColor: Int = 0xFF111827.toInt(),
    val panelOpacity: Int = 92,
    val boardColor: Int = 0xFFFFFFFF.toInt(),
    val boardOpacity: Int = 96,
    val lineColor: Int = 0xFF94A3B8.toInt(),
    val buttonColor: Int = 0xFF1F2937.toInt(),
    val buttonOpacity: Int = 88,
    val buttonTextColor: Int = 0xFFF8FAFC.toInt(),
    val accentColor: Int = 0xFF0EA5E9.toInt(),
    val cornerRadiusDp: Int = 16,
    val haptics: Boolean = true,

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
    val autoSwitchToSystemHandwriting: Boolean = true
) {

    /** Warna pena yang sedang dipakai. */
    val currentPenColor: Int
        get() = penColors.getOrElse(penColorIndex) { DEFAULT_PEN_COLORS.first() }

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
        put("autoRecognize", autoRecognize)
        put("autoRecognizeDelayMs", autoRecognizeDelayMs)
        put("autoSpace", autoSpace)
        put("clearAfterRecognize", clearAfterRecognize)
        put("recognitionLanguage", recognitionLanguage)
        put("autoSwitchToSystemHandwriting", autoSwitchToSystemHandwriting)
    }

    /** Semua angka dipangkas ke rentang yang masuk akal. */
    fun sanitized(): PenboardConfig {
        val colors = penColors.map { it }.distinct().take(MAX_PEN_COLORS).ifEmpty { DEFAULT_PEN_COLORS }
        return copy(
            boardHeightDp = boardHeightDp.coerceIn(MIN_BOARD_HEIGHT_DP, MAX_BOARD_HEIGHT_DP),
            lineGapDp = lineGapDp.coerceIn(MIN_LINE_GAP_DP, MAX_LINE_GAP_DP),
            penWidthDp = penWidthDp.coerceIn(MIN_PEN_WIDTH_DP, MAX_PEN_WIDTH_DP),
            penColors = colors,
            penColorIndex = penColorIndex.coerceIn(0, colors.size - 1),
            gridButtonCount = gridButtonCount.coerceIn(MIN_GRID_KEYS, MAX_GRID_KEYS),
            gridKeys = sanitizeKeys(gridKeys),
            panelOpacity = panelOpacity.coerceIn(0, 100),
            boardOpacity = boardOpacity.coerceIn(0, 100),
            buttonOpacity = buttonOpacity.coerceIn(0, 100),
            cornerRadiusDp = cornerRadiusDp.coerceIn(MIN_RADIUS_DP, MAX_RADIUS_DP),
            autoRecognizeDelayMs = autoRecognizeDelayMs.coerceIn(MIN_AUTO_DELAY_MS, MAX_AUTO_DELAY_MS),
            recognitionLanguage = RecognitionLanguages.normalize(recognitionLanguage)
        )
    }

    private fun sanitizeKeys(keys: List<KeyDef>): List<KeyDef> {
        val defaults = defaultGridKeys()
        val result = ArrayList<KeyDef>(GRID_SLOTS)
        for (index in 0 until GRID_SLOTS) {
            result.add(keys.getOrNull(index) ?: defaults[index])
        }
        return result
    }

    private fun JSONObject.putColor(name: String, color: Int) {
        put(name, color.toLong() and 0xFFFFFFFFL)
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val GRID_SLOTS = 9
        const val MAX_PEN_COLORS = 6

        const val MIN_BOARD_HEIGHT_DP = 120
        const val MAX_BOARD_HEIGHT_DP = 340
        const val MIN_LINE_GAP_DP = 28
        const val MAX_LINE_GAP_DP = 76
        const val MIN_PEN_WIDTH_DP = 1
        const val MAX_PEN_WIDTH_DP = 12
        const val MIN_GRID_KEYS = 1
        const val MAX_GRID_KEYS = GRID_SLOTS
        const val MIN_RADIUS_DP = 0
        const val MAX_RADIUS_DP = 32
        const val MIN_AUTO_DELAY_MS = 500
        const val MAX_AUTO_DELAY_MS = 5000

        val DEFAULT_PEN_COLORS: List<Int> = listOf(
            0xFFEF4444.toInt(),
            0xFF111827.toInt(),
            0xFF2563EB.toInt()
        )

        /** Grid bawaan: tombol yang paling sering dipakai saat menulis satu kata. */
        fun defaultGridKeys(): List<KeyDef> = listOf(
            KeyDef(KeyAction.RECOGNIZE),
            KeyDef(KeyAction.SPACE),
            KeyDef(KeyAction.ENTER),
            KeyDef(KeyAction.BACKSPACE, repeatOnHold = true),
            KeyDef(KeyAction.UNDO),
            KeyDef(KeyAction.CLEAR),
            KeyDef(KeyAction.DELETE_STROKE),
            KeyDef(KeyAction.SYSTEM_HANDWRITING),
            KeyDef(KeyAction.HIDE)
        )

        fun fromJson(json: JSONObject): PenboardConfig {
            val base = PenboardConfig()
            val colors = json.optJSONArray("penColors")?.let { array ->
                (0 until array.length()).map { index -> (array.optLong(index) and 0xFFFFFFFFL).toInt() }
            } ?: base.penColors
            val keys = json.optJSONArray("gridKeys")?.let { array ->
                (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(KeyDef::fromJson) }
            } ?: base.gridKeys
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
                autoRecognize = json.optBoolean("autoRecognize", base.autoRecognize),
                autoRecognizeDelayMs = json.optInt("autoRecognizeDelayMs", base.autoRecognizeDelayMs),
                autoSpace = json.optBoolean("autoSpace", base.autoSpace),
                clearAfterRecognize = json.optBoolean("clearAfterRecognize", base.clearAfterRecognize),
                recognitionLanguage = json.optString("recognitionLanguage", base.recognitionLanguage),
                autoSwitchToSystemHandwriting = json.optBoolean(
                    "autoSwitchToSystemHandwriting",
                    base.autoSwitchToSystemHandwriting
                )
            ).sanitized()
        }

        private fun JSONObject.color(name: String, fallback: Int): Int =
            (optLong(name, fallback.toLong()) and 0xFFFFFFFFL).toInt()
    }
}
