package dev.retrobit.penboard.util

/**
 * Bahasa untuk mesin pengenalan (tanpa Android, mudah diuji).
 *
 * ML Kit Digital Ink memakai tag BCP-47 **tanpa wilayah** untuk sebagian bahasa —
 * misalnya model bahasa Indonesia bernama `id`, bukan `id-ID`. Karena itu
 * [candidates] mencoba tag yang dipakai aplikasi lebih dulu (mis. `id-ID`), lalu
 * bentuk pendeknya (`id`), supaya bahasa tidak pernah "hilang" hanya karena
 * beda penulisan tag.
 */
object LanguageTags {

    /** Tag yang dipakai bila pengaturan kosong. */
    const val FALLBACK = "en-US"

    /** Bentuk pendek: `id-ID` -> `id`, `zh-Hans` -> `zh`, `en` -> `en`. */
    fun short(tag: String): String =
        tag.trim().substringBefore('-').substringBefore('_').lowercase()

    /** Berurutan: tag lengkap dulu, lalu bentuk pendeknya (bila berbeda). */
    fun candidates(tag: String): List<String> {
        val clean = tag.trim()
        if (clean.isEmpty()) return listOf(FALLBACK, short(FALLBACK))
        val short = short(clean)
        return if (short == clean.lowercase()) listOf(clean) else listOf(clean, short)
    }

    /** Apakah [tag] ditulis sebagai tag lengkap dengan wilayah. */
    fun hasRegion(tag: String): Boolean =
        tag.contains('-') || tag.contains('_')
}
