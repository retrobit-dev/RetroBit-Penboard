package dev.retrobit.penboard.util

/**
 * Hitungan teks sederhana (tanpa Android) untuk menulis ke kolom isian lewat
 * layanan aksesibilitas: menyisipkan di posisi kursor, menghapus satu huruf, dan
 * mencari awal kata. Semua fungsi harus tahan menerima posisi kursor yang aneh
 * (`-1` dari `AccessibilityNodeInfo`).
 */
object TextInsert {

    /** Posisi kursor yang tidak diketahui. */
    const val UNKNOWN = -1

    /** Teks baru beserta posisi kursor sesudahnya. */
    data class Result(val text: String, val caret: Int)

    /** Sisipkan [insert] pada rentang [start]..[end]. */
    fun into(existing: String, start: Int, end: Int, insert: String): Result {
        val safeStart = normalize(existing, start, fallbackEnd = true)
        val safeEnd = normalize(existing, end, fallbackEnd = true).coerceAtLeast(safeStart)
        val merged = existing.substring(0, safeStart) + insert + existing.substring(safeEnd)
        return Result(merged, safeStart + insert.length)
    }

    /** Hapus satu huruf sebelum kursor (atau seluruh rentang yang dipilih). */
    fun deleteBackward(existing: String, start: Int, end: Int): Result {
        val safeStart = normalize(existing, start, fallbackEnd = true)
        val safeEnd = normalize(existing, end, fallbackEnd = true).coerceAtLeast(safeStart)
        return when {
            safeEnd > safeStart -> Result(existing.substring(0, safeStart) + existing.substring(safeEnd), safeStart)
            safeStart == 0 -> Result(existing, 0)
            else -> Result(existing.substring(0, safeStart - 1) + existing.substring(safeStart), safeStart - 1)
        }
    }

    /** Hapus satu huruf sesudah kursor (atau seluruh rentang yang dipilih). */
    fun deleteForward(existing: String, start: Int, end: Int): Result {
        val safeStart = normalize(existing, start, fallbackEnd = true)
        val safeEnd = normalize(existing, end, fallbackEnd = true).coerceAtLeast(safeStart)
        return when {
            safeEnd > safeStart -> Result(existing.substring(0, safeStart) + existing.substring(safeEnd), safeStart)
            safeStart >= existing.length -> Result(existing, safeStart)
            else -> Result(existing.substring(0, safeStart) + existing.substring(safeStart + 1), safeStart)
        }
    }

    /** Hapus satu kata sebelum kursor (spasi di depan kursor ikut dibuang). */
    fun deleteWord(existing: String, start: Int, end: Int): Result {
        val safeStart = normalize(existing, start, fallbackEnd = true)
        val safeEnd = normalize(existing, end, fallbackEnd = true).coerceAtLeast(safeStart)
        if (safeEnd > safeStart) return Result(existing.substring(0, safeStart) + existing.substring(safeEnd), safeStart)
        val from = wordStart(existing, safeStart)
        if (from == safeStart) return Result(existing, safeStart)
        return Result(existing.substring(0, from) + existing.substring(safeStart), from)
    }

    /** Awal kata sebelum [caret]: spasi tepat di kiri kursor dilewati lebih dulu. */
    fun wordStart(existing: String, caret: Int): Int {
        var index = normalize(existing, caret, fallbackEnd = true)
        while (index > 0 && existing[index - 1].isWhitespace()) index--
        while (index > 0 && !existing[index - 1].isWhitespace()) index--
        return index
    }

    /**
     * Akhir kata di kanan [caret]: spasi tepat di kanan dilewati, lalu habiskan satu kata.
     * Sama seperti Ctrl+kanan.
     */
    fun wordEnd(existing: String, caret: Int): Int {
        var index = normalize(existing, caret, fallbackEnd = false)
        while (index < existing.length && existing[index].isWhitespace()) index++
        while (index < existing.length && !existing[index].isWhitespace()) index++
        return index
    }

    /**
     * Rentang kata di sekitar [caret]. Di tengah kata = kata itu; di spasi atau di
     * akhir kata = kata di kirinya; bila tidak ada, kata di kanannya.
     */
    fun wordRange(existing: String, caret: Int): Pair<Int, Int> {
        if (existing.isEmpty()) return 0 to 0
        val index = normalize(existing, caret, fallbackEnd = true).coerceIn(0, existing.length)
        val inside = index < existing.length && !existing[index].isWhitespace()
        if (inside) {
            var start = index
            var end = index
            while (start > 0 && !existing[start - 1].isWhitespace()) start--
            while (end < existing.length && !existing[end].isWhitespace()) end++
            return start to end
        }
        var end = index
        while (end > 0 && existing[end - 1].isWhitespace()) end--
        var start = end
        while (start > 0 && !existing[start - 1].isWhitespace()) start--
        if (start < end) return start to end
        start = index
        while (start < existing.length && existing[start].isWhitespace()) start++
        end = start
        while (end < existing.length && !existing[end].isWhitespace()) end++
        return start to end
    }

    /** Awal baris yang memuat [caret]. */
    fun lineStart(existing: String, caret: Int): Int {
        val index = normalize(existing, caret, fallbackEnd = true).coerceIn(0, existing.length)
        if (index == 0) return 0
        val newline = existing.lastIndexOf('\n', index - 1)
        return if (newline < 0) 0 else newline + 1
    }

    /**
     * Geser kursor sekitar tiga baris. [direction] negatif = ke atas, positif = ke bawah.
     * Dipakai layanan aksesibilitas; keyboard sungguhan mengirim tombol Page Up/Down.
     */
    fun pageCaret(existing: String, caret: Int, direction: Int): Int {
        var index = normalize(existing, caret, fallbackEnd = false).coerceIn(0, existing.length)
        repeat(3) {
            index = if (direction < 0) {
                val start = lineStart(existing, index)
                if (start < index) start else lineStart(existing, (start - 1).coerceAtLeast(0))
            } else {
                val newline = existing.indexOf('\n', index)
                if (newline < 0) existing.length else (newline + 1).coerceAtMost(existing.length)
            }
        }
        return index.coerceIn(0, existing.length)
    }

    /** Posisi kursor setelah digeser [delta] huruf. */
    fun moveCaret(existing: String, caret: Int, delta: Int): Int {
        val from = normalize(existing, caret, fallbackEnd = false)
        return (from + delta).coerceIn(0, existing.length)
    }

    /** Posisi `-1` dianggap "di akhir teks" bila [fallbackEnd], selain itu dianggap 0. */
    private fun normalize(existing: String, position: Int, fallbackEnd: Boolean): Int = when {
        position < 0 -> if (fallbackEnd) existing.length else 0
        position > existing.length -> existing.length
        else -> position
    }
}
