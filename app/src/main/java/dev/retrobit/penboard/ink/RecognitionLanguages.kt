package dev.retrobit.penboard.ink

/** Bahasa tulisan yang bisa dikenali (BCP-47). */
object RecognitionLanguages {

    val all: List<Pair<String, String>> = listOf(
        "id-ID" to "Indonesia",
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "ms-MY" to "Bahasa Melayu",
        "jv-ID" to "Basa Jawa",
        "su-ID" to "Basa Sunda",
        "zh-Hans" to "中文 (简体)",
        "ja-JP" to "日本語",
        "ko-KR" to "한국어",
        "ar" to "العربية",
        "ru-RU" to "Русский",
        "hi-IN" to "हिन्दी",
        "th-TH" to "ไทย",
        "vi-VN" to "Tiếng Việt"
    )

    const val DEFAULT = "id-ID"

    /** Label bahasa untuk ditampilkan. */
    fun label(tag: String): String =
        all.firstOrNull { it.first.equals(tag, ignoreCase = true) }?.second ?: tag

    fun isSupported(tag: String?): Boolean =
        all.any { it.first.equals(tag, ignoreCase = true) }

    /** Kembalikan tag yang dikenal, atau [DEFAULT] bila tidak diketahui. */
    fun normalize(tag: String?): String {
        val clean = tag?.trim().orEmpty()
        if (clean.isEmpty()) return DEFAULT
        return all.firstOrNull { it.first.equals(clean, ignoreCase = true) }?.first ?: DEFAULT
    }
}
