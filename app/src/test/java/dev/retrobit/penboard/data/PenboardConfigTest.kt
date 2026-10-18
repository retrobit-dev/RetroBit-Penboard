package dev.retrobit.penboard.data

import dev.retrobit.penboard.util.KeyboardChars
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
        assertEquals(PenboardConfig.COLUMNS, config.gridColumns)
        assertEquals(PenboardConfig.COLUMNS * PenboardConfig.DEFAULT_ROWS, config.gridButtonCount)
        // Ukuran tombol bawaan: otomatis (mengikuti tinggi papan), bukan angka tetap.
        assertEquals(0, config.gridCellDp)
        assertEquals(1f, config.panelWidthFraction, 0.0001f)
        assertTrue(config.floatingPanel)
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
            boardLines = 4,
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
        // Enam baris area tulis berarti tombolnya sampai 18 (3 kolom x 6 baris).
        val sixRows = PenboardConfig(boardLines = 6, gridButtonCount = 18).sanitized()
        assertEquals(6, sixRows.effectiveGridRows)
        assertEquals(18, sixRows.gridButtonCount)

        // Empat baris area tulis berarti sampai 12 tombol.
        val fourRows = PenboardConfig(boardLines = 4, gridButtonCount = 12).sanitized()
        assertEquals(4, fourRows.effectiveGridRows)
        assertEquals(12, fourRows.gridButtonCount)

        // Tombol dipangkas bila melebihi jumlah sel di baris yang dipilih.
        val tooMany = PenboardConfig(boardLines = 1, gridButtonCount = 9).sanitized()
        assertEquals(1, tooMany.effectiveGridRows)
        assertEquals(PenboardConfig.COLUMNS, tooMany.gridButtonCount)

        // Baris liar dibatasi ke 1-6 (lewat boardLines saat mode mengikuti).
        assertEquals(
            PenboardConfig.MAX_GRID_ROWS,
            PenboardConfig(boardLines = 99).sanitized().effectiveGridRows
        )
        assertEquals(
            PenboardConfig.MIN_GRID_ROWS,
            PenboardConfig(boardLines = 0).sanitized().effectiveGridRows
        )
    }

    @Test
    fun `jumlah kolom dan baris menentukan berapa tombol yang muat`() {
        // Enam kolom x enam baris = 36 tombol; semua kotak itu bisa dipakai.
        val wide = PenboardConfig(gridColumns = 6, boardLines = 6, gridButtonCount = 36).sanitized()
        assertEquals(6, wide.gridColumns)
        assertEquals(6, wide.effectiveGridRows)
        assertEquals(36, wide.gridButtonCount)
        assertEquals(PenboardConfig.GRID_SLOTS, wide.gridKeys.size)

        // Satu baris tiga kolom: hanya 3 tombol yang boleh tampil.
        val oneRow = PenboardConfig(boardLines = 1, gridButtonCount = 12).sanitized()
        assertEquals(1, oneRow.effectiveGridRows)
        assertEquals(PenboardConfig.COLUMNS, oneRow.gridButtonCount)

        // Dua kolom lima baris: 10 sel, jadi 12 tombol dipangkas jadi 10.
        val twoColumns = PenboardConfig(gridColumns = 2, boardLines = 5, gridButtonCount = 12).sanitized()
        assertEquals(2, twoColumns.gridColumns)
        assertEquals(10, twoColumns.gridButtonCount)

        // Kolom liar dibatasi ke 1-6.
        assertEquals(PenboardConfig.MAX_GRID_COLUMNS, PenboardConfig(gridColumns = 99).sanitized().gridColumns)
        assertEquals(PenboardConfig.MIN_GRID_COLUMNS, PenboardConfig(gridColumns = 0).sanitized().gridColumns)
    }

    @Test
    fun `baris tulisan menentukan posisi teks dan tersimpan bolak-balik`() {
        // Bawaan: menyala, jadi baris tulisan langsung dipakai sebagai posisi teks.
        assertTrue(PenboardConfig().lineOffsetEnabled)

        // Bisa dimatikan, dan pilihannya ikut tersimpan.
        val off = PenboardConfig(lineOffsetEnabled = false)
        val restored = PenboardConfig.fromJson(JSONObject(off.toJson().toString()))
        assertFalse(restored.lineOffsetEnabled)

        // Berkas pengaturan lama (tanpa bidang ini) tetap menyala seperti bawaan.
        assertTrue(PenboardConfig.fromJson(JSONObject("{\"gridRows\":2}")).lineOffsetEnabled)
    }

    @Test
    fun `preset HiWrite tidak mengubah posisi teks`() {
        val preset = PenboardConfig(lineOffsetEnabled = false).withHiWritePreset()
        assertFalse(preset.lineOffsetEnabled)
    }

    @Test
    fun `ukuran tombol persegi dan papan bisa diatur`() {
        // Ukuran tombol: 0 (otomatis) atau 32-160 dp; selebihnya dipangkas.
        assertEquals(0, PenboardConfig(gridCellDp = 0).sanitized().gridCellDp)
        assertEquals(0, PenboardConfig(gridCellDp = -20).sanitized().gridCellDp)
        assertEquals(PenboardConfig.MIN_CELL_DP, PenboardConfig(gridCellDp = 5).sanitized().gridCellDp)
        assertEquals(PenboardConfig.MAX_CELL_DP, PenboardConfig(gridCellDp = 9999).sanitized().gridCellDp)
        assertEquals(64, PenboardConfig(gridCellDp = 64).sanitized().gridCellDp)

        // Lebar papan 45-100 % layar; posisi yang dipindah selalu di dalam 0..1.
        assertEquals(PenboardConfig.MIN_PANEL_WIDTH_FRACTION, PenboardConfig(panelWidthFraction = 0.1f).sanitized().panelWidthFraction, 0.0001f)
        assertEquals(1f, PenboardConfig(panelWidthFraction = 4f).sanitized().panelWidthFraction, 0.0001f)
        assertEquals(0.8f, PenboardConfig(panelWidthFraction = 0.8f).sanitized().panelWidthFraction, 0.0001f)

        // Lebar area tulis 40-80% lebar isi panel; kolom tombol mengambil sisanya.
        assertEquals(
            PenboardConfig.DEFAULT_BOARD_WIDTH_FRACTION,
            PenboardConfig().boardWidthFraction,
            0.0001f
        )
        assertEquals(
            PenboardConfig.MIN_BOARD_WIDTH_FRACTION,
            PenboardConfig(boardWidthFraction = 0.1f).sanitized().boardWidthFraction,
            0.0001f
        )
        assertEquals(
            PenboardConfig.MAX_BOARD_WIDTH_FRACTION,
            PenboardConfig(boardWidthFraction = 4f).sanitized().boardWidthFraction,
            0.0001f
        )
        assertEquals(
            0.72f,
            PenboardConfig(boardWidthFraction = 0.72f).sanitized().boardWidthFraction,
            0.0001f
        )
        assertEquals(0f, PenboardConfig(panelOffsetX = -3f).sanitized().panelOffsetX, 0.0001f)
        assertEquals(1f, PenboardConfig(panelOffsetY = 9f).sanitized().panelOffsetY, 0.0001f)
    }

    @Test
    fun `baris tombol mengikuti area tulis atau diatur sendiri`() {
        // Bawaan: 3 baris area tulis, baris tombol mengikuti.
        assertEquals(3, PenboardConfig().boardLines)
        assertEquals(PenboardConfig.DEFAULT_BOARD_LINES, PenboardConfig().boardLines)
        assertTrue(PenboardConfig().gridRowsFollow)

        // Mode mengikuti: berapa pun baris area tulis, baris tombol sama.
        for (lines in PenboardConfig.MIN_BOARD_LINES..PenboardConfig.MAX_BOARD_LINES) {
            val config = PenboardConfig(
                boardLines = lines,
                gridRows = 1,
                gridButtonCount = lines * 3
            ).sanitized()
            assertEquals(lines, config.boardLines)
            assertEquals(lines, config.effectiveGridRows)
        }

        // Mode sendiri: baris tombol tidak lagi terikat baris tulisan.
        val manual = PenboardConfig(
            boardLines = 3,
            gridRowsFollow = false,
            gridRows = 5,
            gridButtonCount = 15
        ).sanitized()
        assertEquals(5, manual.effectiveGridRows)
        assertEquals(3, manual.boardLines)
        assertEquals(15, manual.gridButtonCount)
        // Mengubah baris tulisan tidak menggeser baris tombol yang diatur sendiri.
        assertEquals(5, manual.copy(boardLines = 6).sanitized().effectiveGridRows)
        // Baris tombol liar tetap dipangkas ke 1-6.
        assertEquals(
            PenboardConfig.MAX_GRID_ROWS,
            PenboardConfig(gridRowsFollow = false, gridRows = 99).sanitized().effectiveGridRows
        )
        assertEquals(
            PenboardConfig.MIN_GRID_ROWS,
            PenboardConfig(gridRowsFollow = false, gridRows = -3).sanitized().effectiveGridRows
        )

        // Baris liar area tulis dipangkas ke 1-6.
        assertEquals(PenboardConfig.MIN_BOARD_LINES, PenboardConfig(boardLines = 0).sanitized().boardLines)
        assertEquals(PenboardConfig.MAX_BOARD_LINES, PenboardConfig(boardLines = 99).sanitized().boardLines)
    }

    @Test
    fun `lebar dan tinggi tombol bawah bisa diatur sendiri`() {
        val custom = PenboardConfig(
            bottomKeys = PenboardConfig.defaultBottomKeys().toMutableList().also {
                it[0] = it[0].copy(widthMultiplier = 2f, heightMultiplier = 1.5f)
            }
        ).sanitized()
        assertEquals(2f, custom.bottomKeys.first().widthMultiplier, 0.0001f)
        assertEquals(1.5f, custom.bottomKeys.first().heightMultiplier, 0.0001f)

        // Nilai liar dipangkas ke rentang aman.
        val wild = PenboardConfig(
            bottomKeys = listOf(
                KeyDef(KeyAction.SPACE, widthMultiplier = 40f, heightMultiplier = 0.05f)
            )
        ).sanitized()
        assertEquals(KeyDef.MAX_WIDTH_MULTIPLIER, wild.bottomKeys.first().widthMultiplier, 0.0001f)
        assertEquals(KeyDef.MIN_HEIGHT_MULTIPLIER, wild.bottomKeys.first().heightMultiplier, 0.0001f)

        // Ikut tersimpan dan terbaca kembali.
        val restored = KeyDef.fromJson(
            JSONObject(KeyDef(KeyAction.ENTER, widthMultiplier = 1.5f, heightMultiplier = 0.8f).toJson().toString())
        )
        assertEquals(1.5f, restored.widthMultiplier, 0.0001f)
        assertEquals(0.8f, restored.heightMultiplier, 0.0001f)
    }

    @Test
    fun `lebar tombol bawah diatur sendiri tanpa mengubah tingginya`() {
        // Tinggi tombol bawah selalu sama dengan tombol samping (rumus GridMath.boardHeightPx),
        // hanya lebarnya yang bisa berbeda: lebar = tinggi x pengali.
        val square = KeyDef(KeyAction.ENTER)
        assertEquals(1f, square.widthMultiplier, 0.0001f)
        val wide = KeyDef(KeyAction.SPACE, widthMultiplier = 2.5f)
        assertEquals(80, (32 * wide.widthMultiplier).toInt())

        // Nilai liar dipangkas ke 1x-4x.
        val clamped = PenboardConfig(
            bottomKeys = listOf(KeyDef(KeyAction.SPACE, widthMultiplier = 40f))
        ).sanitized()
        assertEquals(KeyDef.MAX_WIDTH_MULTIPLIER, clamped.bottomKeys.first().widthMultiplier, 0.0001f)

        // Pengali ikut tersimpan dan dibaca kembali.
        val restored = KeyDef.fromJson(JSONObject(KeyDef(KeyAction.SPACE, widthMultiplier = 2f).toJson().toString()))
        assertEquals(KeyAction.SPACE, restored.action)
        assertEquals(2f, restored.widthMultiplier, 0.0001f)
    }

    @Test
    fun `preset HiWrite menyetel tata letak dan tema seperti HiWrite`() {
        val custom = PenboardConfig(
            boardLines = 2,
            penColorIndex = 2,
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
        assertEquals(4, preset.boardLines)
        assertEquals(4, preset.gridRows)
        assertEquals(PenboardConfig.COLUMNS, preset.gridColumns)
        // HiWrite: 3 kolom x 6 baris = 18 tombol, ukurannya otomatis.
        assertEquals(PenboardConfig.COLUMNS * 4, preset.gridButtonCount)
        assertEquals(0, preset.gridCellDp)
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
            boardLines = 5,
            lineGapDp = 52,
            penWidthDp = 5,
            gridButtonCount = 4,
            gridColumns = 2,
            gridCellDp = 56,
            gridRowsFollow = false,
            gridRows = 5,
            panelWidthFraction = 0.62f,
            boardWidthFraction = 0.72f,
            panelOffsetX = 0.25f,
            panelOffsetY = 0.5f,
            floatingPanel = false,
            autoRecognizeDelayMs = 1500,
            recognitionLanguage = "en-US",
            penButtonPrimary = PenButtonAction.CLEAR
        ).sanitized()
        val restored = PenboardConfig.fromJson(JSONObject(config.toJson().toString()))
        assertEquals(config.boardLines, restored.boardLines)
        assertEquals(config.lineGapDp, restored.lineGapDp)
        assertEquals(config.penWidthDp, restored.penWidthDp)
        assertEquals(config.gridButtonCount, restored.gridButtonCount)
        assertEquals(config.gridColumns, restored.gridColumns)
        assertFalse(restored.gridRowsFollow)
        assertEquals(config.effectiveGridRows, restored.effectiveGridRows)
        assertEquals(config.gridCellDp, restored.gridCellDp)
        assertEquals(config.panelWidthFraction, restored.panelWidthFraction, 0.0001f)
        assertEquals(config.boardWidthFraction, restored.boardWidthFraction, 0.0001f)
        assertEquals(config.panelOffsetX, restored.panelOffsetX, 0.0001f)
        assertEquals(config.panelOffsetY, restored.panelOffsetY, 0.0001f)
        assertFalse(restored.floatingPanel)
        assertEquals(config.autoRecognizeDelayMs, restored.autoRecognizeDelayMs)
        assertEquals("en-US", restored.recognitionLanguage)
        assertEquals(PenButtonAction.CLEAR, restored.penButtonPrimary)
        assertEquals(config.penColors, restored.penColors)
        assertEquals(config.boardColor, restored.boardColor)
        assertEquals(config.gridKeys, restored.gridKeys)

        // Lebar tombol bawah ikut tersimpan (tingginya tetap mengikuti tombol samping).
        val withWidth = PenboardConfig(
            bottomKeys = PenboardConfig.defaultBottomKeys().toMutableList()
                .also { it[0] = it[0].copy(widthMultiplier = 1.8f) }
        ).sanitized()
        val widthRestored = PenboardConfig.fromJson(JSONObject(withWidth.toJson().toString()))
        assertEquals(1.8f, widthRestored.bottomKeys.first().widthMultiplier, 0.0001f)
        assertEquals(withWidth.bottomKeys, widthRestored.bottomKeys)
    }

    @Test
    fun `angka liar dipangkas ke rentang aman`() {
        val wild = PenboardConfig(
            lineGapDp = 1,
            penWidthDp = 99,
            boardLines = 99,
            gridColumns = 99,
            gridButtonCount = 77,
            gridCellDp = 9999,
            panelWidthFraction = 0.1f,
            boardWidthFraction = 0.1f,
            panelOffsetX = -4f,
            panelOffsetY = 5f,
            cornerRadiusDp = -5,
            autoRecognizeDelayMs = 50,
            panelOpacity = 500,
            boardOpacity = -20,
            buttonOpacity = 900,
            recognitionLanguage = "xx-YY"
        ).sanitized()
        assertEquals(PenboardConfig.MAX_BOARD_LINES, wild.boardLines)
        assertEquals(PenboardConfig.MIN_LINE_GAP_DP, wild.lineGapDp)
        assertEquals(PenboardConfig.MAX_PEN_WIDTH_DP, wild.penWidthDp)
        // Semua sel terisi: 6 kolom x 6 baris = 36 tombol.
        assertEquals(PenboardConfig.MAX_GRID_KEYS, wild.gridButtonCount)
        assertEquals(PenboardConfig.MAX_GRID_COLUMNS, wild.gridColumns)
        assertEquals(PenboardConfig.MAX_CELL_DP, wild.gridCellDp)
        assertEquals(PenboardConfig.MIN_PANEL_WIDTH_FRACTION, wild.panelWidthFraction, 0.0001f)
        assertEquals(PenboardConfig.MIN_BOARD_WIDTH_FRACTION, wild.boardWidthFraction, 0.0001f)
        assertEquals(0f, wild.panelOffsetX, 0.0001f)
        assertEquals(1f, wild.panelOffsetY, 0.0001f)
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
        // 36 slot: cukup untuk 6 kolom x 6 baris, dan isian tambahannya tetap berfungsi.
        assertEquals(36, keys.size)
        assertTrue(keys.any { it.action == KeyAction.PASTE })
        assertTrue(keys.any { it.action == KeyAction.COPY })
        assertTrue(keys.take(PenboardConfig.COLUMNS * PenboardConfig.MAX_GRID_ROWS).any { it.action == KeyAction.HIDE })
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
                "text", "charPad", "recognize", "clear", "deleteStroke", "undo", "redo", "cyclePen",
                "space", "enter", "backspace", "deleteWord", "deleteForward", "tab",
                "cursorLeft", "cursorRight", "cursorUp", "cursorDown", "cursorHome", "cursorEnd",
                "cursorWordLeft", "cursorWordRight", "cursorDocStart", "cursorDocEnd", "pageUp", "pageDown",
                "selectAll", "cut", "copy", "paste", "selectWord", "textUndo", "textRedo",
                "shiftTab", "escape", "editorAction",
                "systemHandwriting", "keyboardPicker", "hide"
            ),
            KeyAction.entries.map { it.id }
        )
    }

    @Test
    fun `pengaturan lama tetap terbaca setelah papan melayang dihapus`() {
        // Berkas lama masih memakai "boardHeightDp"; sekarang digantikan "boardLines"
        // (bawaan 3 baris), jadi berkas lama tetap terbaca tanpa error.
        val json = JSONObject(
            """{"boardHeightDp":176,"gridButtonCount":2,"gridKeys":[{"action":"hide"},{"action":"overlay"}]}"""
        )
        val config = PenboardConfig.fromJson(json)
        assertEquals(PenboardConfig.DEFAULT_BOARD_LINES, config.boardLines)
        assertEquals(PenboardConfig.DEFAULT_BOARD_LINES, config.effectiveGridRows)
        // Tombol lama "overlay" (papan melayang) jatuh ke SPACE, bukan menggagalkan pembacaan.
        assertEquals(KeyAction.HIDE, config.gridKeys.first().action)
        assertEquals(KeyAction.SPACE, config.gridKeys[1].action)
        assertEquals(PenboardConfig.GRID_SLOTS, config.gridKeys.size)
        assertEquals(KeyAction.SPACE, KeyAction.fromId("overlay"))
        assertEquals(KeyAction.CHAR_PAD, KeyAction.fromId("charPad"))
        val character = KeyDef.fromJson(JSONObject().put("action", "text").put("text", "@"))
        assertEquals("@", character.text)
        assertEquals(KeyboardChars.MAX_LENGTH, KeyDef.fromJson(
            JSONObject().put("action", "text").put("text", "x".repeat(200))
        ).text.length)
        // Id tombol Penboard sekarang tidak ada yang menyebut papan melayang.
        assertFalse(KeyAction.entries.map { it.id }.contains("overlay"))
    }

    @Test
    fun `bilah atas punya bawaan dan batas aman`() {
        val config = PenboardConfig()
        assertEquals(PenboardConfig.DEFAULT_BOTTOM_KEYS, config.bottomButtonCount)
        // Bilah atas sampai 80 tombol; bawaan 8 aksi mengisi slot pertama.
        assertEquals(PenboardConfig.defaultBottomKeys().size, config.bottomKeys.size)
        assertEquals(80, PenboardConfig.MAX_BOTTOM_KEYS)
        assertEquals(80, PenboardConfig.BOTTOM_SLOTS)
        assertEquals(1, PenboardConfig.DEFAULT_BOTTOM_ROWS)
        assertEquals(8, PenboardConfig.MAX_BOTTOM_ROWS)
        assertEquals(
            listOf(
                KeyAction.RECOGNIZE, KeyAction.ENTER, KeyAction.DELETE_STROKE,
                KeyAction.CLEAR, KeyAction.UNDO, KeyAction.REDO, KeyAction.CYCLE_PEN,
                KeyAction.HIDE
            ),
            config.bottomKeys.map { it.action }
        )

        val many = PenboardConfig(bottomButtonCount = 99, bottomKeys = emptyList()).sanitized()
        assertEquals(PenboardConfig.MAX_BOTTOM_KEYS, many.bottomButtonCount)
        assertEquals(PenboardConfig.BOTTOM_SLOTS, many.bottomKeys.size)
        // Daftar bawaan tidak lebih panjang dari jumlah slot, dan slot tambahan
        // memakai bawaan bergiliran (bukan tombol mati).
        assertTrue(PenboardConfig.defaultBottomKeys().size <= PenboardConfig.BOTTOM_SLOTS)
        assertEquals(PenboardConfig.BOTTOM_SLOTS, many.bottomKeys.size)
        val rows = PenboardConfig(bottomRowCount = 0).sanitized()
        assertEquals(PenboardConfig.MIN_BOTTOM_ROWS, rows.bottomRowCount)
        val tooManyRows = PenboardConfig(bottomRowCount = 40).sanitized()
        assertEquals(PenboardConfig.MAX_BOTTOM_ROWS, tooManyRows.bottomRowCount)
        assertEquals(
            PenboardConfig.defaultBottomKeys().first().action,
            many.bottomKeys[PenboardConfig.defaultBottomKeys().size].action
        )

        // Bilah bawah bisa dimatikan seluruhnya.
        val none = PenboardConfig(bottomButtonCount = -4).sanitized()
        assertEquals(PenboardConfig.MIN_BOTTOM_KEYS, none.bottomButtonCount)
    }

    @Test
    fun `tiap tombol atas bawaan benar-benar melakukan sesuatu`() {
        val defaults = PenboardConfig.defaultBottomKeys()
        assertTrue(defaults.isNotEmpty())
        assertTrue(defaults.size <= PenboardConfig.BOTTOM_SLOTS)
        defaults.forEach { key ->
            // Aksi TEXT tanpa teks hanya "tombol mati"; dengan teks berarti mengirim teks.
            val deadText = key.action == KeyAction.TEXT && key.text.isBlank()
            assertTrue("tombol bawah ${key.action} tidak melakukan apa pun", !deadText)
        }
        // Tombol yang tampil sejak awal (5 pertama) juga bukan tombol mati.
        assertTrue(
            defaults.take(PenboardConfig.DEFAULT_BOTTOM_KEYS)
                .none { it.action == KeyAction.TEXT && it.text.isBlank() }
        )
    }

    @Test
    fun `gaya ikon bawaan pixel art dan tersimpan bolak-balik`() {
        assertEquals(IconStyle.PIXEL, PenboardConfig().iconStyle)
        assertEquals(IconStyle.VECTOR, IconStyle.fromId("vector"))
        assertEquals(IconStyle.PIXEL, IconStyle.fromId("entah"))
        assertEquals(IconStyle.PIXEL, IconStyle.fromId(null))

        val vector = PenboardConfig(iconStyle = IconStyle.VECTOR)
        val restored = PenboardConfig.fromJson(JSONObject(vector.toJson().toString()))
        assertEquals(IconStyle.VECTOR, restored.iconStyle)

        // Berkas pengaturan lama (tanpa bidang iconStyle) tetap terbaca sebagai pixel art.
        val legacy = PenboardConfig.fromJson(JSONObject("{\"gridRows\":2}"))
        assertEquals(IconStyle.PIXEL, legacy.iconStyle)
    }

    @Test
    fun `preset HiWrite memakai gaya ikon pixel art`() {
        val preset = PenboardConfig(iconStyle = IconStyle.VECTOR).withHiWritePreset()
        assertEquals(IconStyle.PIXEL, preset.iconStyle)
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
    fun `tombol minimize tetap tersedia walau bingkai dimatikan`() {
        val frameOn = PenboardConfig()
        assertTrue(frameOn.showsTitleBar)
        assertEquals(PenboardConfig.TITLE_BAR_DP, frameOn.titleBarHeightDp())

        // Bingkai mati, minimize nyala -> bilah kecil setinggi MINI_BAR_DP.
        val mini = PenboardConfig(windowFrame = false, minimizeButton = true)
        assertTrue(mini.showsTitleBar)
        assertEquals(PenboardConfig.MINI_BAR_DP, mini.titleBarHeightDp())
        assertTrue(PenboardConfig.MINI_BAR_DP < PenboardConfig.TITLE_BAR_DP)

        // Keduanya mati -> tidak ada baris atas sama sekali.
        val none = PenboardConfig(windowFrame = false, minimizeButton = false)
        assertFalse(none.showsTitleBar)
        assertEquals(0, none.titleBarHeightDp())
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
        assertFalse(config.bottomRowsAssigned)
        assertEquals(AppTheme.DARK, config.appTheme)
        assertEquals(0, config.bottomKeys.first().row)
    }

    @Test
    fun `baris tombol dan tema tersimpan`() {
        val config = PenboardConfig(
            appTheme = AppTheme.LIGHT,
            bottomRowsAssigned = true,
            bottomRowCount = 3,
            bottomKeys = listOf(KeyDef(KeyAction.SPACE, row = 2), KeyDef(KeyAction.ENTER, row = 3))
        ).sanitized()
        val restored = PenboardConfig.fromJson(JSONObject(config.toJson().toString()))
        assertEquals(AppTheme.LIGHT, restored.appTheme)
        assertTrue(restored.bottomRowsAssigned)
        assertEquals(2, restored.bottomKeys[0].row)
        assertEquals(3, restored.bottomKeys[1].row)
        assertEquals(0, KeyDef.fromJson(JSONObject("{\"action\":\"space\"}")).row)
    }

    @Test
    fun `memilih baris mengunci urutan lama lalu memindahkan satu tombol`() {
        val config = PenboardConfig(
            bottomButtonCount = 4,
            bottomRowCount = 1,
            bottomKeys = listOf(
                KeyDef(KeyAction.SPACE),
                KeyDef(KeyAction.ENTER),
                KeyDef(KeyAction.DELETE),
                KeyDef(KeyAction.CLEAR)
            )
        ).sanitized()
        val moved = config.assignTopRow(1, 2, perRow = 2)
        assertTrue(moved.bottomRowsAssigned)
        assertEquals(2, moved.bottomRowCount)
        assertEquals(1, moved.bottomKeys[0].row)
        assertEquals(2, moved.bottomKeys[1].row)
        assertEquals(KeyAction.ENTER, moved.bottomKeys[1].action)
        assertEquals(2, moved.bottomKeys[2].row)
    }

    @Test
    fun `tema terang memasang warna kertas dan tetap bisa diganti`() {
        val light = PenboardConfig().withLightTheme()
        assertEquals(AppTheme.LIGHT, light.appTheme)
        assertFalse(light.scanlines)
        assertEquals(0xFF1C1C1C.toInt(), light.buttonTextColor)
        val custom = light.copy(boardColor = 0xFF00FF00.toInt()).sanitized()
        assertEquals(0xFF00FF00.toInt(), custom.boardColor)
        assertEquals(AppTheme.LIGHT, custom.appTheme)
        assertEquals(160, PenboardConfig.MAX_LINE_GAP_DP)
    }
}
