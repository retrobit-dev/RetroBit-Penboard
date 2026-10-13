package dev.retrobit.penboard.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Alat aktif di papan tulis. */
enum class ToolMode { WRITE, ERASE }

/** Posisi baris tombol tambahan terhadap papan tulis. */
enum class ToolbarSide(val id: String, val label: String) {
    BOTTOM("bottom", "Bawah papan"),
    TOP("top", "Atas papan"),
    SIDE("side", "Samping papan");

    companion object {
        fun fromId(id: String?): ToolbarSide = entries.firstOrNull { it.id == id } ?: BOTTOM
    }
}

/** Semua aksi yang bisa ditempelkan ke sebuah tombol. */
enum class KeyAction(val id: String, val label: String) {
    INSERT("insert", "Tulis teks / simbol"),
    SPACE("space", "Spasi"),
    ENTER("enter", "Entri (baris baru)"),
    BACKSPACE("backspace", "Hapus huruf"),
    DELETE_STROKE("deleteStroke", "Hapus goresan terakhir"),
    CLEAR("clear", "Bersihkan papan"),
    UNDO("undo", "Urungkan"),
    REDO("redo", "Ulangi"),
    CURSOR_LEFT("cursorLeft", "Geser kursor ke kiri"),
    CURSOR_RIGHT("cursorRight", "Geser kursor ke kanan"),
    SAVE_IMAGE("saveImage", "Simpan gambar ke Galeri"),
    COLOR("color", "Ganti warna pena"),
    PROFILE_NEXT("profileNext", "Ganti profil papan"),
    ERASE("erase", "Alat penghapus"),
    WRITE("write", "Alat pena"),
    HIDE("hide", "Sembunyikan papan");

    companion object {
        fun fromId(id: String?): KeyAction = entries.firstOrNull { it.id == id } ?: INSERT
    }
}

fun newId(): String = UUID.randomUUID().toString().substring(0, 8)

/** Satu tombol pada baris tombol. */
data class KeyDef(
    val id: String = newId(),
    val action: KeyAction = KeyAction.INSERT,
    val text: String = "",
    /** Bobot lebar relatif terhadap tombol lain pada baris yang sama. */
    val width: Float = 1f,
    /** 0 = lebar tetap. > 0 = mengisi sisa ruang baris (porsi 0..0.85). */
    val flex: Float = 0f,
    /** Ulangi aksi selama tombol ditahan (cocok untuk hapus / geser kursor). */
    val repeat: Boolean = false
) {
    val label: String
        get() = when (action) {
            KeyAction.INSERT -> text
            else -> action.label
        }

    fun displayLabel(): String = when (action) {
        KeyAction.INSERT -> text
        else -> action.label
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("action", action.id)
        .put("text", text)
        .put("width", width.toDouble())
        .put("flex", flex.toDouble())
        .put("repeat", repeat)

    fun sanitized(): KeyDef = copy(
        text = text.take(12),
        width = width.coerceIn(0.2f, 12f),
        flex = flex.coerceIn(0f, 0.85f)
    )

    companion object {
        fun fromJson(o: JSONObject): KeyDef = KeyDef(
            id = o.optString("id").ifBlank { newId() },
            action = KeyAction.fromId(o.optString("action")),
            text = o.optString("text"),
            width = o.optDouble("width", 1.0).toFloat().coerceIn(0.2f, 12f),
            flex = o.optDouble("flex", 0.0).toFloat().coerceIn(0f, 0.85f),
            repeat = o.optBoolean("repeat", false)
        )

        fun text(value: String, width: Float = 1f, flex: Float = 0f): KeyDef =
            KeyDef(action = KeyAction.INSERT, text = value, width = width, flex = flex)

        fun action(action: KeyAction, width: Float = 1f, flex: Float = 0f, repeat: Boolean = false): KeyDef =
            KeyDef(action = action, width = width, flex = flex, repeat = repeat)
    }
}

/** Satu baris tombol. */
data class RowDef(
    val id: String = newId(),
    val keys: List<KeyDef> = emptyList(),
    /** Tinggi relatif terhadap tinggi dasar tombol. */
    val height: Float = 1f
) {
    fun toJson(): JSONObject {
        val arr = JSONArray()
        keys.forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("id", id)
            .put("height", height.toDouble())
            .put("keys", arr)
    }

    fun sanitized(): RowDef = copy(
        keys = keys.take(10).map { it.sanitized() },
        height = height.coerceIn(0.6f, 2.5f)
    )

    companion object {
        fun of(height: Float = 1f, vararg keys: KeyDef): RowDef = RowDef(keys = keys.toList(), height = height)

        fun fromJson(o: JSONObject): RowDef {
            val keys = mutableListOf<KeyDef>()
            val arr = o.optJSONArray("keys")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { keys.add(KeyDef.fromJson(it)) }
                }
            }
            return RowDef(
                id = o.optString("id").ifBlank { newId() },
                keys = keys,
                height = o.optDouble("height", 1.0).toFloat().coerceIn(0.6f, 2.5f)
            )
        }
    }
}

