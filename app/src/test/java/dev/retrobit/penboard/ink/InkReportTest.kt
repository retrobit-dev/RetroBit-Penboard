package dev.retrobit.penboard.ink

import dev.retrobit.penboard.board.InkPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InkReportTest {

    private fun stroke(vararg coords: Float): NormalizedStroke {
        val points = mutableListOf<InkPoint>()
        var index = 0
        while (index + 1 < coords.size) {
            points.add(InkPoint(coords[index], coords[index + 1]))
            index += 2
        }
        return NormalizedStroke(points, 0)
    }

    @Test
    fun `ringkasan tinta menghitung goresan titik dan kotak`() {
        val ink = InkReport.inkOf(listOf(stroke(0f, 0f, 100f, 40f), stroke(10f, 0f, 60f, 30f)))
        assertEquals(2, ink?.strokes)
        assertEquals(4, ink?.points)
        assertEquals(100, ink?.width)
        assertEquals(40, ink?.height)
    }

    @Test
    fun `tinta kosong tidak punya ringkasan`() {
        assertNull(InkReport.inkOf(emptyList()))
        assertNull(InkReport.inkOf(listOf(NormalizedStroke(emptyList(), 0))))
    }

    @Test
    fun `laporan memuat semua bagian penting`() {
        val report = InkReport.lines(
            appVersion = "3.0.6",
            engineAvailable = true,
            language = "Indonesia",
            modelStatus = "siap dipakai",
            message = "mesin tidak menemukan kandidat teks",
            candidates = listOf("halo", "hala"),
            ink = InkReport.Ink(3, 74, 120, 44)
        )
        assertTrue(report.contains("Penboard 3.0.6"))
        assertTrue(report.contains("Mesin ML Kit: tersedia"))
        assertTrue(report.contains("Bahasa: Indonesia"))
        assertTrue(report.contains("Model bahasa: siap dipakai"))
        assertTrue(report.contains("3 goresan, 74 titik, kotak 120x44"))
        assertTrue(report.contains("\"halo\"") && report.contains("\"hala\""))
        assertTrue(report.contains("mesin tidak menemukan kandidat teks"))
    }

    @Test
    fun `laporan tetap masuk akal saat belum ada apa-apa`() {
        val report = InkReport.lines(
            appVersion = "3.0.6",
            engineAvailable = false,
            language = "Indonesia",
            modelStatus = null,
            message = null,
            candidates = emptyList(),
            ink = null
        )
        assertTrue(report.contains("TIDAK tersedia"))
        assertTrue(report.contains("Model bahasa: belum diperiksa"))
        assertTrue(report.contains("Tinta terakhir: belum ada tulisan"))
        assertTrue(report.contains("Kandidat terakhir: tidak ada"))
        assertTrue(report.contains("Pesan mesin terakhir: tidak ada"))
    }
}
