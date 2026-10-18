package dev.retrobit.penboard.ink

/**
 * Laporan ringkas keadaan mesin pengenalan (murni Kotlin, mudah diuji).
 *
 * Dipakai tombol **Salin laporan mesin** di halaman pengaturan: teksnya bisa
 * ditempelkan ke chat/laporan bug supaya penyebab "tidak terbaca" bisa diketahui
 * tanpa perlu memasang alat tambahan.
 */
object InkReport {

    /** Ringkasan tinta yang dikirim ke mesin. */
    data class Ink(val strokes: Int, val points: Int, val width: Int, val height: Int)

    /** Ringkasan tinta hasil normalisasi. */
    fun inkOf(strokes: List<NormalizedStroke>): Ink? {
        if (strokes.isEmpty()) return null
        val points = strokes.flatMap { it.points }
        if (points.isEmpty()) return null
        val width = (points.maxOf { it.x } - points.minOf { it.x }).toInt()
        val height = (points.maxOf { it.y } - points.minOf { it.y }).toInt()
        return Ink(strokes.size, points.size, width, height)
    }

    /** Teks laporan siap tempel. */
    fun lines(
        appVersion: String,
        engineAvailable: Boolean,
        language: String,
        modelStatus: String?,
        message: String?,
        candidates: List<String>,
        ink: Ink?
    ): String {
        val builder = StringBuilder()
        builder.appendLine("Penboard $appVersion — laporan mesin pengenalan")
        builder.appendLine("Mesin ML Kit: " + if (engineAvailable) "tersedia" else "TIDAK tersedia (tanpa Google Play services)")
        builder.appendLine("Bahasa: $language")
        builder.appendLine("Model bahasa: " + (modelStatus?.takeIf { it.isNotBlank() } ?: "belum diperiksa"))
        builder.appendLine(
            "Tinta terakhir: " + when {
                ink == null -> "belum ada tulisan yang dikirim ke mesin"
                else -> "${ink.strokes} goresan, ${ink.points} titik, kotak ${ink.width}x${ink.height}"
            }
        )
        builder.appendLine(
            "Kandidat terakhir: " + when {
                candidates.isEmpty() -> "tidak ada"
                else -> candidates.joinToString(" | ") { "\"" + it + "\"" }
            }
        )
        builder.appendLine("Pesan mesin terakhir: " + (message?.takeIf { it.isNotBlank() } ?: "tidak ada"))
        builder.appendLine("(Semua pengenalan berjalan di dalam ponsel; tidak ada data yang dikirim ke server.)")
        return builder.toString().trim()
    }
}