/** Profil lengkap: papan tulis + susunan tombol + tema warna. */
data class PenboardConfig(
    val id: String = newId(),
    val name: String = "Papan Baru",
    /** Diisi jika profil berasal dari preset bawaan (untuk fitur pulihkan). */
    val builtinId: String? = null,

    // ---------- papan tulis ----------
    val boardHeightDp: Int = 300,
    val lineGapDp: Int = 44,
    val boardBgColor: Int = 0xFF111827.toInt(),
    val boardBgOpacity: Int = 92,
    val gridEnabled: Boolean = true,
    val gridColor: Int = 0xFF94A3B8.toInt(),
    val gridOpacity: Int = 40,
    val showBaseline: Boolean = true,
    val strokeColor: Int = 0xFFF8FAFC.toInt(),
    val strokeWidthDp: Float = 3.2f,
    val eraserWidthDp: Float = 22f,
    val allowFinger: Boolean = true,
    val autoShift: Boolean = true,

    // ---------- panel & tombol ----------
    val panelColor: Int = 0xFF0B1220.toInt(),
    val panelOpacity: Int = 96,
    val toolbarSide: ToolbarSide = ToolbarSide.BOTTOM,
    val showToolbar: Boolean = true,
    val keyHeightDp: Int = 46,
    val keyCornerDp: Int = 12,
    val keyBgColor: Int = 0xFF1E293B.toInt(),
    val keyBgOpacity: Int = 92,
    val keyTextColor: Int = 0xFFF8FAFC.toInt(),
    val keyTextSizeSp: Int = 17,
    val keyAccentColor: Int = 0xFF3D5AFE.toInt(),
    val toolbarRows: List<RowDef> = emptyList(),
    val boardRows: List<RowDef> = emptyList()
) {

    fun boardCopy(name: String = this.name): PenboardConfig = copy(
        id = newId(),
        name = name,
        builtinId = null,
        toolbarRows = toolbarRows.map { it.copy(id = newId(), keys = it.keys.map { k -> k.copy(id = newId()) }) },
        boardRows = boardRows.map { it.copy(id = newId(), keys = it.keys.map { k -> k.copy(id = newId()) }) }
    )

    fun sanitized(): PenboardConfig {
        val safeName = name.trim().ifBlank { "Papan" }.take(40)
        // Baris kosong tetap dibiarkan ada supaya pengguna bisa menyusun tombol dahulu.
        val toolRows = toolbarRows.take(MAX_ROWS).map { it.sanitized() }
        val boardRowsSafe = boardRows.take(MAX_ROWS).map { it.sanitized() }
        val fallback = Presets.defaultConfig()
        return copy(
            name = safeName,
            boardHeightDp = boardHeightDp.coerceIn(140, 600),
            lineGapDp = lineGapDp.coerceIn(22, 96),
            boardBgOpacity = boardBgOpacity.coerceIn(0, 100),
            gridOpacity = gridOpacity.coerceIn(0, 100),
            strokeWidthDp = strokeWidthDp.coerceIn(0.8f, 16f),
            eraserWidthDp = eraserWidthDp.coerceIn(8f, 80f),
            panelOpacity = panelOpacity.coerceIn(0, 100),
            keyHeightDp = keyHeightDp.coerceIn(30, 96),
            keyCornerDp = keyCornerDp.coerceIn(0, 28),
            keyBgOpacity = keyBgOpacity.coerceIn(0, 100),
            keyTextSizeSp = keyTextSizeSp.coerceIn(10, 30),
            toolbarRows = toolRows.ifEmpty { fallback.toolbarRows },
            boardRows = boardRowsSafe.ifEmpty { fallback.boardRows }
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("builtinId", builtinId ?: JSONObject.NULL)
        put("boardHeightDp", boardHeightDp)
        put("lineGapDp", lineGapDp)
        put("boardBgColor", boardBgColor)
        put("boardBgOpacity", boardBgOpacity)
        put("gridEnabled", gridEnabled)
        put("gridColor", gridColor)
        put("gridOpacity", gridOpacity)
        put("showBaseline", showBaseline)
        put("strokeColor", strokeColor)
        put("strokeWidthDp", strokeWidthDp.toDouble())
        put("eraserWidthDp", eraserWidthDp.toDouble())
        put("allowFinger", allowFinger)
        put("autoShift", autoShift)
        put("panelColor", panelColor)
        put("panelOpacity", panelOpacity)
        put("toolbarSide", toolbarSide.id)
        put("showToolbar", showToolbar)
        put("keyHeightDp", keyHeightDp)
        put("keyCornerDp", keyCornerDp)
        put("keyBgColor", keyBgColor)
        put("keyBgOpacity", keyBgOpacity)
        put("keyTextColor", keyTextColor)
        put("keyTextSizeSp", keyTextSizeSp)
        put("keyAccentColor", keyAccentColor)
        put("toolbarRows", JSONArray().also { a -> toolbarRows.forEach { a.put(it.toJson()) } })
        put("boardRows", JSONArray().also { a -> boardRows.forEach { a.put(it.toJson()) } })
    }

    companion object {
        /** Batas jumlah baris tombol supaya keyboard tetap masuk akal di layar. */
        const val MAX_ROWS = 8

        fun fromJson(o: JSONObject): PenboardConfig {
            val toolbar = mutableListOf<RowDef>()
            o.optJSONArray("toolbarRows")?.let { arr ->
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { toolbar.add(RowDef.fromJson(it)) }
            }
            val board = mutableListOf<RowDef>()
            o.optJSONArray("boardRows")?.let { arr ->
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { board.add(RowDef.fromJson(it)) }
            }
            return PenboardConfig(
                id = o.optString("id").ifBlank { newId() },
                name = o.optString("name", "Papan"),
                builtinId = if (o.isNull("builtinId")) null else o.optString("builtinId").ifBlank { null },
                boardHeightDp = o.optInt("boardHeightDp", 300),
                lineGapDp = o.optInt("lineGapDp", 44),
                boardBgColor = o.optInt("boardBgColor", 0xFF111827.toInt()),
                boardBgOpacity = o.optInt("boardBgOpacity", 92),
                gridEnabled = o.optBoolean("gridEnabled", true),
                gridColor = o.optInt("gridColor", 0xFF94A3B8.toInt()),
                gridOpacity = o.optInt("gridOpacity", 40),
                showBaseline = o.optBoolean("showBaseline", true),
                strokeColor = o.optInt("strokeColor", 0xFFF8FAFC.toInt()),
                strokeWidthDp = o.optDouble("strokeWidthDp", 3.2).toFloat(),
                eraserWidthDp = o.optDouble("eraserWidthDp", 22.0).toFloat(),
                allowFinger = o.optBoolean("allowFinger", true),
                autoShift = o.optBoolean("autoShift", true),
                panelColor = o.optInt("panelColor", 0xFF0B1220.toInt()),
                panelOpacity = o.optInt("panelOpacity", 96),
                toolbarSide = ToolbarSide.fromId(o.optString("toolbarSide")),
                showToolbar = o.optBoolean("showToolbar", true),
                keyHeightDp = o.optInt("keyHeightDp", 46),
                keyCornerDp = o.optInt("keyCornerDp", 12),
                keyBgColor = o.optInt("keyBgColor", 0xFF1E293B.toInt()),
                keyBgOpacity = o.optInt("keyBgOpacity", 92),
                keyTextColor = o.optInt("keyTextColor", 0xFFF8FAFC.toInt()),
                keyTextSizeSp = o.optInt("keyTextSizeSp", 17),
                keyAccentColor = o.optInt("keyAccentColor", 0xFF3D5AFE.toInt()),
                toolbarRows = toolbar,
                boardRows = board
            ).sanitized()
        }
    }
}
