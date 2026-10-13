package dev.retrobit.penboard.ink

/**
 * Aturan teks hasil pengenalan tulisan. Fungsi murni (tanpa Android) sehingga bisa diuji.
 *
 * Perilaku "satu tulisan = satu kata": setiap hasil pengenalan dianggap satu kata, lalu
 * ditambah **satu spasi** agar kata berikutnya langsung terpisah.
 */
object RecognitionText {

    private val existingSpace = charArrayOf(' ', '\n', '\t')

    /**
     * Siapkan teks untuk dikirim ke kolom tujuan.
     *
     * @param autoSpace bila aktif, tambahkan spasi di akhir (satu kata per tulisan).
     */
    fun prepare(raw: String?, autoSpace: Boolean): String {
        val clean = raw?.trim().orEmpty()
        if (clean.isEmpty()) return ""
        if (!autoSpace) return clean
        val last = clean.last()
        return if (existingSpace.contains(last)) clean else clean + " "
    }

    /** Teks yang layak dikenali: tidak kosong dan punya setidaknya satu karakter huruf/angka. */
    fun isMeaningful(raw: String?): Boolean {
        val clean = raw?.trim().orEmpty()
        return clean.any { it.isLetterOrDigit() }
    }
}
