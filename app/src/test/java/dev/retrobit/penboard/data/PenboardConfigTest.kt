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
        assertEquals(9, config.gridButtonCount)
        assertEquals(PenboardConfig.GRID_SLOTS, config.gridKeys.size)
        assertEquals(2000, config.autoRecognizeDelayMs)
        assertTrue(config.autoRecognize)
        assertTrue(config.autoSpace)
        assertTrue(config.pressureEnabled)
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

        val none = PenboardConfig(penColors = emptyList(), penColorIndex = 3).sanitized()
        assertTrue(none.penColors.isNotEmpty())
        assertEquals(0, none.penColorIndex)
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
    fun `grid bawaan memuat tombol penting`() {
        val keys = PenboardConfig.defaultGridKeys()
        assertEquals(PenboardConfig.GRID_SLOTS, keys.size)
        assertTrue(keys.any { it.action == KeyAction.RECOGNIZE })
        assertTrue(keys.any { it.action == KeyAction.SPACE })
        assertTrue(keys.any { it.action == KeyAction.BACKSPACE && it.repeatOnHold })
    }

    @Test
    fun `berkas rusak tidak merusak pengaturan`() {
        val restored = PenboardConfig.fromJson(JSONObject("""{"gridKeys":[],"penColors":[]}"""))
        assertEquals(PenboardConfig.GRID_SLOTS, restored.gridKeys.size)
        assertTrue(restored.penColors.isNotEmpty())
        assertFalse(restored.gridButtonCount <= 0)
    }
}
