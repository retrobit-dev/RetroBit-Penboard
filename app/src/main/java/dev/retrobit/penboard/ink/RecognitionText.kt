package dev.retrobit.penboard.ink

/**
 * Teks hasil pengenalan: satu tulisan = satu kata, lalu satu spasi ditambahkan
 * otomatis untuk kata berikutnya.
 */
object RecognitionText {

    /**
     * Gabungkan hasil pengenalan menjadi teks yang siap diinput.
     * - baris baru diubah menjadi spasi, spasi berlebih dirapikan,
     * - bila [autoSpace] aktif, satu spasi di ujung ditambahkan.
     */
    fun prepare(raw: String?, autoSpace: Boolean): String {
        val collapsed = raw
            ?.replace('\n', ' ')
            ?.replace('\r', ' ')
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            .orEmpty()
        if (collapsed.isEmpty()) return ""
        return if (autoSpace) "$collapsed " else collapsed
    }

    /** Apakah hasil pengenalan layak diinput (mengandung huruf/angka). */
    fun isMeaningful(text: String?): Boolean {
        val clean = text?.trim().orEmpty()
        if (clean.isEmpty()) return false
        return clean.any { it.isLetterOrDigit() }
    }
}
