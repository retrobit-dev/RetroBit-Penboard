package dev.retrobit.penboard.data

import dev.retrobit.penboard.data.KeyAction.BACKSPACE
import dev.retrobit.penboard.data.KeyAction.CLEAR
import dev.retrobit.penboard.data.KeyAction.CURSOR_LEFT
import dev.retrobit.penboard.data.KeyAction.CURSOR_RIGHT
import dev.retrobit.penboard.data.KeyAction.DELETE_STROKE
import dev.retrobit.penboard.data.KeyAction.ENTER
import dev.retrobit.penboard.data.KeyAction.ERASE
import dev.retrobit.penboard.data.KeyAction.HIDE
import dev.retrobit.penboard.data.KeyAction.COLOR
import dev.retrobit.penboard.data.KeyAction.REDO
import dev.retrobit.penboard.data.KeyAction.PROFILE_NEXT
import dev.retrobit.penboard.data.KeyAction.PROFILE_PREV
import dev.retrobit.penboard.data.KeyAction.SAVE_IMAGE
import dev.retrobit.penboard.data.KeyAction.SCROLL_NEWER
import dev.retrobit.penboard.data.KeyAction.SCROLL_OLDER
import dev.retrobit.penboard.data.KeyAction.SPACE
import dev.retrobit.penboard.data.KeyAction.UNDO
import dev.retrobit.penboard.data.KeyAction.WRITE

/**
 * Preset bawaan. Semuanya tetap bisa diubah pengguna; preset hanya titik awal
 * supaya aplikasi langsung enak dipakai.
 */
object Presets {

    const val STD = "standard"
    const val COMPACT = "compact"
    const val NUMERIC = "numeric"
    const val QUICK = "quick"
    const val LIGHT = "light"
    const val FULL = "full"
    const val PEN_COLORS = "penColors"
    const val FREEFORM = "freeform"

    fun defaultConfig(): PenboardConfig = standard()

    fun builtins(): List<PenboardConfig> = listOf(
        standard(), compact(), numeric(), quickTools(), light(), fullscreen(), penColors(), freeform()
    )

