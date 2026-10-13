package dev.retrobit.penboard.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenboardConfigTest {

    @Test
    fun `nilai bawaan masuk akal`() {
        val config = PenboardConfig()
        assertEquals(PenboardConfig.SCHEMA_VERSION, config.schemaVersion)
        assertEquals(PenboardConfig.DEFAULT_ROWS, config.gridRows)
        assertEquals(PenboardConfig.COLUMNS * PenboardConfig.DEFAULT_ROWS, config.gridButtonCount)
        assertEquals(PenboardConfig.GRID_SLOTS, config.gridKeys.size)
        assertEquals(2000, config.autoRecognizeDelayMs)
        assertTrue(config.autoRecognize)
        assertTrue(config.autoSpace)
        assertTrue(config.pressureEnabled)
        assertTrue(config.scanlines)
    }

    @Test
    fun `tema bawaan adalah retro cyberpunk grayscale ePaper`() {
        val config = PenboardConfig()
        assertEquals(PenboardConfig.PANEL_COLOR, config.panelColor)
        assertEquals(PenboardConfig.PAPER_COLOR, config.boardColor)
        assertEquals(PenboardConfig.BUTTON_COLOR, config.buttonColor)
        assertEquals(PenboardConfig.ACCENT_COLOR, config.accentColor)
        // Grayscale: warna merah, hijau, dan biru sama rata (tidak ada warna mencolok).
        listOf(
            config.panelColor, config.boardColor, config.lineColor,
            config.buttonColor, config.buttonTextColor, config.accentColor
        ).forEach { color ->
            assertEquals("warna $color bukan grayscale", 0, graySpread(color))
        }
        assertTrue(PenboardConfig.DEFAULT_PEN_COLORS.all { graySpread(it) <= 6 })
    }

    @Test
    fun `tema retro bisa dipakai ulang tanpa mengubah tombol`() {
        val custom = PenboardConfig(
            gridRows = 4,
            gridButtonCount = 12,
            accentColor = 0xFFFF00FF.toInt(),
            autoRecognizeDelayMs = 3000
        ).sanitized()
        val themed = custom.withRetroTheme()
        assertEquals(PenboardConfig.ACCENT_COLOR, themed.accentColor)
        assertEquals(PenboardConfig.PAPER_COLOR, themed.boardColor)
        assertTrue(themed.scanlines)
        assertEquals(custom.gridRows, themed.gridRows)
        assertEquals(custom.gridKeys, themed.gridKeys)
        assertEquals(custom.autoRecognizeDelayMs, themed.autoRecognizeDelayMs)
    }

    @Test
    fun `jumlah baris dan tombol saling menjaga`() {
        // Enam baris berarti sampai 18 tombol.
        val sixRows = PenboardConfig(gridRows = 6, gridButtonCount = 18).sanitized()
        assertEquals(6, sixRows.gridRows)
        assertEquals(18, sixRows.gridButtonCount)

        // Empat baris berarti sampai 12 tombol.
        val fourRows = PenboardConfig(gridRows = 4, gridButtonCount = 12).sanitized()
        assertEquals(4, fourRows.gridRows)
        assertEquals(12, fourRows.gridButtonCount)

        // Tombol dipangkas bila melebihi jumlah sel di baris yang dipilih.
        val tooMany = PenboardConfig(gridRows = 1, gridButtonCount = 9).sanitized()
        assertEquals(1, tooMany.gridRows)
        assertEquals(PenboardConfig.COLUMNS, tooMany.gridButtonCount)

        // Baris liar dibatasi ke 1-6.
        assertEquals(PenboardConfig.MAX_GRID_ROWS, PenboardConfig(gridRows = 99).sanitized().gridRows)
        assertEquals(PenboardConfig.MIN_GRID_ROWS, PenboardConfig(gridRows = 0).sanitized().gridRows)
    }

    @Test
    fun `preset HiWrite menyetel tata letak dan tema seperti HiWrite`() {
        val custom = PenboardConfig(
            boardHeightDp = 130,
            penColorIndex = 2,
            gridRows = 1,
            gridButtonCount = 1,
            scanlines = false,
            panelOpacity = 40,
            autoRecognize = false,
            autoSpace = false,
            autoRecognizeDelayMs = 4500,
            recognitionLanguage = "en-US",
            penButtonPrimary = PenButtonAction.SYSTEM_HANDWRITING
        )

        val preset = custom.withHiWritePreset()

        // Papan tulis & tombol ala HiWrite.
        assertEquals(200, preset.boardHeightDp)
        assertEquals(PenboardConfig.MAX_GRID_ROWS, preset.gridRows)
        assertEquals(PenboardConfig.MAX_GRID_KEYS, preset.gridButtonCount)
        assertEquals(PenboardConfig.GRID_SLOTS, preset.gridKeys.size)
        assertTrue(preset.gridKeys.take(PenboardConfig.COLUMNS).all { it.action.name.startsWith("CURSOR_") })
        listOf(KeyAction.SPACE, KeyAction.ENTER, KeyAction.BACKSPACE).forEach { action ->
            assertTrue("$action tidak ada di preset", preset.gridKeys.any { it.action == action })
        }

        // Tema retro grayscale ePaper dengan garis pindaian.
        assertEquals(0, graySpread(preset.panelColor))
        assertEquals(0, graySpread(preset.boardColor))
        assertEquals(0, graySpread(preset.buttonColor))
        assertTrue(preset.scanlines)

        // Input otomatis 2 detik dengan spasi otomatis.
        assertTrue(preset.autoRecognize)
        assertTrue(preset.autoSpace)
        assertTrue(preset.clearAfterRecognize)
        assertEquals(2000, preset.autoRecognizeDelayMs)

        // Pengaturan pribadi dibiarkan.
        assertEquals("en-US", preset.recognitionLanguage)
        assertEquals(PenButtonAction.SYSTEM_HANDWRITING, preset.penButtonPrimary)

        // Hasilnya sudah bersih: dipanggil lagi tidak mengubah apa pun.
        assertEquals(preset, preset.withHiWritePreset())
    }

    @Test
    fun `penyimpanan bolak-balik tidak mengubah pengaturan`() {
        val config = PenboardConfig(
            boardHeightDp = 220,
            lineGapDp = 52,
            penWidthDp = 5,
            gridButtonCount = 4,
            autoRecognizeDelayMs = 1500,
            recognitionLanguage = "en-US",
            penButtonPrimary = PenButtonAction.CLEAR
        ).sanitized()
        val restored = PenboardConfig.fromJson(JSONObject(config.toJson().toString()))
        assertEquals(config.boardHeightDp, restored.boardHeightDp)
        assertEquals(config.lineGapDp, restored.lineGapDp)
        assertEquals(config.penWidthDp, restored.penWidthDp)
        assertEquals(config.gridButtonCount, restored.gridButtonCount)
        assertEquals(config.autoRecognizeDelayMs, restored.autoRecognizeDelayMs)
        assertEquals("en-US", restored.recognitionLanguage)
        assertEquals(PenButtonAction.CLEAR, restored.penButtonPrimary)
        assertEquals(config.penColors, restored.penColors)
        assertEquals(config.boardColor, restored.boardColor)
        assertEquals(config.gridKeys, restored.gridKeys)
    }

    @Test
    fun `angka liar dipangkas ke rentang aman`() {
            val wild = PenboardConfig(
            boardHeightDp = 9999,
            lineGapDp = 1,
            penWidthDp = 99,
            gridRows = 99,
            gridButtonCount = 77,
            cornerRadiusDp = -5,
            autoRecognizeDelayMs = 50,
            panelOpacity = 500,
            boardOpacity = -20,
            buttonOpacity = 900,
            recognitionLanguage = "xx-YY"
        ).sanitized()
        assertEquals(PenboardConfig.MAX_BOARD_HEIGHT_DP, wild.boardHeightDp)
        assertEquals(PenboardConfig.MIN_LINE_GAP_DP, wild.lineGapDp)
        assertEquals(PenboardConfig.MAX_PEN_WIDTH_DP, wild.penWidthDp)
        assertEquals(PenboardConfig.MAX_GRID_KEYS, wild.gridButtonCount)
        assertEquals(PenboardConfig.MIN_RADIUS_DP, wild.cornerRadiusDp)
        assertEquals(PenboardConfig.MIN_AUTO_DELAY_MS, wild.autoRecognizeDelayMs)
        assertEquals(100, wild.panelOpacity)
        assertEquals(0, wild.boardOpacity)
        assertEquals(100, wild.buttonOpacity)
        assertEquals("id-ID", wild.recognitionLanguage)
    }

    @Test
    fun `warna pena dibatasi dan selalu tersedia`() {
        val many = PenboardConfig(penColors = List(20) { 0xFF000000.toInt() + it }).sanitized()
        assertTrue(many.penColors.size <= PenboardConfig.MAX_PEN_COLORS)

        // Daftar warna kosong diganti warna bawaan, dan indeks warna selalu masuk rentang.
        val none = PenboardConfig(penColors = emptyList(), penColorIndex = 3).sanitized()
        assertTrue(none.penColors.isNotEmpty())
        assertTrue(none.penColorIndex in none.penColors.indices)
        assertTrue(none.currentPenColor != 0)
    }

    @Test
    fun `label tombol memakai teks aksi bila ada`() {
        val config = PenboardConfig()
        assertEquals("Teks", config.labelFor(KeyDef(KeyAction.RECOGNIZE)))
        assertEquals("ok", config.labelFor(KeyDef(KeyAction.TEXT, text = "ok")))
        assertEquals("Kirim", config.labelFor(KeyDef(KeyAction.TEXT, label = "Kirim", text = "ok")))
    }

    @Test
    fun `aksi tidak dikenal jatuh ke nilai aman`() {
        assertEquals(KeyAction.SPACE, KeyAction.fromId("entah"))
        assertEquals(KeyAction.RECOGNIZE, KeyAction.fromId("recognize"))
        assertEquals(PenButtonAction.NONE, PenButtonAction.fromId(null))
        assertEquals(PenButtonAction.CLEAR, PenButtonAction.fromId("clear"))
    }

    @Test
    fun `tombol kursor dan keyboard konvensional bisa dipilih`() {
        assertEquals(KeyAction.CURSOR_LEFT, KeyAction.fromId("cursorLeft"))
        assertEquals(KeyAction.CURSOR_RIGHT, KeyAction.fromId("cursorRight"))
        assertEquals(KeyAction.CURSOR_UP, KeyAction.fromId("cursorUp"))
        assertEquals(KeyAction.CURSOR_DOWN, KeyAction.fromId("cursorDown"))
        assertEquals(KeyAction.CURSOR_HOME, KeyAction.fromId("cursorHome"))
        assertEquals(KeyAction.CURSOR_END, KeyAction.fromId("cursorEnd"))
        assertEquals(KeyAction.SPACE, KeyAction.fromId("space"))
        assertEquals(KeyAction.ENTER, KeyAction.fromId("enter"))
        assertEquals(KeyAction.BACKSPACE, KeyAction.fromId("backspace"))
        assertEquals(KeyAction.DELETE_WORD, KeyAction.fromId("deleteWord"))
        assertEquals(KeyAction.DELETE_FORWARD, KeyAction.fromId("deleteForward"))
        assertEquals(KeyAction.TAB, KeyAction.fromId("tab"))
        assertEquals(KeyAction.SELECT_ALL, KeyAction.fromId("selectAll"))
        assertEquals(KeyAction.CUT, KeyAction.fromId("cut"))
        assertEquals(KeyAction.COPY, KeyAction.fromId("copy"))
        assertEquals(KeyAction.PASTE, KeyAction.fromId("paste"))

        // Semua aksi punya id unik supaya tersimpan dengan benar di pengaturan.
        val ids = KeyAction.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `grid bawaan memuat tombol penting`() {
        val keys = PenboardConfig.defaultGridKeys()
        assertEquals(PenboardConfig.GRID_SLOTS, keys.size)
        assertTrue(keys.any { it.action == KeyAction.RECOGNIZE })
        assertTrue(keys.any { it.action == KeyAction.SPACE })
        assertTrue(keys.any { it.action == KeyAction.ENTER })
        assertTrue(keys.any { it.action == KeyAction.BACKSPACE && it.repeatOnHold })
        // Empat panah kursor ada di susunan bawaan (baris pertama-tama).
        listOf(
            KeyAction.CURSOR_LEFT, KeyAction.CURSOR_RIGHT, KeyAction.CURSOR_UP, KeyAction.CURSOR_DOWN
        ).forEach { action ->
            assertTrue("panah $action tidak ada di grid bawaan", keys.any { it.action == action })
        }
        assertTrue(keys.take(PenboardConfig.COLUMNS).all { it.action.name.startsWith("CURSOR_") })
        // Baris 5-6 bawaan: sisa tombol keyboard konvensional dan tombol layar.
        assertTrue(keys.any { it.action == KeyAction.TAB })
        assertTrue(keys.any { it.action == KeyAction.DELETE_FORWARD })
        assertTrue(keys.any { it.action == KeyAction.SYSTEM_HANDWRITING })
        assertEquals(18, keys.size)
    }

    @Test
    fun `berkas rusak tidak merusak pengaturan`() {
        val restored = PenboardConfig.fromJson(JSONObject("""{"gridKeys":[],"penColors":[]}"""))
        assertEquals(PenboardConfig.GRID_SLOTS, restored.gridKeys.size)
        assertTrue(restored.penColors.isNotEmpty())
        assertFalse(restored.gridButtonCount <= 0)
    }

    /** Selisih terbesar antar kanal warna: 0 berarti benar-benar abu-abu. */
    private fun graySpread(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return maxOf(r, g, b) - minOf(r, g, b)
    }

    @Test
    fun `aksi papan melayang sudah tidak ada`() {
        assertEquals(
            listOf(
                "text", "recognize", "clear", "deleteStroke", "undo", "redo", "cyclePen",
                "space", "enter", "backspace", "deleteWord", "deleteForward", "tab",
                "cursorLeft", "cursorRight", "cursorUp", "cursorDown", "cursorHome", "cursorEnd",
                "selectAll", "cut", "copy", "paste",
                "systemHandwriting", "keyboardPicker", "hide"
            ),
            KeyAction.entries.map { it.id }
        )
    }

    @Test
    fun `pengaturan lama tetap terbaca setelah papan melayang dihapus`() {
        val json = JSONObject(
            """{"gridRows":3,"gridButtonCount":2,"gridKeys":[{"action":"hide"},{"action":"overlay"}]}"""
        )
        val config = PenboardConfig.fromJson(json)
        assertEquals(3, config.gridRows)
        // Tombol lama "overlay" (papan melayang) jatuh ke SPACE, bukan menggagalkan pembacaan.
        assertEquals(KeyAction.HIDE, config.gridKeys.first().action)
        assertEquals(KeyAction.SPACE, config.gridKeys[1].action)
        assertEquals(PenboardConfig.GRID_SLOTS, config.gridKeys.size)
        assertEquals(KeyAction.SPACE, KeyAction.fromId("overlay"))
        // Id tombol Penboard sekarang tidak ada yang menyebut papan melayang.
        assertFalse(KeyAction.entries.map { it.id }.contains("overlay"))
    }

    @Test
    fun `bilah bawah punya bawaan dan batas aman`() {
        val config = PenboardConfig()
        assertEquals(PenboardConfig.DEFAULT_BOTTOM_KEYS, config.bottomButtonCount)
        assertEquals(PenboardConfig.BOTTOM_SLOTS, config.bottomKeys.size)
        assertEquals(
            listOf(
                KeyAction.RECOGNIZE, KeyAction.TEXT, KeyAction.DELETE_STROKE,
                KeyAction.CLEAR, KeyAction.UNDO, KeyAction.REDO, KeyAction.CYCLE_PEN, KeyAction.HIDE
            ),
            config.bottomKeys.map { it.action }
        )

        val many = PenboardConfig(bottomButtonCount = 99, bottomKeys = emptyList()).sanitized()
        assertEquals(PenboardConfig.MAX_BOTTOM_KEYS, many.bottomButtonCount)
        assertEquals(PenboardConfig.BOTTOM_SLOTS, many.bottomKeys.size)

        // Bilah bawah bisa dimatikan seluruhnya.
        val none = PenboardConfig(bottomButtonCount = -4).sanitized()
        assertEquals(PenboardConfig.MIN_BOTTOM_KEYS, none.bottomButtonCount)
    }

    @Test
    fun `bingkai jendela dan bilah bawah tersimpan bolak-balik`() {
        val config = PenboardConfig(
            windowFrame = false,
            minimizeButton = false,
            windowTitle = "  Papan Tulis  ",
            bottomButtonCount = 3,
            bottomKeys = listOf(KeyDef(KeyAction.CLEAR), KeyDef(KeyAction.UNDO), KeyDef(KeyAction.COPY))
        ).sanitized()
        assertEquals("Papan Tulis", config.windowTitle)

        val restored = PenboardConfig.fromJson(JSONObject(config.toJson().toString()))
        assertFalse(restored.windowFrame)
        assertFalse(restored.minimizeButton)
        assertEquals("Papan Tulis", restored.windowTitle)
        assertEquals(3, restored.bottomButtonCount)
        assertEquals(
            listOf(KeyAction.CLEAR, KeyAction.UNDO, KeyAction.COPY),
            restored.bottomKeys.take(3).map { it.action }
        )
        assertEquals(PenboardConfig.BOTTOM_SLOTS, restored.bottomKeys.size)
    }

    @Test
    fun `judul jendela dipangkas dan kosong memakai bawaan`() {
        val blank = PenboardConfig(windowTitle = "   ").sanitized()
        assertEquals(PenboardConfig.DEFAULT_WINDOW_TITLE, blank.effectiveWindowTitle)

        val long = PenboardConfig(windowTitle = "X".repeat(200)).sanitized()
        assertEquals(PenboardConfig.MAX_WINDOW_TITLE, long.effectiveWindowTitle.length)
    }

    @Test
    fun `preset HiWrite menyalakan bingkai jendela dan bilah bawah`() {
        val preset = PenboardConfig(
            windowFrame = false,
            minimizeButton = false,
            bottomButtonCount = 0
        ).withHiWritePreset()
        assertTrue(preset.windowFrame)
        assertTrue(preset.minimizeButton)
        assertEquals(PenboardConfig.DEFAULT_BOTTOM_KEYS, preset.bottomButtonCount)
        assertEquals(PenboardConfig.DEFAULT_WINDOW_TITLE, preset.effectiveWindowTitle)
    }

    @Test
    fun `pengaturan lama tanpa bilah bawah tetap terbaca`() {
        // Berkas pengaturan dari versi sebelum bilah bawah: bidang barunya memakai bawaan.
        val json = JSONObject("""{"gridRows":3,"windowTitle":"Papan"}""")
        val config = PenboardConfig.fromJson(json)
        assertEquals("Papan", config.effectiveWindowTitle)
        assertTrue(config.windowFrame)
        assertTrue(config.minimizeButton)
        assertEquals(PenboardConfig.DEFAULT_BOTTOM_KEYS, config.bottomButtonCount)
        assertEquals(PenboardConfig.BOTTOM_SLOTS, config.bottomKeys.size)
    }
}
