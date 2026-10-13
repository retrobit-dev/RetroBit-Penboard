package dev.retrobit.penboard.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenboardConfigTest {

    @Test
    fun `pengaturan bawaan siap dipakai menulis`() {
        val config = PenboardConfig()
        assertTrue("input otomatis aktif secara bawaan", config.autoRecognize)
        assertEquals("jeda 2 detik", 2000, config.autoRecognizeDelayMs)
        assertTrue("spasi otomatis antar kata", config.autoSpace)
        assertTrue("tulisan dibersihkan setelah diinput", config.clearAfterRecognize)
        assertEquals("id-ID", config.recognitionLanguage)
        assertEquals(PenboardConfig.MAX_GRID_KEYS, config.gridButtonCount)
        assertEquals(PenboardConfig.GRID_SLOTS, config.gridKeys.size)
        assertEquals(KeyAction.RECOGNIZE, config.gridKeys.first().action)
        assertEquals(PenButtonAction.RECOGNIZE, config.penButtonPrimary)
        assertEquals(PenButtonAction.UNDO, config.penButtonSecondary)
        assertFalse(config.allowFinger)
        assertEquals(PenboardConfig.DEFAULT_PEN_COLORS.first(), config.currentPenColor)
    }

    @Test
    fun `putaran json mempertahankan pengaturan pilihan pengguna`() {
        val original = PenboardConfig(
            boardHeightDp = 220,
            lineGapDp = 60,
            penWidthDp = 6,
            panelOpacity = 40,
            boardOpacity = 55,
            buttonOpacity = 70,
            cornerRadiusDp = 20,
            autoRecognize = false,
            autoRecognizeDelayMs = 3200,
            autoSpace = false,
            clearAfterRecognize = false,
            recognitionLanguage = "en-US",
            gridButtonCount = 4,
            showLines = false,
            allowFinger = true,
            gridKeys = PenboardConfig.defaultGridKeys().toMutableList().also {
                it[0] = KeyDef(KeyAction.CLEAR, label = "Hapus semua")
                it[3] = KeyDef(KeyAction.BACKSPACE, repeatOnHold = true)
            }
        )
        val restored = PenboardConfig.fromJson(JSONObject(original.toJson().toString()))
        assertEquals(original.boardHeightDp, restored.boardHeightDp)
        assertEquals(original.lineGapDp, restored.lineGapDp)
        assertEquals(original.penWidthDp, restored.penWidthDp)
        assertEquals(original.panelOpacity, restored.panelOpacity)
        assertEquals(original.boardOpacity, restored.boardOpacity)
        assertEquals(original.buttonOpacity, restored.buttonOpacity)
        assertEquals(original.cornerRadiusDp, restored.cornerRadiusDp)
        assertEquals(original.autoRecognize, restored.autoRecognize)
        assertEquals(original.autoRecognizeDelayMs, restored.autoRecognizeDelayMs)
        assertEquals(original.autoSpace, restored.autoSpace)
        assertEquals(original.clearAfterRecognize, restored.clearAfterRecognize)
        assertEquals(original.recognitionLanguage, restored.recognitionLanguage)
        assertEquals(original.gridButtonCount, restored.gridButtonCount)
        assertEquals(original.showLines, restored.showLines)
        assertEquals(original.allowFinger, restored.allowFinger)
        assertEquals("Hapus semua", restored.gridKeys.first().label)
        assertTrue(restored.gridKeys[3].repeatOnHold)
        assertEquals(original.penColors, restored.penColors)
        assertEquals(original.panelColor, restored.panelColor)
        assertEquals(original.boardColor, restored.boardColor)
        assertEquals(original.lineColor, restored.lineColor)
        assertEquals(original.buttonColor, restored.buttonColor)
        assertEquals(original.buttonTextColor, restored.buttonTextColor)
        assertEquals(original.accentColor, restored.accentColor)
    }

    @Test
    fun `angka di luar batas dipangkas`() {
        val sanitized = PenboardConfig(
            boardHeightDp = 9999,
            lineGapDp = 1,
            penWidthDp = 99,
            cornerRadiusDp = -5,
            autoRecognizeDelayMs = 99_999,
            gridButtonCount = 0,
            panelOpacity = 500,
            boardOpacity = -20,
            buttonOpacity = 300
        ).sanitized()
        assertEquals(PenboardConfig.MAX_BOARD_HEIGHT_DP, sanitized.boardHeightDp)
        assertEquals(PenboardConfig.MIN_LINE_GAP_DP, sanitized.lineGapDp)
        assertEquals(PenboardConfig.MAX_PEN_WIDTH_DP, sanitized.penWidthDp)
        assertEquals(PenboardConfig.MIN_RADIUS_DP, sanitized.cornerRadiusDp)
        assertEquals(PenboardConfig.MAX_AUTO_DELAY_MS, sanitized.autoRecognizeDelayMs)
        assertEquals(PenboardConfig.MIN_GRID_KEYS, sanitized.gridButtonCount)
        assertEquals(100, sanitized.panelOpacity)
        assertEquals(0, sanitized.boardOpacity)
        assertEquals(100, sanitized.buttonOpacity)
    }

    @Test
    fun `berkas lama tanpa kolom baru memakai bawaan`() {
        val migrated = PenboardConfig.fromJson(JSONObject("""{"boardHeightDp":200}"""))
        assertEquals(200, migrated.boardHeightDp)
        assertTrue(migrated.autoRecognize)
        assertEquals(2000, migrated.autoRecognizeDelayMs)
        assertTrue(migrated.autoSpace)
        assertEquals("id-ID", migrated.recognitionLanguage)
        assertEquals(PenboardConfig.GRID_SLOTS, migrated.gridKeys.size)
    }

    @Test
    fun `slot grid yang kosong diisi tombol bawaan`() {
        val config = PenboardConfig(gridKeys = listOf(KeyDef(KeyAction.CLEAR))).sanitized()
        assertEquals(PenboardConfig.GRID_SLOTS, config.gridKeys.size)
        assertEquals(KeyAction.CLEAR, config.gridKeys.first().action)
        assertEquals(PenboardConfig.defaultGridKeys()[1].action, config.gridKeys[1].action)
        assertEquals(PenboardConfig.defaultGridKeys()[8].action, config.gridKeys[8].action)
    }

    @Test
    fun `label tombol memakai label khusus atau teks aksi`() {
        val config = PenboardConfig()
        assertEquals("Teks", config.labelFor(KeyDef(KeyAction.RECOGNIZE)))
        assertEquals("Kirim", config.labelFor(KeyDef(KeyAction.RECOGNIZE, label = "Kirim")))
        assertEquals("halo", config.labelFor(KeyDef(KeyAction.TEXT, text = "halo")))
    }

    @Test
    fun `aksi tidak dikenal kembali ke bawaan yang aman`() {
        assertEquals(KeyAction.SPACE, KeyAction.fromId("entah"))
        assertEquals(KeyAction.UNDO, KeyAction.fromId("undo"))
        assertEquals(KeyAction.SYSTEM_HANDWRITING, KeyAction.fromId("systemHandwriting"))
        assertEquals(PenButtonAction.NONE, PenButtonAction.fromId(null))
        assertEquals(PenButtonAction.CLEAR, PenButtonAction.fromId("clear"))
        assertEquals(KeyAction.CLEAR, PenButtonAction.fromId("clear").key)
    }

    @Test
    fun `warna pena terpilih selalu valid`() {
        val config = PenboardConfig(penColors = listOf(0xFF000000.toInt()), penColorIndex = 5).sanitized()
        assertEquals(1, config.penColors.size)
        assertEquals(0, config.penColorIndex)
        assertEquals(0xFF000000.toInt(), config.currentPenColor)
    }

    @Test
    fun `jumlah warna pena dibatasi`() {
        val many = (0 until 20).map { 0xFF000000.toInt() + it }
        val config = PenboardConfig(penColors = many).sanitized()
        assertEquals(PenboardConfig.MAX_PEN_COLORS, config.penColors.size)
    }

    @Test
    fun `bahasa yang tidak didukung kembali ke indonesia`() {
        val config = PenboardConfig(recognitionLanguage = "xx-YY").sanitized()
        assertEquals("id-ID", config.recognitionLanguage)
        assertEquals("id-ID", PenboardConfig(recognitionLanguage = "").sanitized().recognitionLanguage)
    }
}
