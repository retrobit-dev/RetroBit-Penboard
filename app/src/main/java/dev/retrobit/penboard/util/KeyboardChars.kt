package dev.retrobit.penboard.util

/**
 * Karakter keyboard konvensional yang bisa dipasang pada satu tombol, atau dipilih
 * dari papan karakter: angka, huruf, tanda baca, dan simbol.
 *
 * Bukan susunan QWERTY — hanya daftar karakter yang tulis tangan sering salah baca
 * (angka, tanda baca) atau yang ingin diketik langsung.
 */
object KeyboardChars {

    /** Batas teks satu tombol, supaya pengaturan tidak menyimpan string tak terbatas. */
    const val MAX_LENGTH = 64

    /** Satu kelompok pada pemilih karakter dan papan karakter. */
    data class Group(val title: String, val short: String, val chars: String)

    val groups: List<Group> = listOf(
        Group("Angka", "123", "0123456789"),
        Group("Huruf kecil", "abc", "abcdefghijklmnopqrstuvwxyz"),
        Group("Huruf besar", "ABC", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        Group("Tanda baca", ".,!", punctuation()),
        Group("Simbol", "@#%", symbols())
    )

    /** Potong [raw] sampai [MAX_LENGTH] titik kode, tanpa membelah pasangan surrogate. */
    fun clamp(raw: String): String {
        if (raw.isEmpty()) return ""
        var count = 0
        var index = 0
        while (index < raw.length && count < MAX_LENGTH) {
            index += Character.charCount(raw.codePointAt(index))
            count++
        }
        return raw.substring(0, index)
    }

    /**
     * Tulisan di muka tombol karakter.
     * Label khusus menang bila diisi; kalau tidak, karakter yang akan dimasukkan
     * yang digambar. Null berarti pakai ikon aksi (tombol karakter yang masih kosong).
     */
    fun faceOf(label: String, text: String): String? {
        if (label.isNotBlank()) return label.take(12)
        if (text.isEmpty()) return null
        if (text.isBlank()) return "\u2423".repeat(text.length.coerceAtMost(3))
        return text.replace("\n", "\u21B5").replace("\t", "\u21E5").take(12)
    }

    /** Ukuran huruf muka tombol, dibanding sisi tombol. Satu karakter digambar besar. */
    fun faceTextPx(sidePx: Int, face: String): Float {
        val fraction = when {
            face.length <= 1 -> 0.42f
            face.length == 2 -> 0.32f
            face.length <= 4 -> 0.24f
            else -> 0.16f
        }
        return sidePx * fraction
    }

    private fun punctuation(): String = String(
        charArrayOf('.', ',', '!', '?', ';', ':', '\'', '"', '-', '(', ')', '[', ']', '{', '}')
    )

    private fun symbols(): String = String(
        charArrayOf('@', '#', '$', '%', '&', '*', '+', '=', '/', '\\', '_', '|', '~', '^', '<', '>', '`')
    )
}