    /** Profil default: 6 baris tulis + dua baris tombol. */
    fun standard(): PenboardConfig = PenboardConfig(
        name = "Catatan Standar",
        builtinId = STD,
        boardHeightDp = 300,
        lineGapDp = 44,
        toolbarRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(UNDO),
                KeyDef.action(REDO),
                KeyDef.action(DELETE_STROKE, repeat = true),
                KeyDef.action(CLEAR),
                KeyDef.action(COLOR),
                KeyDef.action(ERASE),
                KeyDef.action(SAVE_IMAGE),
                KeyDef.action(HIDE)
            )
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.text(".", 0.7f),
                KeyDef.text(",", 0.7f),
                KeyDef.text("?", 0.7f),
                KeyDef.text("!", 0.7f),
                KeyDef.text("-", 0.7f),
                KeyDef.action(SPACE, flex = 0.3f),
                KeyDef.action(ENTER, 1.3f)
            ),
            RowDef.of(
                1f,
                KeyDef.action(BACKSPACE, repeat = true),
                KeyDef.action(CURSOR_LEFT, repeat = true),
                KeyDef.action(CURSOR_RIGHT, repeat = true),
                KeyDef.action(COLOR),
                KeyDef.action(HIDE)
            )
        )
    )

    /** Papan ringkas: hemat ruang layar, cocok untuk layar kecil. */
    fun compact(): PenboardConfig = PenboardConfig(
        name = "Catatan Ringkas",
        builtinId = COMPACT,
        boardHeightDp = 232,
        lineGapDp = 40,
        keyHeightDp = 42,
        toolbarRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(UNDO),
                KeyDef.action(DELETE_STROKE, repeat = true),
                KeyDef.action(CLEAR),
                KeyDef.action(SAVE_IMAGE),
                KeyDef.action(HIDE)
            )
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.text(".", 0.8f),
                KeyDef.text(",", 0.8f),
                KeyDef.action(SPACE, flex = 0.35f),
                KeyDef.action(ENTER, 1.2f),
                KeyDef.action(HIDE)
            )
        )
    )

    /** Untuk mengisi kolom angka cepat. */
    fun numeric(): PenboardConfig = PenboardConfig(
        name = "Angka & Simbol",
        builtinId = NUMERIC,
        boardHeightDp = 220,
        lineGapDp = 44,
        keyHeightDp = 44,
        keyTextSizeSp = 19,
        toolbarRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(UNDO),
                KeyDef.action(CLEAR),
                KeyDef.action(SAVE_IMAGE),
                KeyDef.action(HIDE)
            )
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.text("1"), KeyDef.text("2"), KeyDef.text("3"),
                KeyDef.text("4"), KeyDef.text("5"), KeyDef.text("6"),
                KeyDef.text("7"), KeyDef.text("8"), KeyDef.text("9"), KeyDef.text("0")
            ),
            RowDef.of(
                1f,
                KeyDef.text("-"), KeyDef.text(","), KeyDef.text("."), KeyDef.text(":"),
                KeyDef.text("+"), KeyDef.text("/"), KeyDef.text("%"), KeyDef.text("=")
            ),
            RowDef.of(
                1f,
                KeyDef.text("Rp", 1.3f),
                KeyDef.text("IDR", 1.3f),
                KeyDef.action(BACKSPACE, repeat = true),
                KeyDef.action(SPACE, flex = 0.3f),
                KeyDef.action(ENTER)
            )
        )
    )

    /** Fokus menggambar: papan tinggi, tombol alat di samping. */
    fun quickTools(): PenboardConfig = PenboardConfig(
        name = "Alat Cepat (Menggambar)",
        builtinId = QUICK,
        boardHeightDp = 400,
        lineGapDp = 52,
        strokeWidthDp = 4.5f,
        toolbarSide = ToolbarSide.SIDE,
        toolbarRows = listOf(
            RowDef.of(1f, KeyDef.action(WRITE)),
            RowDef.of(1f, KeyDef.action(ERASE)),
            RowDef.of(1f, KeyDef.action(UNDO)),
            RowDef.of(1f, KeyDef.action(REDO)),
            RowDef.of(1f, KeyDef.action(CLEAR)),
            RowDef.of(1f, KeyDef.action(SAVE_IMAGE)),
            RowDef.of(1f, KeyDef.action(HIDE))
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(SPACE, flex = 0.4f),
                KeyDef.action(ENTER, 1.2f),
                KeyDef.text(".", 0.7f),
                KeyDef.text(",", 0.7f)
            )
        )
    )

    /** Tema terang untuk layar cerah. */
    fun light(): PenboardConfig = PenboardConfig(
        name = "Tema Terang",
        builtinId = LIGHT,
        boardHeightDp = 288,
        lineGapDp = 44,
        boardBgColor = 0xFFFFFFFF.toInt(),
        boardBgOpacity = 98,
        gridColor = 0xFF94A3B8.toInt(),
        gridOpacity = 55,
        strokeColor = 0xFF0F172A.toInt(),
        panelColor = 0xFFE2E8F0.toInt(),
        panelOpacity = 98,
        keyBgColor = 0xFFFFFFFF.toInt(),
        keyBgOpacity = 100,
        keyTextColor = 0xFF0F172A.toInt(),
        keyAccentColor = 0xFF2563EB.toInt(),
        toolbarRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(UNDO),
                KeyDef.action(DELETE_STROKE, repeat = true),
                KeyDef.action(CLEAR),
                KeyDef.action(COLOR),
                KeyDef.action(ERASE),
                KeyDef.action(SAVE_IMAGE),
                KeyDef.action(HIDE)
            )
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.text("."), KeyDef.text(","), KeyDef.text("?"), KeyDef.text("!"),
                KeyDef.action(SPACE, flex = 0.3f),
                KeyDef.action(ENTER, 1.3f)
            )
        )
    )

    /** Papan hampir penuh layar untuk menulis panjang. */
    fun fullscreen(): PenboardConfig = PenboardConfig(
        name = "Papan Penuh",
        builtinId = FULL,
        boardHeightDp = 520,
        lineGapDp = 52,
        toolbarSide = ToolbarSide.TOP,
        toolbarRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(UNDO),
                KeyDef.action(DELETE_STROKE, repeat = true),
                KeyDef.action(CLEAR),
                KeyDef.action(SAVE_IMAGE),
                KeyDef.action(HIDE)
            )
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(SPACE, flex = 0.4f),
                KeyDef.text("."),
                KeyDef.text(","),
                KeyDef.action(ENTER, 1.2f),
                KeyDef.action(HIDE)
            )
        )
    )

    /** Warna pena tetap: tiap tekanan tombol berganti warna secara berurutan. */
    fun penColors(): PenboardConfig = PenboardConfig(
        name = "Warna Pena Tetap",
        builtinId = PEN_COLORS,
        boardHeightDp = 300,
        lineGapDp = 44,
        strokeWidthDp = 3.6f,
        penColors = listOf(
            0xFFF8FAFC.toInt(), 0xFF111827.toInt(), 0xFFEF4444.toInt(),
            0xFF3B82F6.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt()
        ),
        toolbarRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(COLOR),
                KeyDef.action(COLOR),
                KeyDef.action(COLOR),
                KeyDef.action(COLOR),
                KeyDef.action(SCROLL_OLDER),
                KeyDef.action(SCROLL_NEWER),
                KeyDef.action(ERASE)
            )
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(COLOR),
                KeyDef.action(UNDO),
                KeyDef.action(DELETE_STROKE, repeat = true),
                KeyDef.action(CLEAR),
                KeyDef.action(SPACE, flex = 0.3f),
                KeyDef.action(ENTER, 1.2f),
                KeyDef.action(SAVE_IMAGE),
                KeyDef.action(HIDE)
            )
        )
    )

    /** Papan leluasa: hampir seluruh layar, tanpa geser otomatis, tombol minimalis. */
    fun freeform(): PenboardConfig = PenboardConfig(
        name = "Papan Tulis Leluasa",
        builtinId = FREEFORM,
        boardHeightDp = 460,
        lineGapDp = 56,
        strokeWidthDp = 4.2f,
        autoShift = false,
        gridOpacity = 26,
        keyHeightDp = 44,
        toolbarSide = ToolbarSide.TOP,
        toolbarRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(WRITE),
                KeyDef.action(ERASE),
                KeyDef.action(COLOR),
                KeyDef.action(UNDO),
                KeyDef.action(REDO),
                KeyDef.action(SCROLL_OLDER),
                KeyDef.action(SCROLL_NEWER),
                KeyDef.action(CLEAR),
                KeyDef.action(SAVE_IMAGE),
                KeyDef.action(HIDE)
            )
        ),
        boardRows = listOf(
            RowDef.of(
                1f,
                KeyDef.action(SPACE, flex = 0.4f),
                KeyDef.text("."),
                KeyDef.text(","),
                KeyDef.action(ENTER, 1.2f),
                KeyDef.action(HIDE)
            )
        )
    )

    /** Kembalikan preset bawaan berdasarkan id-nya (untuk tombol "Pulihkan"). */
    fun factory(builtinId: String?): PenboardConfig? =
        builtins().firstOrNull { it.builtinId == builtinId }
}
