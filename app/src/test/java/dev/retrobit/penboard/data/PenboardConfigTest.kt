package dev.retrobit.penboard.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenboardConfigTest {

    @Test
    fun `preset bawaan selamat dari putaran json`() {
        Presets.builtins().forEach { preset ->
            // data yang disimpan selalu sudah disanitasi (grid 3x3 selalu lengkap)
            val original = preset.sanitized()
            val restored = PenboardConfig.fromJson(JSONObject(original.toJson().toString()))
            assertEquals(original.id, restored.id)
            assertEquals(original.name, restored.name)
            assertEquals(original.builtinId, restored.builtinId)
            assertEquals(original.boardHeightDp, restored.boardHeightDp)
            assertEquals(original.lineGapDp, restored.lineGapDp)
            assertEquals(original.boardBgColor, restored.boardBgColor)
            assertEquals(original.boardBgOpacity, restored.boardBgOpacity)
            assertEquals(original.strokeColor, restored.strokeColor)
            assertEquals(original.toolbarSide, restored.toolbarSide)
            assertEquals(original.boardLayout, restored.boardLayout)
            assertEquals(original.gridButtonCount, restored.gridButtonCount)
            assertEquals(original.gridKeys.size, restored.gridKeys.size)
            assertEquals(original.gridKeys.first().action, restored.gridKeys.first().action)
            assertEquals(original.autoSpaceAfterWord, restored.autoSpaceAfterWord)
            assertEquals(original.penButtonPrimary, restored.penButtonPrimary)
            assertEquals(original.penButtonSecondary, restored.penButtonSecondary)
            assertEquals(original.autoRecognize, restored.autoRecognize)
            assertEquals(original.autoRecognizeDelayMs, restored.autoRecognizeDelayMs)
            assertEquals(original.recognitionLanguage, restored.recognitionLanguage)
            assertEquals(original.recognitionLines, restored.recognitionLines)
            assertEquals(original.clearAfterRecognize, restored.clearAfterRecognize)
            assertEquals(original.penColors, restored.penColors)
            assertEquals(original.toolbarRows.size, restored.toolbarRows.size)
            assertEquals(original.boardRows.size, restored.boardRows.size)
            assertEquals(original.boardRows.first().keys.size, restored.boardRows.first().keys.size)
            assertEquals(original.boardRows.first().keys.first().action, restored.boardRows.first().keys.first().action)
        }
    }

    @Test
    fun `setiap preset punya id unik dan baris yang tidak kosong`() {
        val presets = Presets.builtins()
        assertEquals(presets.size, presets.map { it.id }.toSet().size)
        presets.forEach { preset ->
            assertTrue(preset.name.isNotBlank())
            assertTrue(preset.toolbarRows.isNotEmpty())
            assertTrue(preset.boardRows.isNotEmpty())
            preset.toolbarRows.forEach { row -> assertTrue(row.keys.isNotEmpty()) }
            preset.boardRows.forEach { row -> assertTrue(row.keys.isNotEmpty()) }
            assertNotNull(preset.builtinId)
        }
    }

    @Test
    fun `nilai di luar batas dipangkas saat sanitasi`() {
        val wild = PenboardConfig(
            name = "  ",
            boardHeightDp = 99_999,
            lineGapDp = 1,
            boardBgOpacity = 500,
            strokeWidthDp = 100f,
            keyHeightDp = 1,
            toolbarRows = emptyList(),
            boardRows = emptyList()
        ).sanitized()

        assertEquals("Papan", wild.name)
        assertEquals(600, wild.boardHeightDp)
        assertEquals(22, wild.lineGapDp)
        assertEquals(100, wild.boardBgOpacity)
        assertEquals(16f, wild.strokeWidthDp, 0.001f)
        assertEquals(30, wild.keyHeightDp)
        assertTrue(wild.toolbarRows.isNotEmpty())
        assertTrue(wild.boardRows.isNotEmpty())
    }

    @Test
    fun `json rusak tetap menghasilkan profil yang bisa dipakai`() {
        val broken = JSONObject("""{"name":"Uji","toolbarRows":[],"boardRows":[]}""")
        val config = PenboardConfig.fromJson(broken)
        assertTrue(config.toolbarRows.isNotEmpty())
        assertTrue(config.boardRows.isNotEmpty())
        assertEquals("Uji", config.name)
    }

    @Test
    fun `duplikat memakai id baru tetapi pengaturan sama`() {
        val original = Presets.standard()
        val copy = original.boardCopy("Salinan")
        assertTrue(copy.id != original.id)
        assertEquals("Salinan", copy.name)
        assertEquals(original.boardHeightDp, copy.boardHeightDp)
        assertEquals(original.boardRows.first().keys.map { it.action }, copy.boardRows.first().keys.map { it.action })
        assertTrue(copy.boardRows.first().keys.first().id != original.boardRows.first().keys.first().id)
    }

    @Test
    fun `warna dapat digabung dengan transparansi`() {
        val solid = Colors.withOpacity(0xFF102030.toInt(), 100)
        assertEquals(0xFF102030.toInt(), solid)
        val half = Colors.withOpacity(0xFF102030.toInt(), 50)
        assertEquals(127, half ushr 24)
        val transparent = Colors.withOpacity(0xFF102030.toInt(), 0)
        assertEquals(0, transparent ushr 24)
    }

    @Test
    fun `tata letak grid 3x3 punya sembilan slot yang selalu lengkap`() {
        val config = PenboardConfig(gridKeys = listOf(KeyDef.text("a")), gridButtonCount = 4)
        val safe = config.sanitized()
        assertEquals(PenboardConfig.GRID_SLOTS, safe.gridKeys.size)
        assertEquals(4, safe.gridButtonCount)
        assertEquals("a", safe.gridKeys.first().text)
        // slot sisa diisi tombol bawaan PenboardConfig
        assertEquals(PenboardConfig.defaultGridKeys()[1].action, safe.gridKeys[1].action)
    }

    @Test
    fun `jumlah tombol grid dibatasi satu sampai sembilan`() {
        assertEquals(1, PenboardConfig(gridButtonCount = 0).sanitized().gridButtonCount)
        assertEquals(9, PenboardConfig(gridButtonCount = 50).sanitized().gridButtonCount)
        assertEquals(3, PenboardConfig(gridButtonCount = 3).sanitized().gridButtonCount)
    }

    @Test
    fun `tata letak bawaan adalah papan di kiri dengan tombol grid`() {
        assertEquals(BoardLayoutMode.BOARD_LEFT_GRID, PenboardConfig().boardLayout)
        // data lama tanpa keterangan tata letak -> memakai tata letak grid
        assertEquals(BoardLayoutMode.BOARD_LEFT_GRID, BoardLayoutMode.fromId(null))
        assertEquals(BoardLayoutMode.BOARD_LEFT_GRID, BoardLayoutMode.fromId("grid"))
        assertEquals(BoardLayoutMode.STACKED, BoardLayoutMode.fromId("stacked"))
        // nilai tak dikenal tetap aman
        assertEquals(BoardLayoutMode.BOARD_LEFT_GRID, BoardLayoutMode.fromId("aneh"))
    }

    @Test
    fun `profil lama dipindahkan ke perilaku satu tulisan satu kata`() {
        // JSON versi lama: belum ada schemaVersion & masih clearAfterRecognize=false
        val old = JSONObject("""{"name":"Lama","clearAfterRecognize":false}""")
        val migrated = PenboardConfig.fromJson(old)
        assertTrue("tulisan harus dibersihkan setelah dikenali", migrated.clearAfterRecognize)
        assertTrue("spasi otomatis harus aktif", migrated.autoSpaceAfterWord)
        assertTrue("input otomatis harus aktif", migrated.autoRecognize)
        assertEquals("jeda bawaan 2 detik", 2000, migrated.autoRecognizeDelayMs)
        assertEquals(BoardLayoutMode.BOARD_LEFT_GRID, migrated.boardLayout)

        // profil baru tetap menghormati pilihan pengguna
        val chosen = PenboardConfig(
            clearAfterRecognize = false,
            autoSpaceAfterWord = false,
            autoRecognize = false,
            autoRecognizeDelayMs = 3000,
            schemaVersion = PenboardConfig.SCHEMA_VERSION
        )
        val restored = PenboardConfig.fromJson(JSONObject(chosen.toJson().toString()))
        assertFalse(restored.clearAfterRecognize)
        assertFalse(restored.autoSpaceAfterWord)
        assertFalse(restored.autoRecognize)
        assertEquals(3000, restored.autoRecognizeDelayMs)
    }

    @Test
    fun `aksi tombol pena dibaca dengan aman`() {
        assertEquals(PenButtonAction.RECOGNIZE, PenButtonAction.fromId("recognize"))
        assertEquals(PenButtonAction.ERASE, PenButtonAction.fromId("erase"))
        assertEquals(PenButtonAction.UNDO, PenButtonAction.fromId("undo"))
        // nilai tak dikenal -> kembali ke bawaan yang diberikan
        assertEquals(PenButtonAction.RECOGNIZE, PenButtonAction.fromId("tidak-ada"))
        assertEquals(PenButtonAction.UNDO, PenButtonAction.fromId(null, PenButtonAction.UNDO))
        assertEquals(PenButtonAction.NONE, PenButtonAction.fromId("none"))
    }

    @Test
    fun `pengaturan pengenalan tulisan punya nilai bawaan yang masuk akal`() {
        val config = PenboardConfig()
        assertEquals(PenButtonAction.RECOGNIZE, config.penButtonPrimary)
        assertEquals(PenButtonAction.UNDO, config.penButtonSecondary)
        assertTrue("input otomatis harus aktif secara bawaan", config.autoRecognize)
        assertEquals(2000, config.autoRecognizeDelayMs)
        assertTrue("tulisan dibersihkan setelah diinput", config.clearAfterRecognize)
        assertTrue("spasi otomatis antar kata", config.autoSpaceAfterWord)
        assertEquals("id-ID", config.recognitionLanguage)

        // jeda di luar batas dipangkas (minimal 0,5 detik, maksimal 5 detik)
        assertEquals(500, config.copy(autoRecognizeDelayMs = 10).sanitized().autoRecognizeDelayMs)
        assertEquals(5000, config.copy(autoRecognizeDelayMs = 99_999).sanitized().autoRecognizeDelayMs)
        // profil lama tanpa bidang ini tetap memakai bawaan
        val old = PenboardConfig.fromJson(JSONObject("""{"name":"Lama"}"""))
        assertEquals(PenButtonAction.RECOGNIZE, old.penButtonPrimary)
        assertEquals("id-ID", old.recognitionLanguage)
    }

    @Test
    fun `warna bisa dicampur dan kontras dihitung`() {
        val mixed = Colors.blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0.5f)
        assertEquals(127, Colors.red(mixed))
        assertEquals(255, Colors.alpha(0xFF000000.toInt()))
        assertEquals(0xFF0F172A.toInt(), Colors.contrastFor(0xFFFFFFFF.toInt()))
        assertEquals(0xFFF8FAFC.toInt(), Colors.contrastFor(0xFF000000.toInt()))
    }

    @Test
    fun `hex warna bisa dibaca kembali`() {
        val color = 0xFF3D5AFE.toInt()
        assertEquals(color, Colors.parseHex(Colors.toHex(color), 0))
        assertEquals(color, Colors.parseHex("3d5afe", 0))
        assertEquals(color, Colors.parseHex("#3D5AFE", 0))
    }
}
